package com.mopl.api.domain.content.repository;

import com.mopl.api.domain.content.entity.Content;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, UUID> {

}
