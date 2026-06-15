CREATE TABLE categories
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    deleted_at TIMESTAMP NULL,
    name       VARCHAR(50) NOT NULL,
    parent_id  BIGINT NULL,
    depth      INT         NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id)
);

CREATE TABLE comments
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at TIMESTAMP      NOT NULL,
    updated_at TIMESTAMP      NOT NULL,
    deleted_at TIMESTAMP NULL,
    post_id    BIGINT        NOT NULL,
    author_id  BIGINT        NOT NULL,
    parent_id  BIGINT NULL,
    content    VARCHAR(2000) NOT NULL,
    deleted    BOOLEAN        NOT NULL,
    CONSTRAINT pk_comments PRIMARY KEY (id)
);

CREATE TABLE email_white_list
(
    id    BIGINT AUTO_INCREMENT NOT NULL,
    email VARCHAR(255) NOT NULL,
    CONSTRAINT pk_emailwhitelist PRIMARY KEY (id)
);

CREATE TABLE feedbacks
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    created_at    TIMESTAMP      NOT NULL,
    updated_at    TIMESTAMP      NOT NULL,
    deleted_at    TIMESTAMP NULL,
    user_id       BIGINT        NOT NULL,
    title         VARCHAR(100)  NOT NULL,
    content       VARCHAR(2000) NOT NULL,
    user_nickname VARCHAR(30)   NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    CONSTRAINT pk_feedbacks PRIMARY KEY (id)
);

CREATE TABLE files
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    deleted_at      TIMESTAMP NULL,
    original_name   VARCHAR(200) NOT NULL,
    s3key           VARCHAR(500) NOT NULL,
    extension       VARCHAR(30)  NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    purpose         VARCHAR(30)  NOT NULL,
    purpose_id      BIGINT       NOT NULL,
    uploaded_by     BIGINT       NOT NULL,
    upload_order    BIGINT       NOT NULL,
    content_type    VARCHAR(100) NULL,
    upload_batch_id VARCHAR(36)  NOT NULL,
    file_type       VARCHAR(30)  NOT NULL,
    CONSTRAINT pk_files PRIMARY KEY (id)
);

CREATE TABLE messages
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    sender_id   BIGINT        NOT NULL,
    receiver_id BIGINT        NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    is_read     BOOLEAN        NOT NULL,
    created_at  TIMESTAMP      NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (id)
);

CREATE TABLE post
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL,
    deleted_at        TIMESTAMP NULL,
    title             VARCHAR(100) NOT NULL,
    content           TEXT         NOT NULL,
    author_id         BIGINT       NOT NULL,
    category_id       BIGINT       NOT NULL,
    thumbnail_key     VARCHAR(500) NULL,
    linked_request_id BIGINT NULL,
    view_count        BIGINT       NOT NULL,
    like_count        BIGINT       NOT NULL,
    total_file_size   BIGINT       NOT NULL,
    image_resolution  VARCHAR(255) NULL,
    polygon           BIGINT       NOT NULL,
    CONSTRAINT pk_post PRIMARY KEY (id)
);

CREATE TABLE post_likes
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    user_id    BIGINT   NOT NULL,
    post_id    BIGINT   NOT NULL,
    CONSTRAINT pk_post_likes PRIMARY KEY (id)
);

CREATE TABLE post_tag
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    post_id    BIGINT   NOT NULL,
    tag_id     BIGINT   NOT NULL,
    CONSTRAINT pk_post_tag PRIMARY KEY (id)
);

CREATE TABLE request_comments
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at TIMESTAMP      NOT NULL,
    updated_at TIMESTAMP      NOT NULL,
    deleted_at TIMESTAMP NULL,
    request_id BIGINT        NOT NULL,
    author_id  BIGINT        NOT NULL,
    parent_id  BIGINT NULL,
    content    VARCHAR(2000) NOT NULL,
    deleted    BOOLEAN        NOT NULL,
    CONSTRAINT pk_request_comments PRIMARY KEY (id)
);

CREATE TABLE request_posts
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    deleted_at      TIMESTAMP NULL,
    title           VARCHAR(100) NOT NULL,
    content         TEXT         NOT NULL,
    asset_type      VARCHAR(60) NULL,
    preferred_style VARCHAR(60) NULL,
    engine          VARCHAR(60) NULL,
    deadline        TIMESTAMP NULL,
    status          VARCHAR(20)  NOT NULL,
    thumbnail_key   VARCHAR(500) NULL,
    requester_id    BIGINT       NOT NULL,
    assignee_id     BIGINT NULL,
    linked_post_id  BIGINT NULL,
    CONSTRAINT pk_request_posts PRIMARY KEY (id)
);

CREATE TABLE tags
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    deleted_at TIMESTAMP NULL,
    name       VARCHAR(30) NOT NULL,
    CONSTRAINT pk_tags PRIMARY KEY (id)
);

CREATE TABLE users
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    deleted_at      TIMESTAMP NULL,
    email           VARCHAR(50)  NOT NULL,
    public_email    VARCHAR(50) NULL,
    password        VARCHAR(255) NOT NULL,
    name            VARCHAR(50)  NOT NULL,
    nickname        VARCHAR(30)  NOT NULL,
    major           VARCHAR(50)  NOT NULL,
    `description`   TEXT NULL,
    provider        VARCHAR(20)  NOT NULL,
    avatar_key      VARCHAR(500) NULL,
    is_oauth_linked INT DEFAULT 0 NULL,
    `role`          VARCHAR(20)  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE email_white_list
    ADD CONSTRAINT uc_emailwhitelist_email UNIQUE (email);

ALTER TABLE post_likes
    ADD CONSTRAINT uc_f9da256126462064fa4be8c66 UNIQUE (user_id, post_id);

ALTER TABLE tags
    ADD CONSTRAINT uc_tags_name UNIQUE (name);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE files
    ADD CONSTRAINT FK_FILES_ON_UPLOADED_BY FOREIGN KEY (uploaded_by) REFERENCES users (id);

ALTER TABLE post_tag
    ADD CONSTRAINT FK_POST_TAG_ON_POST FOREIGN KEY (post_id) REFERENCES post (id);

ALTER TABLE post_tag
    ADD CONSTRAINT FK_POST_TAG_ON_TAG FOREIGN KEY (tag_id) REFERENCES tags (id);