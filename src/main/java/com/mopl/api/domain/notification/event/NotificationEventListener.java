package com.mopl.api.domain.notification.event;

import com.mopl.api.domain.follow.entity.Follow;
import com.mopl.api.domain.follow.repository.FollowRepository;
import com.mopl.api.domain.notification.dto.event.DmReceivedEvent;
import com.mopl.api.domain.notification.dto.event.FolloweePlaylistCreatedEvent;
import com.mopl.api.domain.notification.dto.event.FolloweeWatchingStartedEvent;
import com.mopl.api.domain.notification.dto.event.NewFollowerEvent;
import com.mopl.api.domain.notification.dto.event.NotificationCreatedEvent;
import com.mopl.api.domain.notification.dto.event.PlaylistSubscribedEvent;
import com.mopl.api.domain.notification.dto.event.RoleChangedEvent;
import com.mopl.api.domain.notification.dto.event.SubscribingPlaylistContentAddedEvent;
import com.mopl.api.domain.notification.dto.request.NotificationCreateRequest;
import com.mopl.api.domain.notification.service.NotificationService;
import com.mopl.api.domain.playlist.entity.Subscription;
import com.mopl.api.domain.playlist.repository.SubscriptionRepository;
import com.mopl.api.domain.sse.service.SseService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final SseService sseService;
    private final SubscriptionRepository subscriptionRepository;
    private final FollowRepository followRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreatedEvent(NotificationCreatedEvent event) {
        // SSE 알림 발송
        sseService.send(
            Set.of(event.notification()
                        .receiverId()),
            "notifications",
            event.notification()
        );

        log.info("NotificationCreatedEvent 처리 receiver={} notification={}",
            event.notification()
                 .receiverId(),
            event.notification()
                 .id());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoleChangedEvent(RoleChangedEvent event) {
        notificationService.addNotification(NotificationCreateRequest.builder()
                                                                     .receiverId(event.userId())
                                                                     .title("내 권한이 변경되었어요.")
                                                                     .content(
                                                                         "내 권한이 [" + event.beforeRole() + "]에서 "
                                                                             + "[" + event.currentRole()
                                                                             + "](으)로 변경되었어요.")
                                                                     .build());

        log.info("RoleChangedEvent 처리 완료: receiverId={}", event.userId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNewFollowerEvent(NewFollowerEvent event) {
        notificationService.addNotification(NotificationCreateRequest.builder()
                                                                     .receiverId(event.followeeId())
                                                                     .title(event.followerName() + "님이 나를 팔로우 했어요.")
                                                                     .build());

        log.info("NewFollowerEvent 처리 완료: receiver(followee)Id={}, followerId={}", event.followeeId(),
            event.followerId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDmReceivedEvent(DmReceivedEvent event) {
        sseService.send(
            Set.of(event.receiverId()),
            "direct-messages",
            event.directMessageDto()
        );

        notificationService.addNotification(NotificationCreateRequest.builder()
                                                                     .receiverId(event.receiverId())
                                                                     .title("[DM] " + event.senderName())
                                                                     .content(event.content())
                                                                     .build());

        log.info("DmReceivedEvent 처리 완료: conversationId={}, receiverId={}, senderId={}", event.conversationId(),
            event.receiverId(), event.senderId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPlaylistSubscribedEvent(PlaylistSubscribedEvent event) {
        notificationService.addNotification(NotificationCreateRequest.builder()
                                                                     .receiverId(event.ownerId())
                                                                     .title("플레이리스트 [" + event.playlistTitle()
                                                                         + "](이)가 구독되었어요.")
                                                                     .content(event.subscriberName() + "님이 "
                                                                         + "[" + event.playlistTitle() + "] "
                                                                         + event.playlistDescription()
                                                                         + "(을)를 구독했어요.")
                                                                     .build());

        log.info("PlaylistSubscribedEvent 처리 완료: playlistId={}, ownerId={}, subscriberId={}",
            event.playlistId(), event.ownerId(), event.subscriberId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscribingPlaylistContentAddedEvent(SubscribingPlaylistContentAddedEvent event) {
        List<Subscription> subscriptions = subscriptionRepository.findSubscriptionsByPlaylistId(event.playlistId());

        if (subscriptions.isEmpty()) {
            return;
        }

        subscriptions.forEach(s ->
            notificationService.addNotification(NotificationCreateRequest.builder()
                                                                         .receiverId(s.getUser()
                                                                                      .getId())
                                                                         .title("구독 중인 플레이리스트에 콘텐츠가 추가되었어요.")
                                                                         .content("[" + event.playlistTitle() + "] "
                                                                             + event.playlistDescription() + "에 "
                                                                             + event.contentTitle() + "(이)가 추가되었어요.")
                                                                         .build())
        );

        log.info("SubscribingPlaylistContentAddedEvent 처리 완료: playlistId={}, contentId={}",
            event.playlistId(), event.contentId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFolloweePlaylistCreated(FolloweePlaylistCreatedEvent event) {
        List<Follow> follows = followRepository.findFollowsByFolloweeId(event.ownerId());

        if (follows.isEmpty()) {
            return;
        }

        follows.forEach(f ->
            notificationService.addNotification(NotificationCreateRequest.builder()
                                                                         .receiverId(f.getFollower()
                                                                                      .getId())
                                                                         .title(event.ownerName() + "님이 플레이리스트를 만들었어요.")
                                                                         .content("[" + event.playlistTitle() + "] "
                                                                             + event.playlistDescription())
                                                                         .build())
        );

        log.info("FolloweePlaylistCreatedEvent 처리 완료: playlistId={}, followeeId={}", event.playlistId(),
            event.ownerId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFolloweeWatchingStartedEvent(FolloweeWatchingStartedEvent event) {
        List<Follow> follows = followRepository.findFollowsByFolloweeId(event.watcherId());

        if (follows.isEmpty()) {
            return;
        }

        follows.forEach(f ->
            notificationService.addNotification(NotificationCreateRequest.builder()
                                                                         .receiverId(f.getFollower()
                                                                                      .getId())
                                                                         .title(f.getFollowee()
                                                                                 .getName() + "님이 콘텐츠 시청을 시작했어요.")
                                                                         .content("[" + event.contentTitle() + "]")
                                                                         .build())
        );

        log.info("FolloweeWatchingStartedEvent 처리 완료: watchingSessionId={}, contentId={}", event.watchingSessionId(),
            event.contentId());
    }

}
