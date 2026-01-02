package com.mopl.api.global.config.security.jwt;

import com.mopl.api.domain.user.dto.request.JwtInformation;
import com.nimbusds.jose.JOSEException;
import java.util.UUID;

public interface JwtRegistry {
    void registerJwtInformation(JwtInformation jwtInformation);
    void invalidateJwtInformationByUserId(UUID userId);
    boolean hasActiveJwtInformationByUserId(UUID userId);
    boolean hasActiveJwtInformationByAccessToken(String accessToken);
    boolean hasActiveJwtInformationByRefreshToken(String refreshToken);
    void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation);
    void clearExpiredJwtInformation() throws JOSEException;

}
