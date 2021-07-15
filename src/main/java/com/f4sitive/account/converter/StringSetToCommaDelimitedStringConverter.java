package com.f4sitive.account.converter;

import org.springframework.util.StringUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Set;

@Converter
public class StringSetToCommaDelimitedStringConverter implements AttributeConverter<Set<String>, String> {
    /**
     * @see StringUtils#collectionToCommaDelimitedString
     */
    @Override
    public String convertToDatabaseColumn(Set<String> attribute) {
        return StringUtils.collectionToDelimitedString(attribute, ",");
    }

    @Override
    public Set<String> convertToEntityAttribute(String dbData) {
        return StringUtils.commaDelimitedListToSet(dbData);
    }
}