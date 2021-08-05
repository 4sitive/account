create table `revision_generator`
(
    `next_val` bigint
) engine=InnoDB;

insert into `revision_generator`
values (1);

create table `revision`
(
    `id`           bigint not null,
    `created_by`   varchar(36),
    `created_date` datetime(6),
    primary key (`id`)
) engine=InnoDB;

create table `revision_modified_entity`
(
    `revision_id` bigint not null,
    `name`        varchar(200)
) engine=InnoDB;

create table `oauth2_authorization`
(
    `id`                            varchar(100)  not null,
    `access_token_expires_at`       datetime(6),
    `access_token_issued_at`        datetime(6),
    `access_token_metadata`         longtext,
    `access_token_scopes`           longtext,
    `access_token_type`             varchar(100),
    `access_token_value`            longblob,
    `attributes`                    longtext,
    `authorization_code_expires_at` datetime(6),
    `authorization_code_issued_at`  datetime(6),
    `authorization_code_metadata`   longtext,
    `authorization_code_value`      longblob,
    `authorization_grant_type`      varchar(100)  not null,
    `created_by`                    varchar(36),
    `created_date`                  datetime(6),
    `last_modified_by`              varchar(36),
    `last_modified_date`            datetime(6),
    `oidc_id_token_expires_at`      datetime(6),
    `oidc_id_token_issued_at`       datetime(6),
    `oidc_id_token_metadata`        longtext,
    `oidc_id_token_value`           longblob,
    `refresh_token_expires_at`      datetime(6),
    `refresh_token_issued_at`       datetime(6),
    `refresh_token_metadata`        longtext,
    `refresh_token_value`           longblob,
    `state`                         varchar(500),
    `version`                       int default 0 not null,
    `registered_client_id`          varchar(100)  not null,
    `principal_name`                varchar(36)   not null,
    primary key (`id`)
) engine=InnoDB;

create table `oauth2_authorization_consent`
(
    `registered_client_id` varchar(100)  not null,
    `principal_name`       varchar(36)   not null,
    `authorities`          longtext,
    `created_by`           varchar(36),
    `created_date`         datetime(6),
    `last_modified_by`     varchar(36),
    `last_modified_date`   datetime(6),
    `version`              int default 0 not null,
    primary key (`registered_client_id`, `principal_name`)
) engine=InnoDB;

create table `oauth2_authorized_client`
(
    `client_registration_id`  varchar(100)  not null,
    `principal_name`          varchar(36)   not null,
    `access_token_expires_at` datetime(6),
    `access_token_issued_at`  datetime(6),
    `access_token_scopes`     longtext,
    `access_token_type`       varchar(50),
    `access_token_value`      longtext,
    `created_by`              varchar(36),
    `created_date`            datetime(6),
    `last_modified_by`        varchar(36),
    `last_modified_date`      datetime(6),
    `refresh_token_issued_at` datetime(6),
    `refresh_token_value`     longtext,
    `version`                 int default 0 not null,
    primary key (`client_registration_id`, `principal_name`)
) engine=InnoDB;

create table `oauth2_client_registration`
(
    `id`                 varchar(100)  not null,
    `created_by`         varchar(36),
    `created_date`       datetime(6),
    `last_modified_by`   varchar(36),
    `last_modified_date` datetime(6),
    `version`            int default 0 not null,
    primary key (`id`)
) engine=InnoDB;

create table `oauth2_registered_client`
(
    `id`                            varchar(100)  not null,
    `authorization_grant_types`     longtext      not null,
    `client_authentication_methods` longtext      not null,
    `client_id`                     varchar(100)  not null,
    `client_id_issued_at`           datetime(6) not null,
    `client_name`                   varchar(200)  not null,
    `client_secret`                 longtext,
    `client_secret_expires_at`      datetime(6),
    `client_settings`               longtext      not null,
    `created_by`                    varchar(36),
    `created_date`                  datetime(6),
    `last_modified_by`              varchar(36),
    `last_modified_date`            datetime(6),
    `redirect_uris`                 longtext,
    `scopes`                        longtext,
    `token_settings`                longtext      not null,
    `version`                       int default 0 not null,
    primary key (`id`)
) engine=InnoDB;

alter table `oauth2_registered_client`
    add constraint registered_client_ux_client_id unique (`client_id`);

create table `user`
(
    `id`                  varchar(36)   not null,
    `account_expired`     bit           not null,
    `account_locked`      bit           not null,
    `created_by`          varchar(36),
    `created_date`        datetime(6),
    `credentials_expired` bit           not null,
    `disabled`            bit           not null,
    `email`               varchar(255),
    `image`               varchar(255),
    `introduce`           longtext,
    `last_modified_by`    varchar(36),
    `last_modified_date`  datetime(6),
    `name`                varchar(200),
    `password`            longtext,
    `username`            varchar(200)  not null,
    `version`             int default 0 not null,
    primary key (`id`)
) engine=InnoDB;

alter table `user`
    add constraint user_ux_username unique (`username`);

create table `user_authority`
(
    `user_id` varchar(36) not null,
    `name`    varchar(200)
) engine=InnoDB;

create table `group`
(
    `id`                 varchar(36)   not null,
    `created_by`         varchar(36),
    `created_date`       datetime(6),
    `last_modified_by`   varchar(36),
    `last_modified_date` datetime(6),
    `name`               varchar(200)  not null,
    `version`            int default 0 not null,
    primary key (`id`)
) engine=InnoDB;

alter table `group`
    add constraint group_ux_name unique (`name`);

create table `group_authority`
(
    `group_id` varchar(36) not null,
    `name`     varchar(200)
) engine=InnoDB;

create table `group_member`
(
    `group_id` varchar(36) not null,
    `user_id`  varchar(36) not null,
    primary key (`group_id`, `user_id`)
) engine=InnoDB;


-- create table "revision_generator" (
--     "next_val" bigint
-- );
-- insert into "revision_generator"
-- values (100);