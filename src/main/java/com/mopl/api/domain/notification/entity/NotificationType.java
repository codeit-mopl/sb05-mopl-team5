package com.mopl.api.domain.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {
    ROLE_CHANGED("권한 변경"),
    MY_PLAYLIST_SUBSCRIBED("내 플레이리스트 구독"),
    PLAYLIST_CONTENT_ADDED("구독 중인 플레이리스트에 콘텐츠 추가"),
    FOLLOWEE_PLAYLIST_CREATED("팔로우한 사용자가 플레이리스트 생성"),
    FOLLOWEE_WATCHING_STARTED("팔로우한 사용자가 콘텐츠 시청 시작"),
    NEW_FOLLOWER("다른 사용자가 나를 팔로우"),
    DM_RECEIVED("DM 수신");

    private final String notificationName;
}