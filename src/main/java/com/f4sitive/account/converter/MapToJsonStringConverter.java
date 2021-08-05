package com.f4sitive.account.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.SneakyThrows;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Converter
public class MapToJsonStringConverter implements AttributeConverter<Map<String, Object>, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT);
    private final ObjectWriter writer;
    private final ObjectReader reader;

    MapToJsonStringConverter() {
        this.writer = OBJECT_MAPPER.writer();
        this.reader = OBJECT_MAPPER.readerFor(new TypeReference<Map<String, Object>>() {
        });
    }

    @SneakyThrows
    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        return writer.writeValueAsString(Optional.ofNullable(attribute).orElse(Collections.emptyMap()));
    }

    @SneakyThrows
    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        return reader.readValue(Optional.ofNullable(dbData).orElse("{}"));
    }
}