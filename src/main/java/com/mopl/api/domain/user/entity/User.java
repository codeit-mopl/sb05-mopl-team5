package com.mopl.api.domain.user.entity;

import com.mopl.api.global.common.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdatableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 500)
    private String profileImageUrl;

    @Enumerated(value = EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    private Boolean locked = false;

    @Column(nullable = false)
    private Long followerCount = 0L;
}