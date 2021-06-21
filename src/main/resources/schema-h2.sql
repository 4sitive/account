create table "client"
(
    "id"                            varchar(36) not null,
    "authorization_grant_types"     varchar(255),
    "client_authentication_methods" varchar(255),
    "client_id"                     varchar(255),
    "client_secret"                 varchar(255),
    "created_by"                    varchar(255),
    "created_date"                  timestamp,
    "last_modified_by"              varchar(255),
    "last_modified_date"            timestamp,
    "name"                          varchar(255),
    "redirect_uris"                 varchar(255),
    "scopes"                        varchar(255),
    "version"                       bigint,
    primary key ("id")
);

create table "revision"
(
    "id"                    bigint generated by default as identity,
    "created_by"            varchar(20),
    "created_date"          timestamp,
    "modified_entity_names" clob not null,
    primary key ("id")
);

create table "user"
(
    "id"                 varchar(36) not null,
    "authorities"        varchar(255),
    "created_by"         varchar(255),
    "created_date"       timestamp,
    "display_name"       varchar(200),
    "last_modified_by"   varchar(255),
    "last_modified_date" timestamp,
    "name"               varchar(200),
    "password"           clob,
    "username"           varchar(200),
    "version"            bigint,
    primary key ("id")
);

create table "user_attribute"
(
    "user_id" varchar(36) not null,
    "value"   clob        not null,
    "key"     varchar(45) not null,
    primary key ("user_id", "key")
);