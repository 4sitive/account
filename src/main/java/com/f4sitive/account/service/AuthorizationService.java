package com.f4sitive.account.service;

import com.f4sitive.account.repository.DeviceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.f4sitive.account.entity.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2TokenType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthorizationService extends JdbcOAuth2AuthorizationService {
    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final static TypeReference<Map<String, Object>> TYPE_REFERENCE = new TypeReference<Map<String, Object>>() {
    };
    private final ObjectMapper objectMapper;
    private final DeviceRepository deviceRepository;
    private final RegisteredClientRepository registeredClientRepository;
    private final JwtDecoder jwtDecoder;
    private final Cache cache;

    public AuthorizationService(JdbcOperations jdbcOperations,
                                RegisteredClientRepository registeredClientRepository,
                                ObjectMapper objectMapper,
                                DeviceRepository deviceRepository,
                                JwtDecoder jwtDecoder,
                                CacheManager cacheManager) {
        super(jdbcOperations, registeredClientRepository, new DefaultLobHandler());
        this.registeredClientRepository = registeredClientRepository;
        this.objectMapper = objectMapper;
        this.deviceRepository = deviceRepository;
        this.jwtDecoder = jwtDecoder;
        this.cache = cacheManager.getCache("TOKEN");
    }

    public JsonNode cache(String token) {
        return cache.get(token, JsonNode.class);
    }

    @Transactional(readOnly = true)
    public List<OAuth2Authorization> findAllByUserId(String userId) {
        return deviceRepository.queryAllByUserId(userId)
                .stream()
                .map(device -> oauth2Authorization(device))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void save(OAuth2Authorization authorization) {
        RegisteredClient registeredClient = registeredClientRepository.findById(authorization.getRegisteredClientId());
        boolean userIdAsSerialNumber = Optional.ofNullable(registeredClient.getClientSettings().<Boolean>getSetting("user-id-as-serial-number")).orElse(false);

        String userId = authorization.getPrincipalName();

        Optional<String> data = Optional.<OAuth2AuthorizationRequest>ofNullable(authorization.getAttribute(OAuth2AuthorizationRequest.class.getName())).map(oauth2AuthorizationRequest -> oauth2AuthorizationRequest.getAdditionalParameters().get("data")).filter(String.class::isInstance).map(String.class::cast);
        String serialNumber = serialNumber(data).orElse(userId);
        String externalSerialNumber = userIdAsSerialNumber ? serialNumber : null;
        if (userIdAsSerialNumber) {
            serialNumber = userId;
        }

        Device device = deviceRepository.queryByClientIdAndSerialNumberAndUserId(registeredClient.getId(), serialNumber, userId)
                .orElseGet(() -> new Device());
        device.setClientId(registeredClient.getId());
        device.setSerialNumber(serialNumber);
        device.setUserId(userId);
        device.setExternalSerialNumber(externalSerialNumber);

        device.setAuthorizationGrantType(authorization.getAuthorizationGrantType().getValue());
        device.setAttributes(authorization.getAttributes());
        device.setState(authorization.getAttribute(OAuth2ParameterNames.STATE));

        Optional.ofNullable(authorization.getToken(OAuth2AuthorizationCode.class))
                .ifPresent(authorizationCode -> {
                    device.setAuthorizationCode(authorizationCode.getToken().getTokenValue());
                    device.setAuthorizationCodeIssuedAt(authorizationCode.getToken().getIssuedAt());
                    device.setAuthorizationCodeExpiresAt(authorizationCode.getToken().getExpiresAt());
                    device.setAuthorizationCodeMetadata(authorizationCode.getMetadata());
                });

        Optional.ofNullable(authorization.getAccessToken())
                .ifPresent(accessToken -> {
                    device.setAccessToken(accessToken.getToken().getTokenValue());
                    device.setAccessTokenIssuedAt(accessToken.getToken().getIssuedAt());
                    device.setAccessTokenExpiresAt(accessToken.getToken().getExpiresAt());
                    device.setAccessTokenMetadata(accessToken.getMetadata());
                });

        Optional.ofNullable(authorization.getRefreshToken())
                .ifPresent(refreshToken -> {
                    Instant issuedAt = Instant.now();
                    Instant expiresAt = issuedAt.plus(registeredClient.getTokenSettings().getRefreshTokenTimeToLive());
                    device.setRefreshToken(refreshToken.getToken().getTokenValue());
                    device.setRefreshTokenIssuedAt(issuedAt);
                    device.setRefreshTokenExpiresAt(expiresAt);
                    device.setRefreshTokenMetadata(refreshToken.getMetadata());
                });
        deviceRepository.save(device);
        Optional.ofNullable(authorization.getAccessToken())
                .ifPresent(accessToken -> {
                    if (accessToken.isActive()) {
                        cache.put(device.token(), token(device, registeredClient));
                    } else {
                        cache.evict(device.token());
                    }
                });
    }

    @Override
    @Transactional
    public void remove(OAuth2Authorization authorization) {
        Optional.ofNullable(authorization.getAccessToken())
                .map(OAuth2Authorization.Token::getClaims)
                .map(claims -> claims.get("ext"))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(ext -> ext.get("dvc"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .flatMap(deviceRepository::findById)
                .ifPresent(deviceRepository::delete);
    }

    String deviceId(String token) {
        try {
            return jwtDecoder.decode(token).<Map<String, String>>getClaim("ext").get("dvc");
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    Optional<String> serialNumber(Optional<String> data) {
        return data.map(content -> {
                    try {
                        return objectMapper.readTree(content);
                    } catch (JsonProcessingException e) {
                        return NullNode.instance;
                    }
                })
                .map(jsonNode -> jsonNode.get("serialNumber"))
                .map(deviceSerialNumber -> deviceSerialNumber.asText());
    }

    ObjectNode token(Device device, RegisteredClient registeredClient) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("deviceUniqueId", device.serialNumber());
        objectNode.put("userId", device.getUserId());
        objectNode.put("deviceId", device.getId());
        objectNode.put("serviceId", "SVC0000001");
        objectNode.put("nuguDeviceTypeCd", "LUNA");
        objectNode.put("deviceTypeId", "DT000000000000000000");
        objectNode.put("deviceTypeCd", "LUNA");
        objectNode.put("issueDate", DATE_TIME_FORMATTER.format(LocalDateTime.now()));
        objectNode.put("pocId", registeredClient.getClientId());
        objectNode.put("pocStatus", "REGISTERED");
        objectNode.put("pocEnabled", true);
        objectNode.put("pocModelTypeCode", "LUNA");
        objectNode.put("deviceModelName", "LUNA");
        objectNode.put("token", device.token());
        objectNode.put("uniqueId", device.getUserId() + device.getId());
        objectNode.put("anonymous", false);
        objectNode.with("additional").put("userId", device.getUserId());
        objectNode.with("additional").put("deviceId", device.getId());
        objectNode.with("attributes");
        device.getAttributes().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof String)
                .forEach(entry -> objectNode.with("attributes").put(entry.getKey(), (String) entry.getValue()));
        return objectNode;
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2Authorization findById(String id) {
        return deviceRepository.findById(id)
                .map(device -> oauth2Authorization(device))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        if (tokenType == null) {
            return Optional.ofNullable(deviceId(token))
                    .flatMap(accessToken -> deviceRepository.findById(accessToken))
                    .map(device -> oauth2Authorization(device))
                    .orElse(null);
        } else if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
            return deviceRepository.queryByState(token)
                    .map(device -> oauth2Authorization(device))
                    .orElse(null);
        } else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
            return deviceRepository.queryByAuthorizationCode(token)
                    .map(this::oauth2Authorization)
                    .orElse(null);
        } else if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
            return Optional.ofNullable(deviceId(token))
                    .flatMap(accessToken -> deviceRepository.findById(accessToken))
                    .map(device -> oauth2Authorization(device))
                    .orElse(null);
        } else if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            return deviceRepository.queryByRefreshToken(token)
                    .map(device -> oauth2Authorization(device))
                    .orElse(null);
        }
        return null;
    }

    OAuth2Authorization oauth2Authorization(Device device) {
        RegisteredClient registeredClient = registeredClientRepository.findById(device.getClientId());
        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(device.getId())
                .principalName(device.getUserId())
                .authorizationGrantType(new AuthorizationGrantType(device.getAuthorizationGrantType()))
                .attributes(attrs -> {
                    attrs.putAll(device.getAttributes());
                    Map<String, Object> ext = new LinkedHashMap<>();
                    ext.put("usr", device.getUserId());
                    ext.put("dvc", device.getId());
                    ext.put("tkn", device.token());
                    ext.put("poc", registeredClient.getClientId());
                    ext.put("srl", device.serialNumber());
                    ext.put("svc", registeredClient.getClientSettings().getSetting("svc"));
                    attrs.put("ext", ext);
                });
        Optional.ofNullable(device.getState()).filter(StringUtils::hasText).ifPresent(state -> builder.attribute(OAuth2ParameterNames.STATE, state));
        Optional.ofNullable(device.getAuthorizationCode())
                .map(authorizationCode -> new OAuth2AuthorizationCode(authorizationCode, device.getAuthorizationCodeIssuedAt(), device.getAuthorizationCodeExpiresAt()))
                .ifPresent(oauth2Token -> builder.token(oauth2Token, (metadata) -> metadata.putAll(device.getAuthorizationCodeMetadata())));
        Optional.ofNullable(device.getAccessToken())
                .map(accessToken -> new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, accessToken, device.getAccessTokenIssuedAt(), device.getAccessTokenExpiresAt(), device.getAccessTokenScopes()))
                .ifPresent(oauth2Token -> builder.token(oauth2Token, (metadata) -> metadata.putAll(device.getAccessTokenMetadata())));
        Optional.ofNullable(device.getRefreshToken())
                .map(refreshToken -> new OAuth2RefreshToken(refreshToken, device.getRefreshTokenIssuedAt(), device.getRefreshTokenExpiresAt()))
                .ifPresent(oauth2Token -> builder.token(oauth2Token, (metadata) -> metadata.putAll(device.getRefreshTokenMetadata())));
        return builder.build();
    }
}
