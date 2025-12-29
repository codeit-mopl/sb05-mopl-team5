package com.mopl.api.domain.playlist.entity;

import com.mopl.api.domain.user.entity.User;
import com.mopl.api.global.common.entity.BaseDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "playlists")
@Getter
public class Playlist extends BaseDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long subscriberCount = 0L;

    public static Playlist create(User owner, String title, String description) {
        Playlist playlist = new Playlist();
        playlist.owner = owner;
        playlist.title = title;
        playlist.description = description;
        playlist.subscriberCount = 0L;
        return playlist;
    }

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void incrementSubscriberCount() {
        this.subscriberCount++;
    }

    public void decrementSubscriberCount() {
        if (this.subscriberCount > 0) {
            this.subscriberCount--;
        }
    }
}
