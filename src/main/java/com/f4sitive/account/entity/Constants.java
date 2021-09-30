package com.f4sitive.account.entity;

import com.f4sitive.account.util.Snowflakes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;

import java.util.UUID;

public class Constants {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModules(SecurityJackson2Modules.getModules(Constants.class.getClassLoader()))
            .registerModule(new OAuth2AuthorizationServerJackson2Module());

    public static final int ID_LENGTH = 36;
    public static final String SNOWFLAKE_IDENTIFIER_GENERATOR = "snowflake_identifier_generator";
    public static final String SNOWFLAKE_IDENTIFIER_GENERATOR_STRATEGY = "com.f4sitive.account.entity.generator.SnowflakeIdentifierGenerator";

    public static String id(Snowflakes snowflakes) {
        return Snowflakes.uuid(snowflakes.generate()).toString();
    }

    public static UUID uuid(String id) {
        return UUID.fromString(id);
    }
}
