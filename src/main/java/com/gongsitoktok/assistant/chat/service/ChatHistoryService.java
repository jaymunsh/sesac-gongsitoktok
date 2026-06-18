/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/service/ChatHistoryService.java
 */
package com.gongsitoktok.assistant.chat.service;

import com.gongsitoktok.assistant.chat.dto.fastapi.MessageDto;
import com.gongsitoktok.assistant.chat.entity.QaHistory;
import com.gongsitoktok.assistant.chat.repository.QaHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 멀티턴 messages 배열 빌더 (제작요청 v6 §4-10 step 3).
 *
 * <p>FastAPI 가 stateless 이므로 Spring 이 과거 Q&A 를 모두 조립해서 보내야 한다.</p>
 *
 * <h3>messages 구성 규칙</h3>
 * <ol>
 *     <li>{@code tb_qa_history} 를 {@code createdAt ASC} 로 조회.</li>
 *     <li>각 행에서 {@code question} → role=user, {@code answer} → role=assistant 두 항목 생성.</li>
 *     <li>마지막에 새 질문을 role=user 로 append.</li>
 * </ol>
 *
 * <p>{@code answer} 는 정상 응답만 영속화되므로(가이드 §스텝 E) "추론 실패가 history 에 섞일 일은 없다" 는 가정이 유지된다.</p>
 */
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final QaHistoryRepository qaHistoryRepository;

    /**
     * 멀티턴 messages 빌드.
     *
     * @param roomId      대상 방 id
     * @param newQuestion 새 사용자 질문
     * @return user/assistant 가 교차로 들어간 messages 배열 (마지막 항목은 항상 새 user 메시지)
     */
    @Transactional(readOnly = true)
    public List<MessageDto> buildMessages(Long roomId, String newQuestion) {
        List<QaHistory> past = qaHistoryRepository.findByChatRoomRoomIdOrderByCreatedAtAsc(roomId);
        List<MessageDto> messages = new ArrayList<>(past.size() * 2 + 1);
        for (QaHistory qa : past) {
            messages.add(MessageDto.user(qa.getQuestion()));
            messages.add(MessageDto.assistant(qa.getAnswer()));
        }
        messages.add(MessageDto.user(newQuestion));
        return messages;
    }
}
