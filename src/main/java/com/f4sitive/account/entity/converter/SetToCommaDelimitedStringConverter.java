package com.f4sitive.account.entity.converter;

import org.springframework.util.StringUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Set;

@Converter
public class SetToCommaDelimitedStringConverter implements AttributeConverter<Set<String>, String> {
    @Override
    public String convertToDatabaseColumn(Set<String> attribute) {
        return StringUtils.collectionToCommaDelimitedString(attribute);
    }

    @Override
    public Set<String> convertToEntityAttribute(String dbData) {
        return StringUtils.commaDelimitedListToSet(dbData);
    }
}