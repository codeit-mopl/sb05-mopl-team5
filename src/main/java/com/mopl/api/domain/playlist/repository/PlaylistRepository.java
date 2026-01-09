package com.mopl.api.domain.playlist.repository;

import com.mopl.api.domain.playlist.entity.Playlist;
import com.mopl.api.domain.playlist.repository.impl.PlaylistRepositoryCustom;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID>, PlaylistRepositoryCustom {

}