package com.mopl.api.global.common.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public class BaseDeletableEntity extends BaseUpdatableEntity {

    private Boolean isDeleted;


    public void softDelete() {
        this.isDeleted = true;
    }

    public void restore() {
        this.isDeleted = false;
    }
}