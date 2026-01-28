package com.mopl.api.domain.content.repository;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.ContentType;
import com.mopl.api.domain.content.repository.impl.ContentRepositoryCustom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom {

    Optional<Content> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByApiIdAndType(Long apiId, ContentType type);

    @Modifying
    @Query("UPDATE Content c SET c.ratingSum = c.ratingSum + :ratingValue, c.reviewCount = c.reviewCount + 1 WHERE c.id = :contentId")
    void incrementRating(@Param("contentId") UUID contentId, @Param("ratingValue") long ratingValue);

    @Modifying
    @Query("UPDATE Content c SET c.ratingSum = c.ratingSum - :oldRatingValue + :newRatingValue WHERE c.id = :contentId")
    void updateRating(@Param("contentId") UUID contentId, @Param("oldRatingValue") long oldRatingValue, @Param("newRatingValue") long newRatingValue);

    @Modifying
    @Query("UPDATE Content c SET c.ratingSum = c.ratingSum - :ratingValue, c.reviewCount = c.reviewCount - 1 WHERE c.id = :contentId")
    void decrementRating(@Param("contentId") UUID contentId, @Param("ratingValue") long ratingValue);
}
