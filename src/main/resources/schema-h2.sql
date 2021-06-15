create table CLIENT
(
    ID VARCHAR(36) not null,
    CLIENT_ID VARCHAR(200) not null,
    NAME VARCHAR(200)
);

create unique index CLIENT_CLIENT_ID_UINDEX
	on CLIENT (CLIENT_ID);

alter table CLIENT
    add constraint CLIENT_PK
        primary key (CLIENT_ID);

