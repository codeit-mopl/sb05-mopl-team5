package com.mopl.api.domain.review.repository;

import com.mopl.api.domain.review.entity.Review;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID>, ReviewRepositoryCustom {

    boolean existsByContentIdAndUserIdAndIsDeletedFalse(UUID contentId, UUID userId);
}
