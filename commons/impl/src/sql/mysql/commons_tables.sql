CREATE TABLE COMMONS_COMMONS
(
    ID char(36) NOT NULL,
    SITE_ID varchar(99) NOT NULL,
    EMBEDDER varchar(24) NOT NULL,
    PRIMARY KEY(ID)
);

CREATE TABLE COMMONS_COMMONS_POST
(
    COMMONS_ID char(36) references COMMONS_COMMONS(ID),
    POST_ID char(36) references COMMONS_POST(ID),
    UNIQUE INDEX commons_id_post_id (COMMONS_ID,POST_ID)
);

CREATE TABLE COMMONS_POST
(
    ID char(36) NOT NULL,
    CONTENT MEDIUMTEXT NOT NULL,
    CREATOR_ID varchar(99) NOT NULL,
    CREATED_DATE datetime NOT NULL,
    MODIFIED_DATE datetime NOT NULL,
    RELEASE_DATE datetime NOT NULL,
    INDEX creator_id (CREATOR_ID),
    PRIMARY KEY(ID)
);

CREATE TABLE COMMONS_COMMENT
(
    ID char(36) NOT NULL,
    POST_ID char(36) references COMMONS_POST(ID),
    CONTENT MEDIUMTEXT NOT NULL,
    CREATOR_ID varchar(99) NOT NULL,
    CREATED_DATE datetime NOT NULL,
    MODIFIED_DATE datetime NOT NULL,
    INDEX creator_id (CREATOR_ID),
    INDEX post_id (POST_ID),
    PRIMARY KEY(ID)
);
