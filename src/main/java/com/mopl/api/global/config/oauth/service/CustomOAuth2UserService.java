package com.mopl.api.global.config.oauth.service;

import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.entity.SocialAccount;
import com.mopl.api.domain.user.entity.SocialAccountProvider;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.exception.user.detail.UserNotFoundException;
import com.mopl.api.domain.user.repository.SocialAccountRepository;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.global.config.oauth.claim.CustomOAuth2UserDetails;
import com.mopl.api.global.config.oauth.userInfo.GoogleUserInfo;
import com.mopl.api.global.config.oauth.userInfo.KakaoUserInfo;
import com.mopl.api.global.config.oauth.userInfo.OAuth2UserInfo;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) {

        OAuth2User oAuth2User = super.loadUser(request);

        String registrationId = request.getClientRegistration()
                                       .getRegistrationId();
        OAuth2UserInfo userInfo = resolveUserInfo(registrationId, oAuth2User.getAttributes());

        SocialAccountProvider provider = userInfo.getProvider();
        String providerId = userInfo.getProviderId();

        Optional<SocialAccount> socialOpt = socialAccountRepository.findByProviderAndProviderId(provider, providerId);

        User user;
        if (socialOpt.isPresent()) {
            // 이미 연결된 계정이면 기존 사용자로
            SocialAccount social = socialOpt.get();
            UUID userId = social.getUser()
                                .getId();
            user = userRepository.findById(userId)
                                 .orElseThrow(() -> UserNotFoundException.withUserId(userId));
        } else {
            // 첫 로그인이라면 -> 계정 만들기!
            String email = userInfo.getEmail();
            String name = userInfo.getName();
            String profileImageUrl = userInfo.getProfileImageUrl();

            String finalEmail = (email == null || email.isBlank()) ? generateEmail(provider, providerId, name) : email;

            // 이메일 있으면 가져오기 ->  없으면 만들기
            user = userRepository.findByEmail(finalEmail)
                                 .orElseGet(() -> {
                                     String encodePassword = passwordEncoder.encode(generateRandomPassword());
                                     User newUser = new User(finalEmail, encodePassword, name);
                                     newUser.updateProfileImageUrl(profileImageUrl);
                                     return userRepository.saveAndFlush(newUser);
                                 });

            SocialAccount social = new SocialAccount(user, provider, providerId);
            socialAccountRepository.save(social);
        }

        UserDto userDto = UserDto.builder()
                                 .id(user.getId())
                                 .createdAt(user.getCreatedAt())
                                 .email(user.getEmail())
                                 .name(user.getName())
                                 .profileImageUrl(user.getProfileImageUrl())
                                 .role(user.getRole())
                                 .locked(user.getLocked())
                                 .build();

        return new CustomOAuth2UserDetails(userDto, user.getPassword(), oAuth2User.getAttributes());
    }

    private OAuth2UserInfo resolveUserInfo(String registrationId, Map<String, Object> attributes) {

        if ("google".equalsIgnoreCase(registrationId)) {
            return new GoogleUserInfo(attributes);
        }
        if ("kakao".equalsIgnoreCase(registrationId)) {
            return new KakaoUserInfo(attributes);
        }

        log.error("[OAuth2] 지원하지 않는 소셜 로그인 : {}", registrationId);
        throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
    }

    private String generateEmail(SocialAccountProvider provider, String providerId, String name) {
        String safeName = (name == null ? "user" : name.replaceAll("\\s+", ""));
        if (safeName.isBlank()) {
            safeName = "user";
        }

        if (provider == SocialAccountProvider.KAKAO) {
            return safeName + "_" + providerId + "@kakao.com";
        }
        return provider.name()
                       .toLowerCase() + "_" + providerId + "@social.local";
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder()
                     .withoutPadding()
                     .encodeToString(bytes);
    }

}


