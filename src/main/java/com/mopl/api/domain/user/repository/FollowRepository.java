package com.mopl.api.domain.user.repository;

import com.mopl.api.domain.user.entity.Follow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

}