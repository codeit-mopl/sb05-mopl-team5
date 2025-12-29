package com.mopl.api.domain.playlist.entity;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.global.common.entity.BaseDeletableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "playlist_contents",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_PLAYLIST_CONTENTS", columnNames = {"playlist_id", "content_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistContent extends BaseDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;
}
