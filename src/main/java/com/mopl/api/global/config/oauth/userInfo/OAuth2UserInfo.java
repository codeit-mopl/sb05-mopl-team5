package com.mopl.api.global.config.oauth.userInfo;

import com.mopl.api.domain.user.entity.SocialAccountProvider;

public interface OAuth2UserInfo {
    String getEmail();                          // provider email (없을 수 있음)
    String getName();                           // provider name/nickname
    String getProfileImageUrl();
    SocialAccountProvider getProvider();        // GOOGLE/KAKAO
    String getProviderId();                     // sub/id

}
