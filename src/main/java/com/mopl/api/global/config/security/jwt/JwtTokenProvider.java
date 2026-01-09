package com.mopl.api.global.config.security.jwt;

import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";

    private final int accessTokenExpirationMs;
    private final int refreshTokenExpirationMs;

    private final JWSSigner accessTokenSigner;
    private final JWSSigner refreshTokenSigner;
    private final JWSVerifier accessTokenVerifier;
    private final JWSVerifier refreshTokenVerifier;

    public JwtTokenProvider(
        @Value("${custom.jwt.access-token.secret}")String accessTokenSecret,
        @Value("${custom.jwt.access-token.expiration-ms}") int accessTokenExpirationMs,
        @Value("${custom.jwt.refresh-token.secret}") String refreshTokenSecret,
        @Value("${custom.jwt.refresh-token.expiration-ms}") int refreshTokenExpirationMs)
        throws JOSEException{

        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;

        byte[] accessSecretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSigner = new MACSigner(accessSecretBytes);
        this.accessTokenVerifier = new MACVerifier(accessSecretBytes);

        byte[] refreshSecretBytes = refreshTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.refreshTokenSigner = new MACSigner(refreshSecretBytes);
        this.refreshTokenVerifier = new MACVerifier(refreshSecretBytes);
    }

    // 토큰 생성 (access)
    public String generateAccessToken(CustomUserDetails userDetails) throws JOSEException {

        String tokenId = UUID.randomUUID().toString();
        UserDto userDto = userDetails.getUserDto();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(userDto.email())
            .jwtID(tokenId)
            .claim("userId", userDto.id().toString())
            .claim("type", "access")
            .claim("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()))
            .issueTime(now)
            .expirationTime(expiryDate)
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader(JWSAlgorithm.HS256),
            claims
        );

        signedJWT.sign(accessTokenSigner);
        String token = signedJWT.serialize();

        log.debug("Generated access token: {} , username : {}", token, userDto.name());
        return token;
    }

    // 토큰 생성 (refresh)
    public String generateRefreshToken(CustomUserDetails userDetails) throws JOSEException {

        String tokenId = UUID.randomUUID().toString();
        UserDto userDto = userDetails.getUserDto();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(userDto.email())
            .jwtID(tokenId)
            .claim("userId", userDto.id().toString())
            .claim("type", "refresh")
            .claim("roles", userDetails.getAuthorities().stream()
                                       .map(GrantedAuthority::getAuthority)
                                       .collect(Collectors.toList()))
            .issueTime(now)
            .expirationTime(expiryDate)
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader(JWSAlgorithm.HS256),
            claims
        );

        signedJWT.sign(refreshTokenSigner);
        String token = signedJWT.serialize();

        log.debug("Generated refresh token: {} , username : {}", token, userDto.name());
        return token;
    }

    // 토큰 검증 (access)
    public boolean validateAccessToken(String token){
        return validateToken(token, accessTokenVerifier, "access");
    }

    // 토큰 검증 (refresh)
    public boolean validateRefreshToken(String token){
        return validateToken(token, refreshTokenVerifier, "refresh");
    }

    private boolean validateToken(String token, JWSVerifier verifier, String tokenType) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // 서명 검증
            if(!signedJWT.verify(verifier)){
                log.debug("{} token verification failed", tokenType);
                return false;
            }

            // 토큰 타입 체크
            String checkType = (String) signedJWT.getJWTClaimsSet().getClaim("type");
            if(!checkType.equals(tokenType)){
                log.debug("{} token verification failed", tokenType);
                return false;
            }

            // 토큰 만료일 체크
            Date expiryDate = signedJWT.getJWTClaimsSet().getExpirationTime();
            if(expiryDate.before(new Date())){
                log.debug("{} token expired", tokenType);
                return false;
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    // 토큰에서 username 출력
    public String getUsernameFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch(Exception e) {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    // 토큰에서 tokenId 출력
    public String getTokenIdFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getJWTID();
        } catch (Exception e){
            throw new IllegalArgumentException("Invalid token");
        }
    }

    // 토큰에서 userId 출력
    public UUID getUserIdFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String userId = (String) signedJWT.getJWTClaimsSet().getClaim("userId");
            if(userId == null){
                throw new IllegalArgumentException("User Id claim not found in JWT");
            }
            return UUID.fromString(userId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    public Cookie generateRefreshTokenCookie(String refreshToken) {
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN, refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // 개발 환경에서 false로 유지
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(refreshTokenExpirationMs / 1000);
        return refreshCookie;
    }

    public Cookie generateRefreshTokenExpirationCookie() {
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN, "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // 개발 환경에서 false로 유지
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        return refreshCookie;
    }
}