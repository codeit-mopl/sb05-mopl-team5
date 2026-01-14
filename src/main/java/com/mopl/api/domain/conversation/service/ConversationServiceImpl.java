package com.mopl.api.domain.conversation.service;

import com.mopl.api.domain.conversation.dto.request.ConversationRequestDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationLatestMessage;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationListRow;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationReceiver;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationResponseDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationSend;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationWith;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageLastestMessage;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageReceiver;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageResponseDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageSender;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageWith;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageWithDto;
import com.mopl.api.domain.conversation.entity.Conversation;
import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.conversation.mapper.ConversationMapper;
import com.mopl.api.domain.conversation.mapper.DirectMessageMapper;
import com.mopl.api.domain.conversation.repository.ConversationParticipantRepository;
import com.mopl.api.domain.conversation.repository.ConversationRepository;
import com.mopl.api.domain.conversation.repository.DirectMessageRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;

    private final ConversationMapper conversationMapper;
    private final DirectMessageMapper directMessageMapper;

    // -------------------------
    // 1) 1:1 대화방 생성 (없으면 생성, 있으면 existing 반환)
    // -------------------------
    @Override
    @Transactional
    public ConversationDto createConversation(UUID me, ConversationRequestDto withUserId) {
        UUID other = withUserId.withUserId();

        // 1. 유효성 검사 (생략)

        // 2. 이미 존재하는 방 확인
        UUID existingConversationId = conversationRepository.findOneToOneConversationId(Set.of(me, other))
                                                            .orElse(null);

        // 🔥 [수정] 이미 방이 있다면? -> '빈 방'을 주면 안 되고, '상세 정보(메시지 포함)'를 줘야 함!
        if (existingConversationId != null) {
            // 꿀팁: 아까 만든 conversationCheck 메서드를 재사용하면 로직 중복을 없앨 수 있습니다.
            // (같은 클래스 내에 있다면 호출, 다른 서비스라면 주입받거나 로직 복사)
            return conversationCheck(me, existingConversationId);
        }

        // 3. 새 방 생성 (여기는 메시지가 없으니 null/false가 맞음)
        User meUser = userRepository.getReferenceById(me);
        User otherUser = userRepository.findById(other)
                                       .orElseThrow(() -> new IllegalArgumentException("상대 유저가 존재하지 않습니다."));

        Conversation newConversation = conversationRepository.save(Conversation.create());
        conversationParticipantRepository.save(new ConversationParticipant(newConversation, meUser));
        conversationParticipantRepository.save(new ConversationParticipant(newConversation, otherUser));

        // 4. 새 방은 비어있으므로 toEmptyDto 사용 (이건 괜찮음)
        return conversationMapper.toEmptyDto(newConversation, otherUser);
    }

    // -------------------------
    // 2) 대화 목록 조회 (repo custom 최적쿼리 사용)
    // -------------------------
    @Override
    public ConversationResponseDto getConversationList(
        UUID me,
        String keywordLike,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
    ) {
        // 1. 커서 파싱 & 리스트 조회 & 개수 조회
        LocalDateTime cursorTime = parseCursor(cursor);
        List<ConversationListRow> rows = conversationRepository.findConversationList(
            me, keywordLike, cursorTime, idAfter, limit, sortDirection
        );
        long totalCount = conversationRepository.countConversationList(me, keywordLike);

        // 2. hasNext 판단 및 자르기
        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        // 3. DTO 리스트 변환 (기존 Mapper 활용)
        List<ConversationDto> data = rows.stream()
                                         .map(conversationMapper::toDto)
                                         .toList();

        // 4. 다음 커서 계산
        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !rows.isEmpty()) {
            ConversationListRow lastRow = rows.get(rows.size() - 1);

            // 포맷 고정 (잘하셨습니다!)
            if (lastRow.lastMessageCreatedAt() != null) {
                nextCursor = lastRow.lastMessageCreatedAt()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"));
            }
            nextIdAfter = lastRow.conversationId();
        }

        // 5. 🔥 [수정] Mapper에게 조립 위임! (Builder 제거)
        return conversationMapper.toResponseDto(
            data,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            sortBy,
            sortDirection
        );
    }

    // -------------------------
    // 3) 읽음 처리 (DB에서 메시지 createdAt만 가져와 조건 업데이트)
    // -------------------------
    @Override
    @Transactional
    public void conversationRead(UUID userId, UUID conversationId, UUID directMessageId) {

        ensureParticipant(conversationId, userId);

        LocalDateTime messageCreatedAt = directMessageRepository
            .findCreatedAtByIdAndConversationId(directMessageId, conversationId)
            .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));

        // 성능: "현재 lastReadAt보다 더 최신일 때만 갱신"을 DB에서 처리
        conversationParticipantRepository.updateLastReadAtIfNewer( conversationId, userId,messageCreatedAt);
    }

    // -------------------------
    // 4) 대화방 단건 조회 (권한 + 최신 메시지 + hasUnread)
    // -------------------------
    @Override
    @Transactional(readOnly = true)
    public ConversationDto conversationCheck(UUID me, UUID conversationId) {

        // 1. 대화방 존재 확인
        Conversation conversation = conversationRepository.findById(conversationId)
                                                          .orElseThrow(() -> new IllegalStateException("존재하지 않는 대화방입니다."));

        // 2. 참여자 조회 (+ EntityGraph로 User 패치조인 권장)
        List<ConversationParticipant> participants = conversationParticipantRepository.findAllByConversationId(conversationId);

        // 3. 내 참여 정보 검증
        ConversationParticipant myParticipant = participants.stream()
                                                            .filter(p -> p.getUser().getId().equals(me))
                                                            .findFirst()
                                                            .orElseThrow(() -> new AccessDeniedException("대화방 참여자가 아닙니다."));

        // 4. 상대방 찾기
        User otherUser;
        if (participants.size() == 1) {
            otherUser = myParticipant.getUser(); // 나와의 채팅
        } else {
            otherUser = participants.stream()
                                    .map(ConversationParticipant::getUser)
                                    .filter(u -> !u.getId().equals(me))
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalStateException("대화 상대방을 찾을 수 없습니다."));
        }

        // 5. 마지막 메시지 조회
        DirectMessage lastMessage = directMessageRepository.findLatestByConversationId(conversationId)
                                                           .orElse(null);

        // 6. 안 읽음 여부 계산
        boolean hasUnread = false;
        if (lastMessage != null) {
            boolean isMyMessage = lastMessage.getSender().getId().equals(me);
            LocalDateTime lastReadAt = myParticipant.getLastReadAt();

            if (!isMyMessage) {
                hasUnread = (lastReadAt == null) || lastReadAt.isBefore(lastMessage.getCreatedAt());
            }
        }

        // 7. 🔥 [핵심] Mapper 한 줄로 끝내기!
        // 기존의 길었던 builder() 코드가 사라집니다.
        return conversationMapper.toCheckDto(conversation, otherUser, lastMessage, hasUnread);
    }



    // -------------------------
    // 5) DM 목록 조회 (✅ 참가자 검증 + repo seek pagination)
    // -------------------------
    @Override
    @Transactional
    public DirectMessageResponseDto getDirectMessageList(
        UUID me,
        UUID conversationId,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
    ) {
        // 1. 권한 체크
        ensureParticipant(conversationId, me);

        // 2. 커서 파싱 & 리스트 조회 & 전체 개수 조회
        LocalDateTime cursorTime = parseCursor(cursor);
        List<DirectMessage> list = directMessageRepository.findMessageList(
            conversationId, cursorTime, idAfter, limit, sortDirection
        );
        long totalCount = directMessageRepository.countMessageList(conversationId);

        // 3. 읽음 처리
        if (!list.isEmpty()) {
            boolean isDesc = "DESCENDING".equalsIgnoreCase(sortDirection);
            DirectMessage latestMessage = isDesc ? list.get(0) : list.get(list.size() - 1);

            conversationParticipantRepository.updateLastReadAtIfNewer(
                conversationId, me, latestMessage.getCreatedAt()
            );
        }

        // 4. hasNext 계산 및 리스트 자르기
        boolean hasNext = list.size() > limit;
        if (hasNext) {
            list = list.subList(0, limit);
        }

        // 5. DTO 리스트 변환 (기존 Mapper 메서드 활용)
        List<DirectMessageDto> data = list.stream()
                                          .map(directMessageMapper::toDto)
                                          .toList();

        // 6. 다음 커서 계산
        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !list.isEmpty()) {
            DirectMessage last = list.get(list.size() - 1);
            // 포맷 고정
            nextCursor = last.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"));
            nextIdAfter = last.getId();
        }

        // 7. 🔥 [수정] Mapper에게 조립 위임! (Builder 코드 제거)
        return directMessageMapper.toResponseDto(
            data,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            sortDirection,
            sortBy
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DirectMessageWithDto getDirectMessageWith(UUID me, UUID other) {
        // 1. 유효성 검사
        if (other == null) throw new IllegalArgumentException("withUserId는 필수입니다.");
        if (me.equals(other)) throw new IllegalArgumentException("자기 자신과 대화할 수 없습니다.");

        // 2. 상대방 유저 정보 조회
        User otherUser = userRepository.findById(other)
                                       .orElseThrow(() -> new IllegalArgumentException("상대 유저가 존재하지 않습니다."));

        // 🔥 [수정] Mapper 사용으로 코드가 깔끔해짐
        DirectMessageWith withUserDto = directMessageMapper.toWithDto(otherUser);

        // 3. 1:1 대화방 ID 찾기
        UUID conversationId = conversationRepository.findOneToOneConversationId(Set.of(me, other))
                                                    .orElse(null);

        // 4. 대화방이 없을 경우 (빈 방 리턴)
        if (conversationId == null) {
            return DirectMessageWithDto.builder()
                                       .id(null)
                                       .with(withUserDto)
                                       .lastestMessage(null)
                                       .hasUnread(false)
                                       .build();
        }

        // 5. 참여자 검증 및 읽은 시간 조회
        LocalDateTime myLastReadAt = conversationParticipantRepository
            .findLastReadAtByConversationIdAndUserId(conversationId, me)
            .orElseThrow(() -> new AccessDeniedException("대화방 참여자가 아닙니다."));

        // 6. 마지막 메시지 조회
        DirectMessage latest = directMessageRepository.findLatestByConversationId(conversationId)
                                                      .orElse(null);

        // 7. 안 읽음 여부 판별
        boolean hasUnread = false;
        DirectMessageLastestMessage latestDto = null; // 오타 주의: Lastest -> Latest 권장

        if (latest != null) {
            latestDto = directMessageMapper.toLatestMessageDto(latest);

            // ✅ 로직 좋음: 내가 보낸 게 아니고(상대방이 보냈고) && 내가 읽은 시간보다 최신이면 -> 안 읽음
            boolean iAmSender = latest.getSender().getId().equals(me);
            boolean newerThanRead = (myLastReadAt == null) || latest.getCreatedAt().isAfter(myLastReadAt);

            hasUnread = !iAmSender && newerThanRead;
        }

        // 8. 최종 반환
        return DirectMessageWithDto.builder()
                                   .id(conversationId)
                                   .with(withUserDto)
                                   .lastestMessage(latestDto)
                                   .hasUnread(hasUnread)
                                   .build();
    }




    /**
     * 참가자 검증 - repo에 exists 메서드가 없더라도
     * findLastReadAtByConversationIdAndUserId로 검증 가능.
     */
    private void ensureParticipant(UUID conversationId, UUID me) {
        boolean ok = conversationParticipantRepository
            .findLastReadAtByConversationIdAndUserId(conversationId, me)
            .isPresent();
        if (!ok) {
            throw new AccessDeniedException("대화방 참여자가 아닙니다.");
        }
    }

    private LocalDateTime parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(cursor.trim());
        } catch (Exception e) {
            // ✅ cursor 잘못 들어오면 400이 나게 만드는 게 명세/테스트에 유리
            throw new IllegalArgumentException("cursor 형식이 올바르지 않습니다. (예: 2026-01-01T12:30:00)");
        }
    }
}
