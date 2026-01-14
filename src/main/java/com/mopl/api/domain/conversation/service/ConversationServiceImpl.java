package com.mopl.api.domain.conversation.service;

import com.mopl.api.domain.conversation.dto.request.ConversationRequestDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationListRow;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationResponseDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationSummary;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageLastestMessage;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageResponseDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageWith;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageWithDto;
import com.mopl.api.domain.conversation.entity.Conversation;
import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.conversation.exception.ConversationNotFoundException;
import com.mopl.api.domain.conversation.mapper.ConversationConverter;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final ConversationConverter conversationConverter;

    // -------------------------
    // 1) 1:1 대화방 생성 (없으면 생성, 있으면 existing 반환)
    // -------------------------
    @Override
    @Transactional
    public ConversationDto createConversation(UUID me, ConversationRequestDto withUserId) {
        UUID other = withUserId.withUserId();

        // 1. 유효성 검사
        if (other == null) {
            throw new IllegalArgumentException("withUserId는 필수입니다.");
        }
        if (me.equals(other)) {
            throw new IllegalArgumentException("자기 자신과 대화할 수 없습니다.");
        }

        // 2. 상대방 정보 조회 (어차피 필요함)
        User otherUser = userRepository.findById(other)
                                       .orElseThrow(() -> new IllegalArgumentException("상대 유저가 존재하지 않습니다."));

        // 3. 이미 존재하는 방 확인
        UUID existingConversationId = conversationRepository.findOneToOneConversationId(Set.of(me, other))
                                                            .orElse(null);

        // 🔥 [Case 1] 이미 방이 있는 경우 -> "상세 정보"를 조회해서 리턴 (내부 호출 제거)
        if (existingConversationId != null) {
            Conversation conversation = conversationRepository.findById(existingConversationId)
                                                              .orElseThrow(() -> new IllegalStateException(
                                                                  "데이터 무결성 오류: 방 ID는 있는데 데이터가 없음"));

            // (1) 마지막 메시지 조회 (JPA 메서드 사용)
            DirectMessage lastMessage = directMessageRepository
                .findTopByConversationIdOrderByCreatedAtDesc(existingConversationId)
                .orElse(null);

            // (2) 안 읽음 여부 계산 (참여자 정보 필요)
            ConversationParticipant myParticipant = conversationParticipantRepository
                .findByConversationIdAndUserId(existingConversationId, me)
                .orElseThrow(() -> new IllegalStateException("참여자 정보 누락"));

            boolean hasUnread = false;
            if (lastMessage != null) {
                boolean isMyMessage = lastMessage.getSender().getId().equals(me);
                LocalDateTime lastReadAt = myParticipant.getLastReadAt();
                if (!isMyMessage) {
                    hasUnread = (lastReadAt == null) || lastReadAt.isBefore(lastMessage.getCreatedAt());
                }
            }

            // (3) Mapper로 DTO 변환
            return conversationMapper.toCheckDto(conversation, otherUser, lastMessage, hasUnread);
        }

        // 🔥 [Case 2] 방이 없는 경우 -> "새로 생성" 후 "빈 방 정보" 리턴
        User meUser = userRepository.getReferenceById(me);

        Conversation newConversation = conversationRepository.save(Conversation.create());
        conversationParticipantRepository.save(new ConversationParticipant(newConversation, meUser));
        conversationParticipantRepository.save(new ConversationParticipant(newConversation, otherUser));

        return conversationMapper.toEmptyDto(newConversation, otherUser);
    }

    // -------------------------
    // 2) 대화 목록 조회
    // -------------------------
    @Override
    public ConversationResponseDto getConversationList(
        UUID me, String keywordLike, String cursor, UUID idAfter, int limit, String sortDirection, String sortBy
    ) {
        // 1. 커서 파싱
        LocalDateTime cursorTime = parseCursor(cursor);

        // 2. Pageable 생성 (limit + 1로 다음 페이지 존재 여부 확인)
        Pageable pageable = PageRequest.of(0, limit + 1);

        // 3. Repository 호출 (반환 타입이 Interface인 Summary로 변경됨)
        List<ConversationSummary> rows = conversationRepository.findConversationList(
            me, keywordLike, cursorTime, pageable
        );

        // 4. 전체 카운트 조회
        long totalCount = conversationRepository.countConversationList(me, keywordLike);

        // 5. 다음 페이지 여부 확인 및 리스트 자르기
        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        // 6. 변환 (Converter 사용)
        List<ConversationDto> data = rows.stream()
                                         .map(conversationConverter::toDto) // Mapper 대신 Converter 사용
                                         .toList();

        // 7. 다음 커서 계산 (Interface Getter 사용)
        String nextCursor = null;
        UUID nextIdAfter = null;

        if (!rows.isEmpty()) {
            ConversationSummary lastRow = rows.get(rows.size() - 1);

            // Interface의 Getter 메서드(get...) 사용
            if (lastRow.getLastMessageCreatedAt() != null) {
                nextCursor = lastRow.getLastMessageCreatedAt()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"));
            }
            nextIdAfter = lastRow.getConversationId();
        }

        // 8. 응답 DTO 생성 (Builder 직접 사용)
        return ConversationResponseDto.builder()
                                      .data(data)
                                      .nextCursor(nextCursor)
                                      .nextIdAfter(nextIdAfter)
                                      .hasNext(hasNext)
                                      .totalCount(totalCount)
                                      .sortBy(sortBy)
                                      .sortDirection(sortDirection)
                                      .build();
    }
    // -------------------------
    // 3) 읽음 처리
    // -------------------------
    @Override
    @Transactional
    public void conversationRead(UUID userId, UUID conversationId, UUID directMessageId) {
        // 참여자 검증 (새로운 Repository 메서드 사용 가능, 혹은 기존 로직 유지)
        if (!conversationParticipantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new AccessDeniedException("대화방 참여자가 아닙니다.");
        }

        LocalDateTime messageCreatedAt = directMessageRepository
            .findCreatedAtByIdAndConversationId(directMessageId, conversationId)
            .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));

        conversationParticipantRepository.updateLastReadAtIfNewer(conversationId, userId, messageCreatedAt);
    }

    // -------------------------
    // 4) 대화방 단건 조회
    // -------------------------
    @Override
    @Transactional(readOnly = true)
    public ConversationDto conversationCheck(UUID me, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                                                          .orElseThrow(
                                                              () -> new IllegalStateException("존재하지 않는 대화방입니다."));

        List<ConversationParticipant> participants = conversationParticipantRepository.findAllByConversationId(
            conversationId);

        ConversationParticipant myParticipant = participants.stream()
                                                            .filter(p -> p.getUser().getId().equals(me))
                                                            .findFirst()
                                                            .orElseThrow(
                                                                () -> new AccessDeniedException("대화방 참여자가 아닙니다."));

        User otherUser;
        if (participants.size() == 1) {
            otherUser = myParticipant.getUser();
        } else {
            otherUser = participants.stream()
                                    .map(ConversationParticipant::getUser)
                                    .filter(u -> !u.getId().equals(me))
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalStateException("대화 상대방을 찾을 수 없습니다."));
        }

        // JPA 메서드 사용 (Top 1)
        DirectMessage lastMessage = directMessageRepository
            .findTopByConversationIdOrderByCreatedAtDesc(conversationId)
            .orElse(null);

        boolean hasUnread = false;
        if (lastMessage != null) {
            boolean isMyMessage = lastMessage.getSender().getId().equals(me);
            LocalDateTime lastReadAt = myParticipant.getLastReadAt();
            if (!isMyMessage) {
                hasUnread = (lastReadAt == null) || lastReadAt.isBefore(lastMessage.getCreatedAt());
            }
        }

        return conversationMapper.toCheckDto(conversation, otherUser, lastMessage, hasUnread);
    }

    // -------------------------
    // 5) DM 목록 조회 (🔥 핵심 수정: QueryDSL 제거 -> JPA 분기 처리)
    // -------------------------
    @Override
    @Transactional
    public DirectMessageResponseDto getDirectMessageList(
        UUID me, UUID conversationId, String cursor, UUID idAfter, int limit, String sortDirection, String sortBy
    ) {
        // 1. 권한 체크 (JPA 메서드 사용)
        if (!conversationParticipantRepository.existsByConversationIdAndUserId(conversationId, me)) {
            throw new AccessDeniedException("대화방 참여자가 아닙니다.");
        }

        // 2. 리스트 조회 (ASC / DESC 분기)
        LocalDateTime cursorTime = parseCursor(cursor);

        // Pageable로 Limit 처리 (+1 해서 hasNext 체크용)
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<DirectMessage> list;

        if ("DESCENDING".equalsIgnoreCase(sortDirection)) {
            list = directMessageRepository.findMessageListDesc(conversationId, cursorTime, idAfter, pageable);
        } else {
            list = directMessageRepository.findMessageListAsc(conversationId, cursorTime, idAfter, pageable);
        }

        // 전체 개수 조회 (메서드명 변경됨)
        long totalCount = directMessageRepository.countByConversationId(conversationId);

        // 3. 읽음 처리
        if (!list.isEmpty()) {
            boolean isDesc = "DESCENDING".equalsIgnoreCase(sortDirection);
            // 리스트가 뒤집히기 전이므로 0번이 최신(DESC 기준)
            DirectMessage latestMessage = isDesc ? list.get(0) : list.get(list.size() - 1);
            conversationParticipantRepository.updateLastReadAtIfNewer(conversationId, me, latestMessage.getCreatedAt());
        }

        // 4. hasNext 계산 및 자르기
        boolean hasNext = list.size() > limit;
        if (hasNext) {
            list = new java.util.ArrayList<>(list.subList(0, limit));
        }

        // 5. 커서 계산 (뒤집기 전에 미리!)
        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !list.isEmpty()) {
            DirectMessage last = list.get(list.size() - 1);
            nextCursor = last.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"));
            nextIdAfter = last.getId();
        }

        if (!list.isEmpty()) {
            log.info("DEBUG [Before Reverse] 첫번째: {}, 마지막: {}",
                list.get(0).getCreatedAt(), list.get(list.size() - 1).getCreatedAt());
        }

        // 6. 🔥 리스트 뒤집기 (UI: 과거 -> 최신)
        if ("DESCENDING".equalsIgnoreCase(sortDirection)) {
            java.util.Collections.reverse(list);
        }

        if (!list.isEmpty()) {
            log.info("DEBUG [After  Reverse] 첫번째: {}, 마지막: {}",
                list.get(0).getCreatedAt(), list.get(list.size() - 1).getCreatedAt());
        }

        // 7. DTO 변환
        List<DirectMessageDto> data = list.stream()
                                          .map(directMessageMapper::toDto)
                                          .toList();

        return directMessageMapper.toResponseDto(
            data, nextCursor, nextIdAfter, hasNext, totalCount, sortDirection, sortBy
        );
    }

    // -------------------------
    // 6) 상대방과 대화 조회
    // -------------------------
    @Override
    @Transactional(readOnly = true)
    public DirectMessageWithDto getDirectMessageWith(UUID me, UUID other) {
        if (other == null) {
            throw new IllegalArgumentException("withUserId는 필수입니다.");
        }
        if (me.equals(other)) {
            throw new IllegalArgumentException("자기 자신과 대화할 수 없습니다.");
        }

        User otherUser = userRepository.findById(other)
                                       .orElseThrow(() -> new IllegalArgumentException("상대 유저가 존재하지 않습니다."));

        DirectMessageWith withUserDto = directMessageMapper.toWithDto(otherUser);

        UUID conversationId = conversationRepository.findOneToOneConversationId(Set.of(me, other))
                                                    .orElse(null);

        // 🔥 [핵심] 404 예외 발생 (프론트엔드 생성 유도)
        if (conversationId == null) {
            throw new ConversationNotFoundException(other);
        }

        LocalDateTime myLastReadAt = conversationParticipantRepository
            .findLastReadAtByConversationIdAndUserId(conversationId, me)
            .orElseThrow(() -> new AccessDeniedException("대화방 참여자가 아닙니다."));

        // JPA 메서드 사용
        DirectMessage latest = directMessageRepository
            .findTopByConversationIdOrderByCreatedAtDesc(conversationId)
            .orElse(null);

        boolean hasUnread = false;
        DirectMessageLastestMessage latestDto = null;

        if (latest != null) {
            latestDto = directMessageMapper.toLatestMessageDto(latest);
            boolean iAmSender = latest.getSender().getId().equals(me);
            boolean newerThanRead = (myLastReadAt == null) || latest.getCreatedAt().isAfter(myLastReadAt);
            hasUnread = !iAmSender && newerThanRead;
        }

        return DirectMessageWithDto.builder()
                                   .id(conversationId)
                                   .with(withUserDto)
                                   .lastestMessage(latestDto) // DTO 필드명 lastestMessage 주의
                                   .hasUnread(hasUnread)
                                   .build();
    }

    private LocalDateTime parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(cursor.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("cursor 형식이 올바르지 않습니다.");
        }
    }
}