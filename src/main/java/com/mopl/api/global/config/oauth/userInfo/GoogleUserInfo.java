package com.mopl.api.global.config.oauth.userInfo;

import com.mopl.api.domain.user.entity.SocialAccountProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GoogleUserInfo implements OAuth2UserInfo{

    private final Map<String, Object> attributes;

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        Object name = (String)attributes.get("name");
        return name == null ? "" : String.valueOf(name);
    }

    @Override
    public String getProfileImageUrl() {
        Object picture = attributes.get("picture");
        return picture == null ? null : String.valueOf(picture);
    }

    @Override
    public SocialAccountProvider getProvider() {
        return SocialAccountProvider.GOOGLE;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("sub")); // 구글 고유ID
    }
}
