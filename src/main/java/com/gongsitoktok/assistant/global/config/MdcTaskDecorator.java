/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/config/MdcTaskDecorator.java
 */
package com.gongsitoktok.assistant.global.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * {@code TaskDecorator} — 부모 스레드의 MDC 컨텍스트를 가상 스레드/Async 작업 스레드로 전파.
 *
 * <h3>왜 필요한가</h3>
 * <ul>
 *     <li>{@link MDC} 는 ThreadLocal 기반. {@code @Async} 가 새 스레드(가상 또는 플랫폼) 에서 작업을 시작하면 부모의 MDC 가 사라진다.</li>
 *     <li>{@code Hooks.enableAutomaticContextPropagation()} 이 자동 전파를 도와주지만, {@code TaskExecutor.execute(Runnable)} 경로는 별도로 데코레이션이 필요하다.</li>
 *     <li>본 데코레이터는 부모 MDC 스냅샷을 자식 작업 실행 직전에 복원하고, 작업 종료 시 {@link MDC#clear()} 로 메모리 누수와 컨텍스트 오염을 동시에 방어한다.</li>
 * </ul>
 *
 * <h3>가상 스레드와의 상호작용</h3>
 * <p>가상 스레드는 매 작업마다 새 스레드 인스턴스이므로 ThreadLocal 격리는 자연스럽지만, 본 데코레이터는
 * MDC 복원 자체를 보장하는 책임을 진다. {@link MDC#clear()} 는 가상 스레드가 종료 직전이라도 carrier
 * 회수 시점에 잔재 컨텍스트가 남지 않도록 보수적으로 호출한다.</p>
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        Map<String, String> parentContext = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (parentContext != null) {
                    MDC.setContextMap(parentContext);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
