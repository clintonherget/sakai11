CREATE TABLE conversations_topic (
    uuid varchar2(255) PRIMARY KEY, 
    site_id varchar2(255) NOT NULL, 
    title varchar2(255) NOT NULL,
    type varchar2(255) NOT NULL
);