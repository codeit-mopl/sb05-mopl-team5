package com.mopl.api.domain.playlist.repository;

import com.mopl.api.domain.playlist.entity.PlaylistContent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, UUID> {

    Optional<PlaylistContent> findByPlaylistIdAndContentIdAndIsDeletedFalse(UUID playlistId, UUID contentId);

    boolean existsByPlaylistIdAndContentIdAndIsDeletedFalse(UUID playlistId, UUID contentId);

    List<PlaylistContent> findByPlaylistIdAndIsDeletedFalse(UUID playlistId);

    @Query("SELECT pc FROM PlaylistContent pc " +
           "JOIN FETCH pc.content c " +
           "WHERE pc.playlist.id IN :playlistIds " +
           "AND pc.isDeleted = false " +
           "AND c.isDeleted = false")
    List<PlaylistContent> findByPlaylistIdInAndIsDeletedFalse(@Param("playlistIds") List<UUID> playlistIds);
}