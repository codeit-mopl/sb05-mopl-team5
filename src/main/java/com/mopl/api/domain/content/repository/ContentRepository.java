package com.mopl.api.domain.content.repository;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.ContentType;
import com.mopl.api.domain.content.repository.impl.ContentRepositoryCustom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom {

    Optional<Content> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByApiIdAndType(Long apiId, ContentType type);
}
