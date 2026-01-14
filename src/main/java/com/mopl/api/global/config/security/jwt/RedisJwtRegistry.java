package com.mopl.api.global.config.security.jwt;

import com.mopl.api.domain.user.dto.request.JwtInformation;
import com.nimbusds.jose.JOSEException;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisJwtRegistry implements JwtRegistry {

    private static final String USER_KEY_PREFIX = "jwt:user:";
    private static final String ACCESS_TOKEN_KEY_PREFIX = "jwt:access_token:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "jwt:refresh_token:";

    @Qualifier("redisObjectTemplate")
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${custom.jwt.access-token.expiration-ms}")
    private long accessExpMs; // 1시간

    @Value("${custom.jwt.refresh-token.expiration-ms}")
    private long refreshExpMs; // 14일


    @Override
    public void registerJwtInformation(JwtInformation jwtInformation) {
        UUID userId = jwtInformation.getUserDto()
                                    .id();
        String userKey = getUserKey(userId);

        Duration refreshTtl = Duration.ofMillis(refreshExpMs);
        Duration accessTtl = Duration.ofMillis(accessExpMs);

        // 기존 세션 있으면 -> 인덱스 정리!
        Object old = redisTemplate.opsForValue()
                                  .get(userKey);
        if (old instanceof JwtInformation oldJwtInfo) {
            deleteIndexes(oldJwtInfo.getAccessToken(), oldJwtInfo.getRefreshToken());
        }

        redisTemplate.opsForValue()
                     .set(userKey, jwtInformation, refreshTtl);
        redisTemplate.opsForValue()
                     .set(getRefreshTokenKey(jwtInformation.getRefreshToken()), userId.toString(), refreshTtl);
        redisTemplate.opsForValue()
                     .set(getAccessTokenKey(jwtInformation.getAccessToken()), userId.toString(), accessTtl);
    }


    @Override
    public void invalidateJwtInformationByUserId(UUID userId) {
        String userKey = getUserKey(userId);

        Object old = redisTemplate.opsForValue()
                                  .get(userKey);
        if (old instanceof JwtInformation oldJwtInfo) {
            deleteIndexes(oldJwtInfo.getAccessToken(), oldJwtInfo.getRefreshToken());
        }
        redisTemplate.delete(userKey);
    }


    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getUserKey(userId)));
    }


    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getAccessTokenKey(accessToken)));
    }


    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getRefreshTokenKey(refreshToken)));
    }


    @Override
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        String oldRefreshKey = getRefreshTokenKey(refreshToken);
        Object userIdObj = redisTemplate.opsForValue()
                                        .get(oldRefreshKey);

        if (!(userIdObj instanceof String userIdStr)) {
            throw new IllegalArgumentException("Refresh Token not found in registry");
        }

        UUID userId = UUID.fromString(userIdStr);
        String userKey = getUserKey(userId);

        // 기존 세션 있으면 -> 인덱스 정리!
        Object old = redisTemplate.opsForValue()
                                           .get(userKey);
        if (old instanceof JwtInformation oldJwtInfo) {
          deleteIndexes(oldJwtInfo.getAccessToken(), oldJwtInfo.getRefreshToken());
        }

        // 새 토큰 등록
        Duration refreshTtl = Duration.ofMillis(refreshExpMs);
        Duration accessTtl = Duration.ofMillis(accessExpMs);

        redisTemplate.opsForValue()
                     .set(userKey, newJwtInformation, refreshTtl);
        redisTemplate.opsForValue()
                     .set(
                         getRefreshTokenKey(newJwtInformation.getRefreshToken()),
                         userId.toString(),
                         refreshTtl
                     );
        redisTemplate.opsForValue()
                     .set(
                         getAccessTokenKey(newJwtInformation.getAccessToken()),
                         userId.toString(),
                         accessTtl
                     );

        redisTemplate.delete(oldRefreshKey);
    }

    @Override
    public void clearExpiredJwtInformation() throws JOSEException {

    }

    private String getUserKey(UUID userId) {
        return USER_KEY_PREFIX + userId.toString();
    }

    private String getRefreshTokenKey(String refreshToken) {
        return REFRESH_TOKEN_KEY_PREFIX + refreshToken;
    }

    private String getAccessTokenKey(String accessToken) {
        return ACCESS_TOKEN_KEY_PREFIX + accessToken;
    }

    private void deleteIndexes(String accessToken, String refreshToken) {
        redisTemplate.delete(getAccessTokenKey(accessToken));
        redisTemplate.delete(getRefreshTokenKey(refreshToken));
    }
}
