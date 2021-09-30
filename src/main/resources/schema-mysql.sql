create table `authorization_consent`
(
    `client_id`          varchar(100)  not null,
    `user_id`            varchar(36)   not null,
    `authorities`        longtext,
    `created_by`         varchar(36),
    `created_date`       timestamp,
    `last_modified_by`   varchar(36),
    `last_modified_date` timestamp,
    `version`            int default 0 not null,
    primary key (`user_id`, `client_id`)
);

create table `authorized_client`
(
    `registration_id`         varchar(100)  not null,
    `user_id`                 varchar(36)   not null,
    `access_token`            longtext,
    `access_token_expires_at` timestamp,
    `access_token_issued_at`  timestamp,
    `access_token_scopes`     longtext,
    `attributes`              longtext,
    `created_by`              varchar(36),
    `created_date`            timestamp,
    `last_modified_by`        varchar(36),
    `last_modified_date`      timestamp,
    `name`                    varchar(200),
    `refresh_token`           longtext,
    `refresh_token_issued_at` timestamp,
    `version`                 int default 0 not null,
    primary key (`user_id`, `registration_id`)
);

create table `client`
(
    `id`                            varchar(100)  not null,
    `authorization_grant_types`     longtext          not null,
    `client_authentication_methods` longtext          not null,
    `client_id`                     varchar(100)  not null,
    `client_id_issued_at`           timestamp     not null,
    `client_name`                   varchar(200)  not null,
    `client_secret`                 longtext,
    `client_secret_expires_at`      timestamp,
    `client_settings`               longtext,
    `created_by`                    varchar(36),
    `created_date`                  timestamp,
    `last_modified_by`              varchar(36),
    `last_modified_date`            timestamp,
    `redirect_uris`                 longtext,
    `scopes`                        longtext,
    `token_settings`                longtext,
    `version`                       int default 0 not null,
    primary key (`id`)
);

alter table `client`
    add constraint client_ux_client_id unique (`client_id`);

create table `device`
(
    `id`                            varchar(36)   not null,
    `access_token`                  longtext,
    `access_token_expires_at`       timestamp,
    `access_token_issued_at`        timestamp,
    `access_token_metadata`         longtext,
    `access_token_scopes`           longtext,
    `attributes`                    longtext,
    `authorization_code`            varchar(50),
    `authorization_code_expires_at` timestamp,
    `authorization_code_issued_at`  timestamp,
    `authorization_code_metadata`   longtext,
    `authorization_grant_type`      varchar(100)  not null,
    `client_id`                     varchar(100)  not null,
    `created_by`                    varchar(36),
    `created_date`                  timestamp,
    `external_serial_number`        varchar(100),
    `last_modified_by`              varchar(36),
    `last_modified_date`            timestamp,
    `refresh_token`                 varchar(50),
    `refresh_token_expires_at`      timestamp,
    `refresh_token_issued_at`       timestamp,
    `refresh_token_metadata`        longtext,
    `serial_number`                 varchar(100)  not null,
    `state`                         varchar(50),
    `user_id`                       varchar(36),
    `version`                       int default 0 not null,
    primary key (`id`)
);
alter table `device`
    add constraint device_ux_client_id_serial_number_user_id unique (`client_id`, `serial_number`, `user_id`);
alter table `device`
    add constraint device_ux_authorization_code unique (`authorization_code`);
alter table `device`
    add constraint device_ux_refresh_token unique (`refresh_token`);
alter table `device`
    add constraint device_ux_state unique (`state`);

create table `revision`
(
    `id`           bigint not null,
    `created_by`   varchar(36),
    `created_date` timestamp,
    primary key (`id`)
);

create table `revision_modified_entity`
(
    `revision_id` bigint not null,
    `name`        varchar(200)
);

create table `user`
(
    `id`                 varchar(36)   not null,
    `created_by`         varchar(36),
    `created_date`       timestamp,
    `last_modified_by`   varchar(36),
    `last_modified_date` timestamp,
    `name`               varchar(200),
    `registration_id`    varchar(100),
    `username`           varchar(100)  not null,
    `version`            int default 0 not null,
    `parent_id`          varchar(36),
    primary key (`id`)
);
alter table `user`
    add constraint user_ux_username_registration_id unique (`username`, `registration_id`);