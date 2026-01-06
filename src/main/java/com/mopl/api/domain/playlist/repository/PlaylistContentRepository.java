package com.mopl.api.domain.playlist.repository;

import com.mopl.api.domain.playlist.entity.PlaylistContent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, UUID> {

    Optional<PlaylistContent> findByPlaylistIdAndContentId(UUID playlistId, UUID contentId);

    boolean existsByPlaylistIdAndContentIdAndIsDeletedFalse(UUID playlistId, UUID contentId);

    List<PlaylistContent> findByPlaylistIdAndIsDeletedFalse(UUID playlistId);
}