/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/config/BCryptAsyncConfig.java
 */
package com.gongsitoktok.assistant.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BCrypt 비밀번호 인코딩/검증 전용 플랫폼 스레드 풀.
 *
 * <h3>왜 격리하는가</h3>
 * <ul>
 *     <li>{@link BCryptPasswordEncoder#matches(CharSequence, String)} 는 CPU 코어를 100% 장악하는 본격 암호 연산이다.</li>
 *     <li>가상 스레드 위에서 그대로 실행하면 long-running CPU 작업이 carrier(=플랫폼) 스레드를 길게 점유 → 다른
 *         가상 스레드의 동시성 처리가 모두 정지하는 사실상 Pinned-like 상황이 발생한다.</li>
 *     <li>따라서 CPU 코어 수와 정확히 같은 크기의 <b>고정 플랫폼 스레드 풀</b>로 격리한다. 풀 크기가 코어 수와 같으므로
 *         컨텍스트 스위칭 비용이 최소화되고, 가상 스레드 풀은 CPU 작업으로부터 보호된다.</li>
 * </ul>
 *
 * <h3>ThreadFactory 명명 규약</h3>
 * <p>{@code bcrypt-worker-{n}} — 스레드 덤프·프로파일러에서 한눈에 식별 가능. {@code AtomicInteger} 로 0 부터 단조 증가.</p>
 *
 * <h3>사용 패턴</h3>
 * <pre>{@code
 *   CompletableFuture<Boolean> verified = CompletableFuture.supplyAsync(
 *       () -> passwordEncoder.matches(rawPassword, user.getPassword()),
 *       bcryptExecutor
 *   );
 *   if (!verified.join()) { throw new BadCredentialsException(...); }
 * }</pre>
 *
 * <p>{@code .join()} 호출 자체는 가상 스레드 위에서 일어나므로 carrier 점유 없이 결과 대기 가능.</p>
 */
@Configuration
public class BCryptAsyncConfig {

    /**
     * BCryptPasswordEncoder 빈. 강도 기본값(10) 사용.
     *
     * <p>강도를 12 이상으로 올리면 단일 matches 호출이 수백 ms 까지 늘어 worker 스레드 부담 급증. 11~12 도입은
     * 운영 트래픽과 응답 시간을 보고 결정.</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * BCrypt 격리용 고정 플랫폼 스레드 풀.
     *
     * <p>Java 21 의 {@code Thread.ofPlatform().name(prefix, start).factory()} 를 사용해 이름 prefix 와 시작 번호를 지정.</p>
     */
    @Bean("bcryptExecutor")
    public ExecutorService bcryptExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadFactory factory = new ThreadFactory() {
            private final ThreadFactory delegate = Thread.ofPlatform()
                    .name("bcrypt-worker-", 0)
                    .daemon(true)
                    .factory();
            private final AtomicInteger ignored = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                ignored.incrementAndGet();
                return delegate.newThread(r);
            }
        };
        // TODO(monitoring): MeterRegistry 주입 후 bcrypt.queue.size · bcrypt.active 게이지 노출
        return Executors.newFixedThreadPool(cores, factory);
    }
}
