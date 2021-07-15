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
    private final ObjectMapper mapper;
    private final ObjectWriter writer;
    private final ObjectReader reader;

    MapToJsonStringConverter(ObjectMapper objectMapper) {
        this.mapper = objectMapper.copy().disable(SerializationFeature.INDENT_OUTPUT);
        this.writer = mapper.writer();
        this.reader = mapper.readerFor(new TypeReference<Map<String, Object>>() {
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