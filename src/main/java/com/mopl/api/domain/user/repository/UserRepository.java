package com.mopl.api.domain.user.repository;

import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.impl.UserRepositoryCustom;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {

}