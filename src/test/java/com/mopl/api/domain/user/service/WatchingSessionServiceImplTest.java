package com.mopl.api.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.user.dto.event.WatchingSessionChangeEvent;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.WatchingSession;
import com.mopl.api.domain.user.exception.watching.detail.WatchingSessionNotFoundException;
import com.mopl.api.domain.user.mapper.WatchingSessionMapper;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.domain.user.repository.WatchingSessionRedisRepository;
import com.mopl.api.domain.user.repository.WatchingSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WatchingSessionServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    ContentRepository contentRepository;
    @Mock
    WatchingSessionRepository watchingSessionRepository;
    @Mock
    WatchingSessionRedisRepository watchingSessionCacheRepository;
    @Mock
    WatchingSessionMapper watchingSessionMapper;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    WatchingSessionServiceImpl watchingSessionService;

    UUID watcherId;
    User watcher;
    UUID contentId;
    Content content;
    WatchingSession session;

    @BeforeEach
    void setUp() {
        watcherId = UUID.randomUUID();
        contentId = UUID.randomUUID();
        watcher = mock(User.class);
        content = mock(Content.class);
        session = new WatchingSession(watcher, content);

        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(session, "createdAt", LocalDateTime.now());
    }

    @Test
    @DisplayName("단일 세션 조회 성공: DB에서 데이터를 가져온다")
    void getWatchingSession_Success() {
        given(watchingSessionRepository.findAllByWatcher_Id(watcherId)).willReturn(List.of(session));
        given(watchingSessionMapper.toDto(session)).willReturn(mock(WatchingSessionDto.class));

        WatchingSessionDto result = watchingSessionService.getWatchingSession(watcherId);

        assertThat(result).isNotNull();
        then(watchingSessionRepository).should()
                                       .findAllByWatcher_Id(watcherId);
    }

    @Test
    @DisplayName("커서 기반 목록 조회: 다음 페이지가 있는 경우")
    void getWatchingSession_WithCursor_HasNext() {
        int limit = 2;
        WatchingSessionSearchRequest request = WatchingSessionSearchRequest.builder()
                                                                           .limit(limit)
                                                                           .sortBy(
                                                                               WatchingSessionSearchRequest.SortBy.createdAt)
                                                                           .sortDirection(
                                                                               WatchingSessionSearchRequest.SortDirection.DESCENDING)
                                                                           .build();

        List<WatchingSession> sessions = List.of(session, session, session);
        given(watchingSessionRepository.searchSessions(contentId, request)).willReturn(sessions);
        given(watchingSessionCacheRepository.countWatchers(contentId)).willReturn(10L);

        List<WatchingSessionDto> mockDtoList = List.of(mock(WatchingSessionDto.class), mock(WatchingSessionDto.class));
        given(watchingSessionMapper.toDtoList(any())).willReturn(mockDtoList);

        CursorResponseWatchingSessionDto result = watchingSessionService.getWatchingSession(contentId, request);

        assertThat(result.hasNext()).isTrue();
        assertThat(result.data()).hasSize(limit);
        assertThat(result.totalCount()).isEqualTo(10L);
        assertThat(result.nextCursor()).isNotNull();
    }

    @Test
    @DisplayName("세션 참여: 신규 유저인 경우 DB/Redis 저장 및 이벤트 발행")
    void joinWatchingSession_NewUser_Success() {

        given(userRepository.findById(watcherId)).willReturn(Optional.of(watcher));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(watchingSessionRepository.findByContent_IdAndWatcher_Id(contentId, watcherId)).willReturn(Optional.empty());
        given(watchingSessionRepository.saveAndFlush(any())).willReturn(session);
        given(watchingSessionCacheRepository.countWatchers(contentId)).willReturn(1L);

        watchingSessionService.joinWatchingSession(contentId, watcherId);

        then(watchingSessionRepository).should()
                                       .saveAndFlush(any());
        then(eventPublisher).should()
                            .publishEvent(any(WatchingSessionChangeEvent.class));
    }

    @Test
    @DisplayName("세션 참여: 이미 참여 중인 유저인 경우 추가 저장 없이 기존 정보 반환")
    void joinWatchingSession_ExistingUser_ReturnCurrent() {

        given(userRepository.findById(watcherId)).willReturn(Optional.of(watcher));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(watchingSessionRepository.findByContent_IdAndWatcher_Id(contentId, watcherId)).willReturn(
            Optional.of(session));

        watchingSessionService.joinWatchingSession(contentId, watcherId);

        then(watchingSessionRepository).should(never())
                                       .save(any());
        then(watchingSessionCacheRepository).should(never())
                                            .addWatcher(any(), any());
        then(eventPublisher).should(never())
                            .publishEvent(any());
    }

    @Test
    @DisplayName("세션 이탈: 정상 처리")
    void leaveWatchingSession_Success() {

        given(watchingSessionRepository.findByContent_IdAndWatcher_Id(contentId, watcherId)).willReturn(
            Optional.of(session));
        given(watchingSessionCacheRepository.countWatchers(contentId)).willReturn(5L);

        watchingSessionService.leaveWatchingSession(contentId, watcherId);

        then(watchingSessionRepository).should()
                                       .delete(session);
        then(eventPublisher).should()
                            .publishEvent(any(WatchingSessionChangeEvent.class));
    }

    @Test
    @DisplayName("세션 이탈: 존재하지 않는 세션일 경우 예외 발생")
    void leaveWatchingSession_NotFound_ThrowsException() {

        given(watchingSessionRepository.findByContent_IdAndWatcher_Id(contentId, watcherId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> watchingSessionService.leaveWatchingSession(contentId, watcherId)).isInstanceOf(
            WatchingSessionNotFoundException.class);

        then(watchingSessionRepository).should(never())
                                       .delete(any());
        then(watchingSessionCacheRepository).should(never())
                                            .removeWatcher(any(), any());
    }
}