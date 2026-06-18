/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/config/WebClientConfig.java
 */
package com.gongsitoktok.assistant.global.config;

import com.gongsitoktok.assistant.global.filter.MdcTraceIdFilter;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * FastAPI AI Agent 호출 전용 {@link WebClient} 설정 (v2.0 동기 JSON).
 *
 * <h3>타임아웃 튜닝 (Spring_챗봇_핸들링_가이드 §5 · 제작요청 v6 §스텝 C)</h3>
 * <ul>
 *     <li><b>Connect Timeout 10초</b> — 연결 자체가 안 되면 빠르게 fail-fast (가상 스레드 무한 대기 차단).</li>
 *     <li><b>Write Timeout 30초</b> — 대용량 messages 배열 송신 마진.</li>
 *     <li><b>Response Timeout 90초</b> — AI 전체 파이프라인(RAG + LLM + 검증 + CRAG) 여유분. LLM 자체 60초 가정 + 마진 30초.</li>
 * </ul>
 *
 * <h3>{@code maxInMemorySize} 1MB</h3>
 * <p>긴 답변 누적 시 {@code DataBufferLimitException} 또는 OOM 을 막기 위해 codec 의 in-memory 버퍼를 1MB 로 명시.
 * FastAPI 응답의 {@code answerText} 가 수십 KB 단위이므로 1MB 는 충분한 마진.</p>
 *
 * <h3>X-Trace-Id 자동 전파</h3>
 * <p>{@link ExchangeFilterFunction} 로 매 요청마다 현재 MDC 의 {@code traceId} 를 HTTP 헤더에 주입한다.
 * Reactor 의 자동 컨텍스트 전파({@code ReactorContextConfig}) 와 결합하여, 가상 스레드 → Reactor 스레드 전환
 * 후에도 동일 traceId 가 살아 있다.</p>
 *
 * <h3>왜 SSE 가 아닌 단일 {@code .bodyToMono()} 인가</h3>
 * <p>v2.0 에서 SSE 는 폐기되었다. FastAPI 의 verification / CRAG 단계가 답 생성 뒤에 돌기 때문에 실시간 토큰
 * 스트리밍이 불가능하다. 단일 JSON 응답으로 단순화하고, 브라우저는 spinner UX 로 처리한다.</p>
 *
 * @see com.gongsitoktok.assistant.global.config.ReactorContextConfig
 */
@Configuration
public class WebClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int WRITE_TIMEOUT_SECONDS = 30;
    private static final int RESPONSE_TIMEOUT_SECONDS = 90;
    private static final int MAX_IN_MEMORY_BYTES = 1024 * 1024;

    /**
     * FastAPI 전용 WebClient.
     *
     * @param baseUrl {@code fastapi.base-url} 설정값. 환경별로 다름.
     * @return 타임아웃·헤더·코덱이 모두 결선된 단일 {@link WebClient} 인스턴스.
     */
    @Bean("fastApiWebClient")
    public WebClient fastApiWebClient(@Value("${fastapi.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .doOnConnected(conn -> conn.addHandlerLast(
                        new WriteTimeoutHandler(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)))
                .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS));

        // TODO(monitoring): MeterRegistry 주입 후 webclient.requests.duration / webclient.upstream_error 등록
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_BYTES))
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .filter(traceIdPropagator())
                .build();
    }

    /**
     * 매 outbound 요청에 {@code X-Trace-Id} 헤더를 자동 부착하는 ExchangeFilter.
     * <p>MDC 가 비어 있으면 {@code "unknown"} 으로 폴백 — 그래도 FastAPI 가 헤더 누락으로 오해하지 않도록.</p>
     */
    private ExchangeFilterFunction traceIdPropagator() {
        return (request, next) -> {
            String traceId = MDC.get(MdcTraceIdFilter.MDC_TRACE_ID);
            ClientRequest decorated = ClientRequest.from(request)
                    .header(MdcTraceIdFilter.HEADER_TRACE_ID, traceId != null ? traceId : "unknown")
                    .build();
            return next.exchange(decorated);
        };
    }
}
