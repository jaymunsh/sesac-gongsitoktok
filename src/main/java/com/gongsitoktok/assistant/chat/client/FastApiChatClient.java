/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/client/FastApiChatClient.java
 */
package com.gongsitoktok.assistant.chat.client;

import com.gongsitoktok.assistant.chat.dto.fastapi.FastApiChatRequest;
import com.gongsitoktok.assistant.chat.dto.fastapi.FastApiChatResponse;
import com.gongsitoktok.assistant.global.error.ErrorCode;
import com.gongsitoktok.assistant.global.error.exception.UpstreamUnavailableException;
import io.netty.channel.ConnectTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

/**
 * FastAPI 단일 추론 endpoint 호출 클라이언트 (Spring_챗봇_핸들링_가이드 §3.3 · v2.0 동기 JSON).
 *
 * <h3>호출 흐름</h3>
 * <ol>
 *     <li>{@code POST /api/v1/chat} + JSON body.</li>
 *     <li>HTTP 4xx/5xx → body 파싱 시도 없이 즉시 {@link UpstreamUnavailableException} (Spring_챗봇_핸들링_가이드 §4 정합).</li>
 *     <li>HTTP 200 → {@link FastApiChatResponse} 로 매핑. body 의 {@code error} 필드 분기는 호출 측({@code ChatController}) 책임.</li>
 *     <li>{@link ConnectTimeoutException} / {@link TimeoutException} → {@link ErrorCode#UPSTREAM_TIMEOUT}.</li>
 *     <li>기타 IO 실패 → {@link ErrorCode#UPSTREAM_UNAVAILABLE}.</li>
 * </ol>
 *
 * <h3>{@code .block()} vs {@code .subscribe()}</h3>
 * <p>가상 스레드 환경이라 {@code .block()} 이 carrier 점유 없이 안전. 그러나 본 클라이언트는 {@link Mono} 를 그대로 반환해
 * 호출 측의 동기/비동기 선택을 막지 않는다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiChatClient {

    @Qualifier("fastApiWebClient")
    private final WebClient webClient;

    /**
     * FastAPI 추론 호출. 호출 측은 {@code .block()} 하여 가상 스레드에서 동기적으로 결과 대기.
     *
     * @param request 검증·조립된 요청 바디
     * @return {@code Mono<FastApiChatResponse>} — 200 응답만 도달, 그 외는 onError
     */
    public Mono<FastApiChatResponse> call(FastApiChatRequest request) {
        return webClient.post()
                .uri("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapHttpError)
                .bodyToMono(FastApiChatResponse.class)
                .onErrorMap(this::mapNetworkError);
    }

    private Mono<? extends Throwable> mapHttpError(ClientResponse resp) {
        HttpStatusCode status = resp.statusCode();
        ErrorCode code = switch (status.value()) {
            case 400 -> ErrorCode.UPSTREAM_ERROR;
            case 429 -> ErrorCode.UPSTREAM_RATE_LIMITED;
            case 503 -> ErrorCode.UPSTREAM_UNAVAILABLE;
            case 504 -> ErrorCode.UPSTREAM_TIMEOUT;
            default -> status.is5xxServerError() ? ErrorCode.UPSTREAM_UNAVAILABLE : ErrorCode.UPSTREAM_ERROR;
        };
        log.warn("FastAPI HTTP 오류 status={}", status.value());
        // body 파싱 시도 금지 — Spring_챗봇_핸들링_가이드 §11 함정 회피.
        return resp.releaseBody().then(Mono.error(
                new UpstreamUnavailableException(code, "FastAPI 응답 실패: HTTP " + status.value())));
    }

    private Throwable mapNetworkError(Throwable cause) {
        if (cause instanceof UpstreamUnavailableException up) {
            return up;   // 위 onStatus 에서 이미 분류된 케이스는 그대로 전달
        }
        if (cause instanceof WebClientResponseException wcre) {
            // onStatus 가 안 잡힌 케이스 (이론상 발생하지 않음)
            return new UpstreamUnavailableException(ErrorCode.UPSTREAM_UNAVAILABLE,
                    "WebClient 응답 예외: " + wcre.getStatusCode(), wcre);
        }
        if (cause instanceof ConnectTimeoutException || cause instanceof TimeoutException) {
            return new UpstreamUnavailableException(ErrorCode.UPSTREAM_TIMEOUT,
                    "FastAPI 응답 타임아웃 (90초)", cause);
        }
        if (cause instanceof java.net.ConnectException) {
            return new UpstreamUnavailableException(ErrorCode.UPSTREAM_UNAVAILABLE,
                    "FastAPI 연결 실패", cause);
        }
        log.error("FastAPI 호출 미분류 예외", cause);
        return new UpstreamUnavailableException(ErrorCode.UPSTREAM_UNAVAILABLE,
                "FastAPI 호출 중 알 수 없는 오류: " + cause.getClass().getSimpleName(), cause);
    }
}
