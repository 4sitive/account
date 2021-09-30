insert into `client` (`id`, `authorization_grant_types`, `client_authentication_methods`, `client_id`,
                      `client_id_issued_at`, `client_name`, `client_secret`,
                      `client_secret_expires_at`, `client_settings`, `redirect_uris`, `scopes`,
                      `token_settings`)
values ('4sitive', 'refresh_token,client_credentials,authorization_code',
        'client_secret_post,client_secret_basic', '4sitive', '2021-07-23 15:47:51.368000', '4sitive',
        '{noop}secret', null,
        null,
        'http://localhost:8080/swagger-ui/oauth2-redirect.html,positive://login,http://lvh.me:8080/swagger-ui/oauth2-redirect.html,http://127.0.0.1/login/oauth2/code/TEST,https://api.4sitive.com/swagger-ui/oauth2-redirect.html,https://oauth.pstmn.io/v1/callback,http://lvh.me:8080/callback',
        '',
        '{`@class`:`java.util.HashMap`,`settings.token.access-token-time-to-live`:[`java.time.Duration`,86400.000000000],`settings.token.refresh-token-time-to-live`:[`java.time.Duration`,157680000.000000000]}');
commit;