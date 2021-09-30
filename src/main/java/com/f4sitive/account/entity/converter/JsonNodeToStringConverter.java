package com.f4sitive.account.entity.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.f4sitive.account.entity.Constants;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Slf4j
@Converter
public class JsonNodeToStringConverter implements AttributeConverter<JsonNode, String> {
    @SneakyThrows
    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        return attribute == null ? null : Constants.OBJECT_MAPPER.writeValueAsString(attribute);
    }

    @SneakyThrows
    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Constants.OBJECT_MAPPER.readTree(dbData);
    }
}