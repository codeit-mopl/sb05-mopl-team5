package com.mopl.api.domain.playlist.repository;

import com.mopl.api.domain.playlist.entity.Playlist;
import com.mopl.api.domain.playlist.repository.impl.PlaylistRepositoryCustom;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID>, PlaylistRepositoryCustom {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Playlist p SET p.subscriberCount = p.subscriberCount + 1 WHERE p.id = :playlistId")
    void incrementSubscriberCount(@Param("playlistId") UUID playlistId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Playlist p SET p.subscriberCount = p.subscriberCount - 1 WHERE p.id = :playlistId AND p.subscriberCount > 0")
    void decrementSubscriberCount(@Param("playlistId") UUID playlistId);
}