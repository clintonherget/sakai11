CREATE TABLE conversations_topic (
    uuid varchar2(255) PRIMARY KEY, 
    site_id varchar2(255) NOT NULL, 
    title varchar2(255) NOT NULL,
    type varchar2(255) NOT NULL
);

CREATE TABLE conversations_post (
    uuid varchar2(255) PRIMARY KEY,
    topic_uuid varchar2(255) NOT NULL,
    content CLOB NOT NULL,
    posted_by varchar2(255) NOT NULL,
    posted_at LONG NOT NULL
);