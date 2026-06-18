/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/config/ReactorContextConfig.java
 */
package com.gongsitoktok.assistant.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * Reactor 리액티브 체인의 ThreadLocal(=MDC 포함) 자동 전파 활성.
 *
 * <h3>왜 필요한가</h3>
 * <ul>
 *     <li>WebClient 호출은 Reactor 의 {@code Mono}/{@code Flux} 체인으로 동작하며, 스케줄러 전환 시 부모 스레드의
 *         ThreadLocal 이 사라진다 (가상 스레드에서 호출했더라도 마찬가지).</li>
 *     <li>{@link Hooks#enableAutomaticContextPropagation()} 활성 시, Reactor 가 micrometer-context-propagation 의
 *         ThreadLocalAccessor SPI 를 통해 MDC 같은 ThreadLocal 을 자동으로 캡쳐·복원한다.</li>
 *     <li>덕분에 WebClient {@code ExchangeFilterFunction} 안에서 {@code MDC.get("traceId")} 를 호출하면
 *         원래 요청의 traceId 가 정상 값으로 돌아온다 → {@code X-Trace-Id} 헤더 자동 주입에 사용.</li>
 * </ul>
 *
 * <h3>호출 시점</h3>
 * <p>애플리케이션 부팅 직후 {@link PostConstruct} 에서 단 한 번 호출. 본 훅은 프로세스 전역으로 작용하며 이후
 * 모든 Reactor 체인에 적용된다.</p>
 *
 * @see com.gongsitoktok.assistant.global.config.WebClientConfig
 */
@Configuration
public class ReactorContextConfig {

    @PostConstruct
    public void enableContextPropagation() {
        Hooks.enableAutomaticContextPropagation();
    }
}
