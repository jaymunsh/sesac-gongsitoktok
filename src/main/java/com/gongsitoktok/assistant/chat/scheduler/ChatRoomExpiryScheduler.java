/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/scheduler/ChatRoomExpiryScheduler.java
 */
package com.gongsitoktok.assistant.chat.scheduler;

import com.gongsitoktok.assistant.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 만료 임계({@code lastActiveAt + 30분}) 초과한 활성 대화방을 일괄 close 처리하는 스케줄러.
 *
 * <h3>도입 배경</h3>
 * <p>v2 기획상 30분 만료 트리거는 두 진입점뿐이었다 — {@code /chat/continue} 진입 시점, {@code /chat/rooms} 조회 시점.
 * 그런데 사용자가 채팅 패널을 열어둔 채 페이지를 떠나면 두 진입점 어디도 발화되지 않아 방이 영구적으로 활성 상태에
 * 박혀 마이페이지에도 노출되지 않는 문제가 있었다. 본 스케줄러로 백그라운드 lazy close 를 보장한다.</p>
 *
 * <h3>실행 정책</h3>
 * <ul>
 *     <li>{@code @Scheduled(fixedRate = 60_000)} — 1분 주기. 30분 만료 정책에 비해 최대 1분 lag 발생 가능 (허용 범위).</li>
 *     <li>{@code @Modifying} UPDATE 한 방으로 처리 — dirty checking N+1 회피.</li>
 *     <li>{@link Transactional} 로 단일 트랜잭션 보장.</li>
 *     <li>닫힌 행이 0건이면 로그 미출력 — 평시 로그 노이즈 차단.</li>
 * </ul>
 *
 * <h3>다중 인스턴스 운영 시 주의</h3>
 * <p>현재는 단일 인스턴스 기준. 다중 인스턴스로 확장하면 같은 시각에 모든 인스턴스가 동시에 UPDATE 를 시도해
 * 중복 작업이 발생한다. UPDATE 자체는 멱등이라 정합성 사고는 없지만 불필요한 부하 — ShedLock 등 분산 락 도입 검토 필요
 * ({@code later.md} #2 항목으로 추적).</p>
 *
 * <h3>{@code @Scheduled} 와 가상 스레드</h3>
 * <p>Spring Boot 3.x 의 {@code @Scheduled} 기본 executor 는 단일 스레드 풀이며 가상 스레드는 자동 적용되지 않는다.
 * 본 작업은 UPDATE 한 방 + 짧은 트랜잭션이라 carrier 점유 우려 없음. 만약 작업이 무거워지면 {@code TaskScheduler}
 * 빈 커스터마이즈로 가상 스레드 적용 가능.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomExpiryScheduler {

    private static final int SESSION_TIMEOUT_MINUTES = 30;

    private final ChatRoomRepository chatRoomRepository;

    /**
     * 1분 주기로 만료된 활성 방을 일괄 close 처리.
     *
     * <p>UPDATE 영향 행 수가 0건이면 로그 출력 안 함 (평시 노이즈 방지). 1건 이상일 때만 debug 로 기록.</p>
     */
    @Scheduled(fixedRate = 60_000L)
    @Transactional
    public void closeExpiredRooms() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(SESSION_TIMEOUT_MINUTES);
        int closed = chatRoomRepository.closeExpiredActive(now, threshold);
        if (closed > 0) {
            log.debug("[scheduler] closed {} expired chat rooms (threshold={})", closed, threshold);
        }
    }
}
