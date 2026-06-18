/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/entity/ChatRoom.java
 */
package com.gongsitoktok.assistant.chat.entity;

import com.gongsitoktok.assistant.company.entity.Company;
import com.gongsitoktok.assistant.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 대화방 세션 엔티티 — {@code tb_chat_room}.
 *
 * <h3>핵심 정책</h3>
 * <ul>
 *     <li><b>한 방 = 정확히 한 기업</b>. {@link #company} FK 는 생성 시점에 박히고 이후 변경 불가.</li>
 *     <li><b>Soft Delete</b>: 사용자가 "삭제" 를 눌러도 row 보존, {@link #isActive} 만 {@code false} 로 전환.</li>
 *     <li><b>30분 만료</b>: {@code /chat/continue} 시 {@link #lastActiveAt} 기준 30분 초과면 {@code CHAT_ROOM_EXPIRED}.</li>
 * </ul>
 *
 * <h3>인덱스</h3>
 * <p>{@code (user_seq, is_active, last_active_at DESC)} — 사이드바 목록(§4-11) 의 주요 쿼리 패턴 가속.</p>
 *
 * <h3>FK 안정성</h3>
 * <p>FK 는 항상 불변 {@link User#getUserSeq()} · {@link Company#getCompanySeq()} 를 참조. cascade·@OnDelete 명시 없음.
 * 회원이 dismiss 변형되어도 {@code userSeq} 는 불변이므로 FK 깨지지 않는다.</p>
 */
@Entity
@Table(
        name = "tb_chat_room",
        indexes = {
                @Index(name = "idx_room_user_active_last",
                        columnList = "user_seq, is_active, last_active_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    /** FK → {@link User#getUserSeq()}. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_seq", referencedColumnName = "user_seq", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_room_user"))
    private User user;

    /** FK → {@link Company#getCompanySeq()}. 한 방 = 한 기업. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_seq", referencedColumnName = "company_seq", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_room_company"))
    private Company company;

    /** 최초 질문 첫 20자 슬라이싱. 컬럼 길이는 마진 포함 50. */
    @Column(name = "room_title", nullable = false, length = 50)
    private String roomTitle;

    /** 챗봇 대화방 최종 활성화 시각. 질문 전송 시마다 갱신. */
    @Column(name = "last_active_at", nullable = false)
    private LocalDateTime lastActiveAt;

    /** 사용자 숨김 여부. {@code false} 인 방은 목록·메시지 조회에서 제외. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /** 숨김 처리 시각 (감사용). 미숨김 시 NULL. */
    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 정적 팩토리 — 신규 대화방 생성. 30자 초과 질문은 20자에서 잘라 제목으로 사용.
     */
    public static ChatRoom create(User user, Company company, String firstQuestion, LocalDateTime now) {
        ChatRoom r = new ChatRoom();
        r.user = user;
        r.company = company;
        r.roomTitle = slice(firstQuestion);
        r.lastActiveAt = now;
        r.isActive = true;
        return r;
    }

    /**
     * 대화방 활성 시간 터치 (continue 호출 시).
     */
    public void touch(LocalDateTime now) {
        this.lastActiveAt = now;
    }

    /**
     * 숨김 처리 (Soft Delete).
     */
    public void hide(LocalDateTime now) {
        if (this.isActive) {
            this.isActive = false;
            this.hiddenAt = now;
        }
    }

    /**
     * 30분 만료 여부. {@code lastActiveAt + 30분 < now} 이면 만료.
     */
    public boolean isExpired(LocalDateTime now) {
        return this.lastActiveAt.plusMinutes(30).isBefore(now);
    }

    /**
     * 본인 소유 여부 검증.
     */
    public boolean isOwnedBy(Long userSeq) {
        return this.user.getUserSeq().equals(userSeq);
    }

    private static String slice(String s) {
        if (s == null) {
            return "";
        }
        String trimmed = s.trim();
        return trimmed.length() <= 20 ? trimmed : trimmed.substring(0, 20);
    }
}
