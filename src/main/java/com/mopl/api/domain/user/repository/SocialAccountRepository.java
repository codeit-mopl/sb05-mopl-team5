package com.mopl.api.domain.user.repository;

import com.mopl.api.domain.user.entity.SocialAccount;
import com.mopl.api.domain.user.entity.SocialAccountProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {
    Optional<SocialAccount> findByProviderAndProviderId(SocialAccountProvider provider, String providerId);
    Boolean existsByProviderAndProviderId(SocialAccountProvider provider, String providerId);

}