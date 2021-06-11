package com.f4sitive.account.converter;

import org.springframework.util.StringUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Converter
public class StringSetToWhiteSpaceDelimitedStringConverter implements AttributeConverter<Set<String>, String> {
    @Override
    public String convertToDatabaseColumn(Set<String> attribute) {
        return StringUtils.collectionToDelimitedString(attribute, " ");
    }

    @Override
    public Set<String> convertToEntityAttribute(String dbData) {
        return new LinkedHashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(dbData, " ")));
    }
}