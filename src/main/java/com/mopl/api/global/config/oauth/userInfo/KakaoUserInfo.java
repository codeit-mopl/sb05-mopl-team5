package com.mopl.api.global.config.oauth.userInfo;

import com.mopl.api.domain.user.entity.SocialAccountProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KakaoUserInfo implements OAuth2UserInfo{

    private final Map<String, Object> attributes;

    @Override
    public String getEmail() {
        Object kakaoAccountObject = attributes.get("kakao_account");
        if(kakaoAccountObject instanceof Map<?,?> kakaoAccount){
            return (String)kakaoAccount.get("email");
        }
        return null;
    }

    @Override
    public String getName() {
        Object propertiesObject = attributes.get("properties");
        if(propertiesObject instanceof Map<?,?> properties){
            Object nickname = properties.get("nickname");
            if(nickname != null) return String.valueOf(nickname);
        }
        return "";
    }

    @Override
    public String getProfileImageUrl() {
        Object propertiesObj = attributes.get("properties");
        if (propertiesObj instanceof Map<?, ?> props) {
            Object img = props.get("profile_image");
            return img == null ? null : String.valueOf(img);
        }
        return null;
    }

    @Override
    public SocialAccountProvider getProvider() {
        return SocialAccountProvider.KAKAO;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id")); // 카카오 고유ID
    }
}
