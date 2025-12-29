package com.mopl.api.domain.user.repository;

import com.mopl.api.domain.user.entity.SocialAccount;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

}