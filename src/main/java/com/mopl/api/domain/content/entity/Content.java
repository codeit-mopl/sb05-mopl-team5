package com.mopl.api.domain.content.entity;

import com.mopl.api.global.common.entity.BaseDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Content extends BaseDeletableEntity {

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentType type;

    @Column(name = "api_id")
    private Long apiId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 255)
    private String thumbnailUrl;

    @Column(nullable = false)
    private String tags;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(nullable = false)
    private Long reviewCount = 0L;

    @Column(nullable = false)
    private Long watcherCount = 0L;
}