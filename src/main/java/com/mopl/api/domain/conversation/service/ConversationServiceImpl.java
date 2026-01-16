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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
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

    // 1) 1:1 대화방 생성
    @Override
    @Transactional
    public ConversationDto createConversation(UUID me, ConversationRequestDto withUserId) {
        UUID other = withUserId.withUserId();

        if (other == null) throw new IllegalArgumentException("withUserId는 필수입니다.");
        if (me.equals(other)) throw new IllegalArgumentException("자기 자신과 대화할 수 없습니다.");

        User otherUser = userRepository.findById(other)
                                       .orElseThrow(() -> new IllegalArgumentException("상대 유저가 존재하지 않습니다."));

        Conversation existingConversation = conversationRepository.findOneToOneConversation(Set.of(me, other))
                                                                  .orElse(null);

        if (existingConversation != null) {
            DirectMessage lastMessage = directMessageRepository
                .findTopByConversationIdOrderByCreatedAtDesc(existingConversation.getId())
                .orElse(null);

            ConversationParticipant myParticipant = conversationParticipantRepository
                .findByConversationIdAndUserId(existingConversation.getId(), me)
                .orElseThrow(() -> new IllegalStateException("참여자 정보 누락"));

            boolean hasUnread = false;
            if (lastMessage != null) {
                boolean isMyMessage = lastMessage.getSender().getId().equals(me);
                LocalDateTime lastReadAt = myParticipant.getLastReadAt();
                if (!isMyMessage) {
                    hasUnread = (lastReadAt == null) || lastReadAt.isBefore(lastMessage.getCreatedAt());
                }
            }
            return conversationMapper.toCheckDto(existingConversation, otherUser, lastMessage, hasUnread);
        }

        User meUser = userRepository.getReferenceById(me);
        Conversation newConversation = conversationRepository.save(Conversation.create());
        conversationParticipantRepository.save(new ConversationParticipant(newConversation, meUser));
        conversationParticipantRepository.save(new ConversationParticipant(newConversation, otherUser));

        return conversationMapper.toEmptyDto(newConversation, otherUser);
    }

    // 2) 대화 목록 조회
    @Override
    public ConversationResponseDto getConversationList(
        UUID me, String keywordLike, String cursor, UUID idAfter, int limit, String sortDirection, String sortBy
    ) {
        LocalDateTime cursorTime = parseCursor(cursor);
        Pageable pageable = PageRequest.of(0, limit + 1);

        List<ConversationSummary> rows = conversationRepository.findConversationList(me, keywordLike, cursorTime, pageable);
        long totalCount = conversationRepository.countConversationList(me, keywordLike);

        boolean hasNext = rows.size() > limit;
        if (hasNext) rows = rows.subList(0, limit);

        List<ConversationDto> data = rows.stream().map(conversationConverter::toDto).toList();

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !rows.isEmpty()) {
            ConversationSummary lastRow = rows.get(rows.size() - 1);
            if (lastRow.getLastMessageCreatedAt() != null) {
                nextCursor = lastRow.getLastMessageCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"));
            }
            nextIdAfter = lastRow.getConversationId();
        }

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

    // 3) 읽음 처리
    @Override
    @Transactional
    public void conversationRead(UUID userId, UUID conversationId, UUID directMessageId) {
        // 1. 참여자 조회 (403 발생 지점 -> Repository에 @Query 추가하여 안정성 확보)
        ConversationParticipant participant = conversationParticipantRepository
            .findByConversationIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new AccessDeniedException("대화방 참여자가 아닙니다."));

        // 2. 메시지 생성 시간 조회
        LocalDateTime messageCreatedAt = directMessageRepository
            .findCreatedAtByIdAndConversationId(directMessageId, conversationId)
            .orElseThrow(() -> new IllegalArgumentException("해당 대화방에 존재하지 않는 메시지입니다."));

        // 3. 읽음 처리 업데이트 (내 읽음 시간보다 최신일 경우에만 업데이트)
        LocalDateTime currentReadAt = participant.getLastReadAt();
        if (currentReadAt == null || messageCreatedAt.isAfter(currentReadAt)) {
            participant.updateLastReadAt(messageCreatedAt);
        }
    }

    // 4) 대화방 단건 조회
    @Override
    @Transactional(readOnly = true)
    public ConversationDto conversationCheck(UUID me, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                                                          .orElseThrow(() -> new IllegalStateException("존재하지 않는 대화방입니다."));

        List<ConversationParticipant> participants = conversationParticipantRepository.findAllByConversationId(conversationId);
        ConversationParticipant myParticipant = participants.stream()
                                                            .filter(p -> p.getUser().getId().equals(me))
                                                            .findFirst()
                                                            .orElseThrow(() -> new AccessDeniedException("대화방 참여자가 아닙니다."));

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

    // 5) DM 목록 조회
    @Override
    @Transactional
    public DirectMessageResponseDto getDirectMessageList(
        UUID me, UUID conversationId, String cursor, UUID idAfter, int limit, String sortDirection, String sortBy
    ) {
        ConversationParticipant myParticipant = conversationParticipantRepository
            .findByConversationIdAndUserId(conversationId, me)
            .orElseThrow(() -> new AccessDeniedException("대화방 참여자가 아닙니다."));

        LocalDateTime cursorTime = parseCursor(cursor);
        Pageable pageable = PageRequest.of(0, limit + 1);

        List<DirectMessage> list;
        if ("DESCENDING".equalsIgnoreCase(sortDirection)) {
            list = directMessageRepository.findMessageListDesc(conversationId, cursorTime, idAfter, pageable);
        } else {
            list = directMessageRepository.findMessageListAsc(conversationId, cursorTime, idAfter, pageable);
        }

        if (!list.isEmpty()) {
            boolean isDesc = "DESCENDING".equalsIgnoreCase(sortDirection);
            DirectMessage latestMessage = isDesc ? list.get(0) : list.get(list.size() - 1);

            LocalDateTime myLastReadAt = myParticipant.getLastReadAt();
            LocalDateTime messageCreatedAt = latestMessage.getCreatedAt();

            if (myLastReadAt == null || messageCreatedAt.isAfter(myLastReadAt)) {
                myParticipant.updateLastReadAt(messageCreatedAt);
            }
        }

        long totalCount = directMessageRepository.countByConversationId(conversationId);
        boolean hasNext = list.size() > limit;
        if (hasNext) {
            list = new ArrayList<>(list.subList(0, limit));
        }

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !list.isEmpty()) {
            DirectMessage last = list.get(list.size() - 1);
            nextCursor = last.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"));
            nextIdAfter = last.getId();
        }

        List<DirectMessageDto> data = list.stream().map(directMessageMapper::toDto).toList();

        return directMessageMapper.toResponseDto(
            data, nextCursor, nextIdAfter, hasNext, totalCount, sortDirection, sortBy
        );
    }

    // 6) 상대방과 대화 조회
    @Override
    @Transactional(readOnly = true)
    public DirectMessageWithDto getDirectMessageWith(UUID me, UUID other) {
        if (other == null) throw new IllegalArgumentException("withUserId는 필수입니다.");
        if (me.equals(other)) throw new IllegalArgumentException("자기 자신과 대화할 수 없습니다.");

        User otherUser = userRepository.findById(other)
                                       .orElseThrow(() -> new IllegalArgumentException("상대 유저가 존재하지 않습니다."));
        DirectMessageWith withUserDto = directMessageMapper.toWithDto(otherUser);

        Conversation conversation = conversationRepository.findOneToOneConversation(Set.of(me, other))
                                                          .orElse(null);

        if (conversation == null) throw new ConversationNotFoundException(other);

        LocalDateTime myLastReadAt = conversationParticipantRepository
            .findLastReadAtByConversationIdAndUserId(conversation.getId(), me)
            .orElseThrow(() -> new AccessDeniedException("대화방 참여자가 아닙니다."));

        DirectMessage latest = directMessageRepository
            .findTopByConversationIdOrderByCreatedAtDesc(conversation.getId())
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
                                   .id(conversation.getId())
                                   .with(withUserDto)
                                   .lastestMessage(latestDto)
                                   .hasUnread(hasUnread)
                                   .build();
    }

    private LocalDateTime parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            return LocalDateTime.parse(cursor.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("cursor 형식이 올바르지 않습니다.");
        }
    }
}