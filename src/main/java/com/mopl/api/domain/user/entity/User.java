package com.mopl.api.domain.user.entity;

import com.mopl.api.global.common.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor

public class User extends BaseUpdatableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 500)
    private String profileImageUrl;

    @Enumerated(value = EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    private Boolean locked = false;

    @Column(nullable = false)
    private Long followerCount = 0L;


    public void changeName(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        this.name = name;
    }

    public void changeProfileImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        this.profileImageUrl = url;
    }

    public User(String email, String name, UserRole role) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.locked = false;
    }

    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }

    public void updateUserRole(UserRole role) {
        this.role = role;
    }
}

