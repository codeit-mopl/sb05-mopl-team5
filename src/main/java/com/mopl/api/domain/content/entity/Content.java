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
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class Content extends BaseDeletableEntity {

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentType type;

    @Column(name = "api_id")
    private Long apiId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 255)
    private String thumbnailUrl;

    @Column(nullable = false, length = 500)
    private String tags;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(nullable = false)
    private Long reviewCount = 0L;

    @Column(nullable = false)
    private Long watcherCount = 0L;

    public void update(String title, String description, String tags, String thumbnailUrl) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
        if (tags != null && !tags.isBlank()) {
            this.tags = tags;
        }
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            this.thumbnailUrl = thumbnailUrl;
        }
    }

    public void isDelete() {
        this.isDeleted = true;
    }

    public void updateRatingStats(BigDecimal newAverageRating, Long newReviewCount) {
        this.averageRating = newAverageRating;
        this.reviewCount = newReviewCount;
    }
}