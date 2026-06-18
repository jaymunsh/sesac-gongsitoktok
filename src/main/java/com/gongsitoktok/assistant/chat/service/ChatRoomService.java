/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/service/ChatRoomService.java
 */
package com.gongsitoktok.assistant.chat.service;

import com.gongsitoktok.assistant.chat.dto.ChatMessagesResponse;
import com.gongsitoktok.assistant.chat.dto.ChatRoomListItemResponse;
import com.gongsitoktok.assistant.chat.dto.HideResponse;
import com.gongsitoktok.assistant.chat.entity.ChatRoom;
import com.gongsitoktok.assistant.chat.entity.QaHistory;
import com.gongsitoktok.assistant.chat.repository.ChatRoomRepository;
import com.gongsitoktok.assistant.chat.repository.QaHistoryRepository;
import com.gongsitoktok.assistant.company.entity.Company;
import com.gongsitoktok.assistant.company.repository.CompanyRepository;
import com.gongsitoktok.assistant.global.error.ErrorCode;
import com.gongsitoktok.assistant.global.error.exception.BusinessException;
import com.gongsitoktok.assistant.global.error.exception.ChatRoomExpiredException;
import com.gongsitoktok.assistant.global.error.exception.ChatRoomNotFoundException;
import com.gongsitoktok.assistant.global.error.exception.CompanyNotFoundException;
import com.gongsitoktok.assistant.user.entity.User;
import com.gongsitoktok.assistant.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대화방 라이프사이클 서비스 (제작요청 v6 §4-9, §4-10, §4-11, §4-12, §4-13).
 *
 * <h3>본인 소유 + isActive 검증 통일</h3>
 * <p>"방이 없다" / "다른 사람 것이다" / "숨겨졌다" 를 모두 단일 {@link ChatRoomNotFoundException} (404) 로 응답하여
 * 정보 누출을 차단한다.</p>
 */
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final QaHistoryRepository qaHistoryRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    /**
     * 최초 질문 시 대화방 생성 (§4-9 step 1·2).
     *
     * @return 새로 INSERT 된 방 (company 함께 채워짐)
     */
    @Transactional
    public ChatRoom createRoom(Long userSeq, String corpCode, String firstQuestion) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_WITHDRAWN);
        }
        Company company = companyRepository.findByCorpCode(corpCode)
                .orElseThrow(() -> new CompanyNotFoundException(corpCode));
        return chatRoomRepository.save(ChatRoom.create(user, company, firstQuestion, LocalDateTime.now()));
    }

    /**
     * {@code /continue} 진입 시 본인 소유 + 활성 + 만료 검증 후 lastActiveAt 터치 (§4-10 step 1·2·5).
     *
     * @return 검증된 방 (company 함께 채워짐)
     */
    @Transactional
    public ChatRoom validateAndTouch(Long roomId, Long userSeq) {
        ChatRoom room = chatRoomRepository.findByIdWithCompany(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(roomId));
        if (!room.isOwnedBy(userSeq) || !room.isActive()) {
            throw new ChatRoomNotFoundException(roomId);
        }
        LocalDateTime now = LocalDateTime.now();
        if (room.isExpired(now)) {
            throw new ChatRoomExpiredException(roomId);
        }
        room.touch(now);
        return room;
    }

    /**
     * 사이드바 목록 — JOIN FETCH 한 번에 corpCode/corpName 동봉 (§4-11).
     */
    @Transactional(readOnly = true)
    public List<ChatRoomListItemResponse> list(Long userSeq) {
        return chatRoomRepository.findActiveByUserSeqWithCompany(userSeq).stream()
                .map(ChatRoomListItemResponse::from)
                .toList();
    }

    /**
     * 타임라인 조회 (§4-12). 본인 소유 + isActive 검증 통과 후 QA 시간순 묶음.
     */
    @Transactional(readOnly = true)
    public ChatMessagesResponse timeline(Long roomId, Long userSeq) {
        ChatRoom room = chatRoomRepository.findByIdWithCompany(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(roomId));
        if (!room.isOwnedBy(userSeq) || !room.isActive()) {
            throw new ChatRoomNotFoundException(roomId);
        }
        List<QaHistory> history = qaHistoryRepository.findByChatRoomRoomIdOrderByCreatedAtAsc(roomId);
        List<ChatMessagesResponse.Item> items = history.stream()
                .map(ChatMessagesResponse.Item::from)
                .toList();
        return new ChatMessagesResponse(
                room.getRoomId(),
                room.getCompany().getCorpCode(),
                room.getCompany().getCorpName(),
                items
        );
    }

    /**
     * 숨김 처리 (§4-13). 이미 숨겨진 방은 멱등.
     */
    @Transactional
    public HideResponse hide(Long roomId, Long userSeq) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(roomId));
        if (!room.isOwnedBy(userSeq)) {
            throw new ChatRoomNotFoundException(roomId);
        }
        room.hide(LocalDateTime.now());
        return new HideResponse(room.getHiddenAt());
    }
}
