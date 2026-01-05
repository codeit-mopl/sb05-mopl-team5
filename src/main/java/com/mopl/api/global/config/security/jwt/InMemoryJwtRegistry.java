package com.mopl.api.global.config.security.jwt;

import com.mopl.api.domain.user.dto.request.JwtInformation;
import com.nimbusds.jose.JOSEException;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
public class InMemoryJwtRegistry implements JwtRegistry {

    private final Map<UUID, Queue<JwtInformation>> origin = new ConcurrentHashMap<>();

    // ✅ 토큰 문자열이 아니라 tokenId(jti)만 저장
    private final Set<String> accessTokenIdIndexes = ConcurrentHashMap.newKeySet();
    private final Set<String> refreshTokenIdIndexes = ConcurrentHashMap.newKeySet();

    private final int maxActiveJwtCount;
    private final JwtTokenProvider jwtTokenProvider;

    @CacheEvict(value = "users", key = "'all'")
    @Override
    public void registerJwtInformation(JwtInformation jwtInformation) {

        UUID userId = jwtInformation.getUserDto().id();

        origin.compute(userId, (key, queue) -> {

            if (queue == null) {
                queue = new ConcurrentLinkedQueue<>();
            }

            // maxActiveJwtCount 초과 시 가장 오래된 토큰 제거
            if (queue.size() >= maxActiveJwtCount) {
                JwtInformation deprecated = queue.poll();
                if (deprecated != null) {
                    removeTokenIdIndex(deprecated.getAccessToken(), deprecated.getRefreshToken());
                }
            }

            queue.add(jwtInformation);
            addTokenIdIndex(jwtInformation.getAccessToken(), jwtInformation.getRefreshToken());

            return queue;
        });
    }

    @CacheEvict(value = "users", key = "'all'")
    @Override
    public void invalidateJwtInformationByUserId(UUID userId) {

        origin.computeIfPresent(userId, (key, queue) -> {

            queue.forEach(info ->
                removeTokenIdIndex(info.getAccessToken(), info.getRefreshToken())
            );

            queue.clear();
            return null; // userId 엔트리 제거
        });
    }

    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        return origin.containsKey(userId);
    }

    /**
     * ✅ accessToken 문자열을 받지만, 내부에서는 tokenId(jti)만 뽑아서 조회
     */
    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        String tokenId = safeGetTokenId(accessToken);
        return tokenId != null && accessTokenIdIndexes.contains(tokenId);
    }

    /**
     * ✅ refreshToken 문자열을 받지만, 내부에서는 tokenId(jti)만 뽑아서 조회
     */
    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        String tokenId = safeGetTokenId(refreshToken);
        return tokenId != null && refreshTokenIdIndexes.contains(tokenId);
    }

    /**
     * refresh rotate: 기존 refreshToken을 기준으로 registry에 저장된 토큰쌍을 교체
     * ✅ 비교도 refreshToken 문자열이 아니라 refresh tokenId(jti)로 비교
     */
    @Override
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {

        UUID userId = newJwtInformation.getUserDto().id();

        String oldRefreshTokenId = safeGetTokenId(refreshToken);
        if (oldRefreshTokenId == null) {
            return; // 이상한 토큰이면 그냥 종료(또는 예외 처리)
        }

        origin.computeIfPresent(userId, (key, queue) -> {

            queue.stream()
                 .filter(info -> {
                     String storedRefreshTokenId = safeGetTokenId(info.getRefreshToken());
                     return oldRefreshTokenId.equals(storedRefreshTokenId);
                 })
                 .findFirst()
                 .ifPresent(oldInfo -> {

                     // 기존 tokenId 인덱스 제거
                     removeTokenIdIndex(oldInfo.getAccessToken(), oldInfo.getRefreshToken());

                     // JwtInformation 내부 토큰 교체(기존 방식 유지)
                     oldInfo.rotate(
                         newJwtInformation.getAccessToken(),
                         newJwtInformation.getRefreshToken()
                     );

                     // 새 tokenId 인덱스 추가
                     addTokenIdIndex(
                         newJwtInformation.getAccessToken(),
                         newJwtInformation.getRefreshToken()
                     );
                 });

            return queue;
        });
    }

    /**
     * 만료 토큰 정리
     * - 토큰 유효성(validate) 실패하면 Queue에서 제거 + tokenId 인덱스도 제거
     */
    @Scheduled(fixedDelay = 1000 * 60 * 5)
    @Override
    public void clearExpiredJwtInformation() {

        origin.entrySet().removeIf(entry -> {

            Queue<JwtInformation> queue = entry.getValue();

            queue.removeIf(info -> {
                boolean isExpired =
                    !jwtTokenProvider.validateAccessToken(info.getAccessToken()) ||
                        !jwtTokenProvider.validateRefreshToken(info.getRefreshToken());

                if (isExpired) {
                    removeTokenIdIndex(info.getAccessToken(), info.getRefreshToken());
                }

                return isExpired;
            });

            return queue.isEmpty();
        });
    }

    // -----------------------
    // tokenId 인덱스 관리
    // -----------------------

    private void addTokenIdIndex(String accessToken, String refreshToken) {
        String accessTokenId = safeGetTokenId(accessToken);
        if (accessTokenId != null) accessTokenIdIndexes.add(accessTokenId);

        String refreshTokenId = safeGetTokenId(refreshToken);
        if (refreshTokenId != null) refreshTokenIdIndexes.add(refreshTokenId);
    }

    private void removeTokenIdIndex(String accessToken, String refreshToken) {
        String accessTokenId = safeGetTokenId(accessToken);
        if (accessTokenId != null) accessTokenIdIndexes.remove(accessTokenId);

        String refreshTokenId = safeGetTokenId(refreshToken);
        if (refreshTokenId != null) refreshTokenIdIndexes.remove(refreshTokenId);
    }

    /**
     * 토큰이 이미 깨졌거나 파싱이 안 될 수도 있으니 안전하게 tokenId 추출
     */
    private String safeGetTokenId(String token) {
        try {
            return jwtTokenProvider.getTokenIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }
}
