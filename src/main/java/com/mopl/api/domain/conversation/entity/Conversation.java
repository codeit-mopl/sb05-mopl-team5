package com.mopl.api.domain.conversation.entity;


import com.mopl.api.global.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "conversations")
@Getter
public class Conversation extends BaseEntity {

}