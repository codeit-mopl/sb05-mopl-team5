package com.mopl.api.domain.user.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.mopl.api.domain.user.dto.request.JwtInformation;
import com.mopl.api.domain.user.dto.request.ResetPasswordRequest;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.exception.auth.detail.InvalidTokenException;
import com.mopl.api.domain.user.exception.user.detail.UserNotFoundException;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import com.mopl.api.global.config.security.jwt.JwtRegistry;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import com.nimbusds.jose.JOSEException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtTokenProvider jwtProvider;
    @Mock
    private JwtRegistry jwtRegistry;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MailService mailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations; // redisTemplate.opsForValue() 반환용

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "expirationSeconds", 180L); // 180초 세팅
    }


    @Test
    @DisplayName("refreshToken 실패 - UserDetails가 null이면 UsernameNotFoundException")
    void refreshToken_userDetailsNull_throws() {
        // given
        String refreshToken = "refresh.token";
        String username = "test@mopl.com";

        when(jwtProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)).thenReturn(true);

        when(jwtProvider.getUsernameFromToken(refreshToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(null);

        // when + then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
            .isInstanceOf(UsernameNotFoundException.class);

        verify(jwtProvider).validateRefreshToken(refreshToken);
        verify(jwtRegistry).hasActiveJwtInformationByRefreshToken(refreshToken);
        verify(jwtProvider).getUsernameFromToken(refreshToken);
        verify(userDetailsService).loadUserByUsername(username);

        verify(jwtRegistry, never()).rotateJwtInformation(anyString(), any());
    }

    @Test
    @DisplayName("refreshToken 성공 - 새 토큰 발급 후 rotateJwtInformation 호출, JwtInformation 반환")
    void refreshToken_success() throws Exception {
        // given
        String oldRefreshToken = "old.refresh.token";
        String username = "test@mopl.com";

        when(jwtProvider.validateRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtRegistry.hasActiveJwtInformationByRefreshToken(oldRefreshToken)).thenReturn(true);

        when(jwtProvider.getUsernameFromToken(oldRefreshToken)).thenReturn(username);

        CustomUserDetails customDetails = mock(CustomUserDetails.class);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(customDetails);

        String newAccess = "NEW_ACCESS_TOKEN_ABCDEFGHIJ";
        String newRefresh = "NEW_REFRESH_TOKEN_ABCDEFGHIJ";

        when(jwtProvider.generateAccessToken(customDetails)).thenReturn(newAccess);
        when(jwtProvider.generateRefreshToken(customDetails)).thenReturn(newRefresh);

        // customDetails.getUserDto() 반환값
        UserDto userDto = mock(UserDto.class);
        when(customDetails.getUserDto()).thenReturn(userDto);

        // when
        JwtInformation result = authService.refreshToken(oldRefreshToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(newAccess);
        assertThat(result.getRefreshToken()).isEqualTo(newRefresh);
        assertThat(result.getUserDto()).isSameAs(userDto);

        ArgumentCaptor<JwtInformation> captor = ArgumentCaptor.forClass(JwtInformation.class);
        verify(jwtRegistry).rotateJwtInformation(eq(oldRefreshToken), captor.capture());

        JwtInformation rotated = captor.getValue();
        assertThat(rotated.getAccessToken()).isEqualTo(newAccess);
        assertThat(rotated.getRefreshToken()).isEqualTo(newRefresh);
        assertThat(rotated.getUserDto()).isSameAs(userDto);
    }

    @Test
    @DisplayName("refreshToken 실패 - JOSEException 발생 시 RuntimeException(INTERNAL_SERVER_ERROR)")
    void refreshToken_joseException_wrapsRuntimeException() throws Exception {
        // given
        String oldRefreshToken = "old.refresh.token";
        String username = "test@mopl.com";

        when(jwtProvider.validateRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtRegistry.hasActiveJwtInformationByRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtProvider.getUsernameFromToken(oldRefreshToken)).thenReturn(username);

        CustomUserDetails customDetails = mock(CustomUserDetails.class);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(customDetails);

        when(jwtProvider.generateAccessToken(customDetails)).thenThrow(new JOSEException("boom"));

        // when + then
        assertThatThrownBy(() -> authService.refreshToken(oldRefreshToken))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("INTERNAL_SERVER_ERROR");

        verify(jwtRegistry, never()).rotateJwtInformation(anyString(), any());
    }

    @Test
    @DisplayName("resetPassword 성공 - 임시비번 생성 후 Redis 저장(TTL) + 메일 전송")
    void resetPassword_success() {
        // given
        String email = "test@mopl.com";
        ResetPasswordRequest request = new ResetPasswordRequest(email);

        when(userRepository.existsByEmail(email)).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("HASHED_TEMP_PW");

        doReturn(valueOperations).when(redisTemplate).opsForValue();

        // when
        authService.resetPassword(request);

        // then
        ArgumentCaptor<String> tempPwCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(tempPwCaptor.capture());
        String tempPassword = tempPwCaptor.getValue();

        assertThat(tempPassword).startsWith("mopl1!");
        assertThat(tempPassword.length()).isEqualTo("mopl1!".length() + 6);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> hashedCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(keyCaptor.capture(), hashedCaptor.capture(), ttlCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("temp-pw:" + email);
        assertThat(hashedCaptor.getValue()).isEqualTo("HASHED_TEMP_PW");
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(180L));

        verify(mailService).sendMail(email, tempPassword);
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("resetPassword 실패 - 존재하지 않는 이메일이면 UserNotFoundException, Redis/메일 전송 안 함")
    void resetPassword_userNotFound_throws() {
        // given
        String email = "test@mopl.com";
        ResetPasswordRequest request = new ResetPasswordRequest(email);

        when(userRepository.existsByEmail(email)).thenReturn(false);

        // when + then
        assertThatThrownBy(() -> authService.resetPassword(request))
            .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).existsByEmail(email);

        // encode/redis/mail은 호출되면 안 됨
        verifyNoInteractions(passwordEncoder, mailService);
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }
}
