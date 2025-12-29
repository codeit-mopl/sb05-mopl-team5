package com.mopl.api.global.common.entity;

import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@MappedSuperclass
public abstract class BaseUpdatableEntity extends BaseEntity {

    @UpdateTimestamp
    protected Instant updatedAt;
}