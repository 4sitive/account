insert into oauth2_registered_client (id, authorization_grant_types, client_authentication_methods, client_id,
                                      client_id_issued_at, client_name, client_secret,
                                      client_secret_expires_at, client_settings, created_by, created_date,
                                      last_modified_by, last_modified_date, redirect_uris, scopes,
                                      token_settings, version)
values ('4sitive', 'refresh_token,client_credentials,authorization_code',
        'client_secret_post,post,basic,client_secret_basic', '4sitive', '2021-07-23 15:47:51.368000', '4sitive',
        '{noop}secret', null,
        '{"@class":"java.util.HashMap","setting.client.require-user-consent":false,"setting.client.require-proof-key":false}',
        null, null, null, null,
        'http://localhost:8080/swagger-ui/oauth2-redirect.html,positive://login,http://lvh.me:8080/swagger-ui/oauth2-redirect.html,http://127.0.0.1/login/oauth2/code/TEST,https://api.4sitive.com/swagger-ui/oauth2-redirect.html,https://oauth.pstmn.io/v1/callback',
        '',
        '{"@class":"java.util.HashMap","setting.token.access-token-time-to-live":["java.time.Duration",86400.000000000],"setting.token.refresh-token-time-to-live":["java.time.Duration",3600.000000000],"setting.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"setting.token.reuse-refresh-tokens":true}',
        1);
