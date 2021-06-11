package com.f4sitive.account.entity;

import com.f4sitive.account.converter.StringSetToWhiteSpaceDelimitedStringConverter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public class OAuth2User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "generator")
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "PROVIDER")
    private String provider;

    @Column(name = "PROVIDER_ID")
    private String providerId;

    @Lob
    @Column(name = "ACCESS_TOKEN")
    private String accessToken;

    @Lob
    @Column(name = "REFRESH_TOKEN")
    private String refreshToken;

    @Column(name = "TOKEN_TYPE")
    private String tokenType;

    @Column(name = "ISSUED_AT")
    private Instant issuedAt;

    @Column(name = "EXPIRES_AT")
    private Instant expiresAt;

    @Convert(converter = StringSetToWhiteSpaceDelimitedStringConverter.class)
    @Column(name = "SCOPES")
    private Set<String> scopes = new LinkedHashSet<>();

    @Version
    @Column(name = "VERSION")
    private long version;
}
