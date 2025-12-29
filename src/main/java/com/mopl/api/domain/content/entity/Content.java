package com.mopl.api.domain.content.entity;

import com.mopl.api.global.common.entity.BaseDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "contents",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_CONTENTS_TYPE_API_ID", columnNames = {"type", "api_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseDeletableEntity {

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentType type;

    @Column(name = "api_id")
    private Long apiId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String thumbnailUrl;

    @Column(nullable = false, columnDefinition = "JSON")
    private String tags;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(nullable = false)
    private Long reviewCount = 0L;

    @Column(nullable = false)
    private Long watcherCount = 0L;
}
