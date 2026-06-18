/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/filter/MdcTraceIdFilter.java
 */
package com.gongsitoktok.assistant.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청 단위 {@code traceId} 를 MDC 에 바인딩하는 최상위 서블릿 필터.
 *
 * <h3>핵심 동작</h3>
 * <ol>
 *     <li>요청 헤더 {@code X-Trace-Id} 가 있으면 그대로 사용 (LB · API Gateway 분산 추적 호환).</li>
 *     <li>없으면 {@link UUID#randomUUID()} 32자 hex 생성.</li>
 *     <li>{@link MDC#put(String, String)} 으로 컨텍스트 바인딩.</li>
 *     <li>응답 헤더에도 동일 {@code X-Trace-Id} 를 노출 → 에러 응답 JSON 의 {@code traceId} 와 정합.</li>
 *     <li>finally 블록에서 반드시 {@link MDC#clear()} — ThreadLocal 메모리 유출 방지.</li>
 * </ol>
 *
 * <h3>가상 스레드와 MDC</h3>
 * <p>Java 21 가상 스레드는 매 요청마다 새 스레드 인스턴스이므로 ThreadLocal 격리는 자연스럽다.
 * 그럼에도 {@code finally MDC.clear()} 를 유지하는 이유는 (1) 플랫폼 스레드로 회귀하는 케이스 방어,
 * (2) {@code @Async} 영속화 풀이 같은 carrier 를 빌릴 때의 보수적 안전망 때문이다.</p>
 *
 * <h3>리액티브 체인과의 통합</h3>
 * <p>{@code ReactorContextConfig#enableContextPropagation()} 활성으로 본 필터가 심은 MDC 값이
 * WebClient 의 {@code ExchangeFilterFunction} (X-Trace-Id 자동 헤더 주입) 까지 자연 전파된다.</p>
 *
 * @see com.gongsitoktok.assistant.global.config.ReactorContextConfig
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcTraceIdFilter extends OncePerRequestFilter {

    /** MDC 키. 로그 패턴 {@code [%X{traceId:-}]} 와 일치해야 한다. */
    public static final String MDC_TRACE_ID = "traceId";

    /** HTTP 헤더 명. WebClient ExchangeFilter, CORS 설정과도 일치해야 한다. */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = pickTraceId(request);
        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(HEADER_TRACE_ID, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private String pickTraceId(HttpServletRequest request) {
        String incoming = request.getHeader(HEADER_TRACE_ID);
        if (incoming != null && !incoming.isBlank() && incoming.length() <= 64) {
            return incoming;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
