package com.mopl.api.domain.content.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ContentTypeConverter implements AttributeConverter<ContentType, String> {

    @Override
    public String convertToDatabaseColumn(ContentType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public ContentType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        
        try {
            return ContentType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            return ContentType.findByValue(dbData);
        }
    }
}
