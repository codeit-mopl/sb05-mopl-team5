package com.mopl.api.domain.playlist.controller;

import com.mopl.api.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.api.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.api.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.mopl.api.domain.playlist.dto.response.PlaylistDto;
import com.mopl.api.domain.playlist.service.PlaylistService;
import com.mopl.api.domain.playlist.service.SubscriptionService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;
    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<PlaylistDto> playlistAdd(
        @Valid @RequestBody PlaylistCreateRequest request,
        @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(playlistService.addPlaylist(request, userId));
    }

    @PatchMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> playlistModify(
        @PathVariable UUID playlistId,
        @Valid @RequestBody PlaylistUpdateRequest request,
        @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(playlistService.modifyPlaylist(playlistId, request, userId));
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> playlistRemove(
        @PathVariable UUID playlistId,
        @RequestHeader("X-User-Id") UUID userId
    ) {
        playlistService.removePlaylist(playlistId, userId);
        return ResponseEntity.noContent()
                             .build();
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> playlistDetails(
        @PathVariable UUID playlistId,
        @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(playlistService.getPlaylist(playlistId, userId));
    }

    @GetMapping
    public ResponseEntity<CursorResponsePlaylistDto> playlistList(
        @RequestParam(required = false) String keywordLike,
        @RequestParam(required = false) UUID ownerIdEqual,
        @RequestParam(required = false) UUID subscriberIdEqual,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "updatedAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDirection,
        @RequestHeader(value = "X-User-Id", required = false) UUID currentUserId
    ) {
        return ResponseEntity.ok(
            playlistService.getPlaylists(
                keywordLike, ownerIdEqual, subscriberIdEqual, cursor, idAfter,
                limit, sortBy, sortDirection, currentUserId
            )
        );
    }

    @PostMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> playlistContentAdd(
        @PathVariable UUID playlistId,
        @PathVariable UUID contentId,
        @RequestHeader("X-User-Id") UUID userId
    ) {
        playlistService.addContentToPlaylist(playlistId, contentId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .build();
    }

    @DeleteMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> playlistContentRemove(
        @PathVariable UUID playlistId,
        @PathVariable UUID contentId,
        @RequestHeader("X-User-Id") UUID userId
    ) {
        playlistService.removeContentFromPlaylist(playlistId, contentId, userId);
        return ResponseEntity.noContent()
                             .build();
    }

    @PostMapping("/{playlistId}/subscription")
    public ResponseEntity<Void> playlistSubscribe(
        @PathVariable UUID playlistId,
        @RequestHeader("X-User-Id") UUID userId
    ) {
        subscriptionService.subscribeToPlaylist(playlistId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .build();
    }

    @DeleteMapping("/{playlistId}/subscription")
    public ResponseEntity<Void> playlistUnsubscribe(
        @PathVariable UUID playlistId,
        @RequestHeader("X-User-Id") UUID userId
    ) {
        subscriptionService.unsubscribeFromPlaylist(playlistId, userId);
        return ResponseEntity.noContent()
                             .build();
    }
}
