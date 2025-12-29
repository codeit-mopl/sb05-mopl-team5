package com.mopl.api.domain.user.repository;

import com.mopl.api.domain.user.entity.JwtSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JwtSessionRepository extends JpaRepository<JwtSession, UUID> {

}