package com.mopl.api.domain.user.entity;

import com.mopl.api.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "social_accounts")
@Getter
public class SocialAccount extends BaseEntity {

    @Enumerated(value = EnumType.STRING)
    private SocialAccountProvider provider;

    @Column(nullable = false, length = 500)
    private String providerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}