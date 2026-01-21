package com.mopl.api.domain.playlist.controller;

import com.mopl.api.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.api.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.api.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.mopl.api.domain.playlist.dto.response.PlaylistDto;
import com.mopl.api.domain.playlist.service.PlaylistService;
import com.mopl.api.domain.playlist.service.SubscriptionService;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "플레이리스트 관리", description = "플레이리스트 관리 API")
@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;
    private final SubscriptionService subscriptionService;

    @Operation(summary = "플레이리스트 생성", description = "새로운 플레이리스트를 생성합니다.")
    @PostMapping
    public ResponseEntity<PlaylistDto> playlistAdd(
        @Valid @RequestBody PlaylistCreateRequest request,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(playlistService.addPlaylist(request, user.getUserDto().id()));
    }

    @Operation(summary = "플레이리스트 수정", description = "플레이리스트 정보를 수정합니다.")
    @PatchMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> playlistModify(
        @PathVariable UUID playlistId,
        @Valid @RequestBody PlaylistUpdateRequest request,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(playlistService.modifyPlaylist(playlistId, request, user.getUserDto().id()));
    }

    @Operation(summary = "플레이리스트 삭제", description = "플레이리스트를 삭제합니다.")
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> playlistRemove(
        @PathVariable UUID playlistId,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        playlistService.removePlaylist(playlistId, user.getUserDto().id());
        return ResponseEntity.noContent()
                             .build();
    }

    @Operation(summary = "플레이리스트 단건 조회", description = "플레이리스트의 상세 정보를 조회합니다.")
    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> playlistDetails(
        @PathVariable UUID playlistId,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(playlistService.getPlaylist(playlistId, user.getUserDto().id()));
    }

    @Operation(summary = "플레이리스트 목록 조회 (커서 페이지네이션)", description = "플레이리스트 목록을 커서 기반 페이징으로 조회합니다.")
    @GetMapping
    public ResponseEntity<CursorResponsePlaylistDto> playlistList(
        @RequestParam(required = false) String keywordLike,
        @RequestParam(required = false) UUID ownerIdEqual,
        @RequestParam(required = false) UUID subscriberIdEqual,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam int limit,
        @RequestParam String sortBy,
        @RequestParam String sortDirection,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        UUID currentUserId = user != null ? user.getUserDto().id() : null;
        return ResponseEntity.ok(
            playlistService.getPlaylists(
                keywordLike, ownerIdEqual, subscriberIdEqual, cursor, idAfter,
                limit, sortBy, sortDirection, currentUserId
            )
        );
    }

    @Operation(summary = "플레이리스트에 콘텐츠 추가", description = "플레이리스트에 콘텐츠를 추가합니다.")
    @PostMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> playlistContentAdd(
        @PathVariable UUID playlistId,
        @PathVariable UUID contentId,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        playlistService.addContentToPlaylist(playlistId, contentId, user.getUserDto().id());
        return ResponseEntity.status(HttpStatus.CREATED)
                             .build();
    }

    @Operation(summary = "플레이리스트에서 콘텐츠 삭제", description = "플레이리스트에서 콘텐츠를 삭제합니다.")
    @DeleteMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> playlistContentRemove(
        @PathVariable UUID playlistId,
        @PathVariable UUID contentId,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        playlistService.removeContentFromPlaylist(playlistId, contentId, user.getUserDto().id());
        return ResponseEntity.noContent()
                             .build();
    }

    @Operation(summary = "플레이리스트 구독", description = "플레이리스트를 구독합니다.")
    @PostMapping("/{playlistId}/subscription")
    public ResponseEntity<Void> playlistSubscribe(
        @PathVariable UUID playlistId,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        subscriptionService.subscribeToPlaylist(playlistId, user.getUserDto().id());
        return ResponseEntity.status(HttpStatus.CREATED)
                             .build();
    }

    @Operation(summary = "플레이리스트 구독 취소", description = "플레이리스트 구독을 취소합니다.")
    @DeleteMapping("/{playlistId}/subscription")
    public ResponseEntity<Void> playlistUnsubscribe(
        @PathVariable UUID playlistId,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        subscriptionService.unsubscribeFromPlaylist(playlistId, user.getUserDto().id());
        return ResponseEntity.noContent()
                             .build();
    }
}
