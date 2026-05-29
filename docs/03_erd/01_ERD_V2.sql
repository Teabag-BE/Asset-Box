-- =========================================================
-- ERDCloud Import SQL
-- 테이블 이름: PascalCase
-- Physical 열 이름: snake_case
-- Logical 열 이름: camelCase
-- =========================================================

CREATE TABLE `User` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    email VARCHAR(50) NOT NULL COMMENT 'email',
    password VARCHAR(255) NULL COMMENT 'password',
    name VARCHAR(50) NOT NULL COMMENT 'name',
    nickname VARCHAR(30) NOT NULL COMMENT 'nickname',
    major VARCHAR(50) NULL COMMENT 'major',
    provider VARCHAR(20) NOT NULL COMMENT 'provider',
    provider_subject VARCHAR(100) NULL COMMENT 'providerSubject',
    avatar_path VARCHAR(255) NULL COMMENT 'avatarPath',
    role VARCHAR(20) NOT NULL COMMENT 'role',
    created_at TIMESTAMP NOT NULL COMMENT 'createdAt',
    updated_at TIMESTAMP NOT NULL COMMENT 'updatedAt',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_email (email)
) COMMENT='User';

CREATE TABLE `Category` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    name VARCHAR(50) NOT NULL COMMENT 'name',
    parent_id BIGINT NULL COMMENT 'parentId',
    depth INT NOT NULL COMMENT 'depth',
    sort_order INT NOT NULL COMMENT 'sortOrder',
    created_at TIMESTAMP NOT NULL COMMENT 'createdAt',
    updated_at TIMESTAMP NOT NULL COMMENT 'updatedAt',
    PRIMARY KEY (id),
    CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_id) REFERENCES `Category` (id)
) COMMENT='Category';

CREATE TABLE `AssetFile` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    original_name VARCHAR(200) NOT NULL COMMENT 'originalName',
    stored_path VARCHAR(500) NOT NULL COMMENT 'storedPath',
    extension VARCHAR(30) NOT NULL COMMENT 'extension',
    size_bytes BIGINT NOT NULL COMMENT 'sizeBytes',
    purpose VARCHAR(30) NOT NULL COMMENT 'purpose',
    resource_type VARCHAR(30) NOT NULL COMMENT 'resourceType',
    resource_id BIGINT NOT NULL COMMENT 'resourceId',
    uploaded_by BIGINT NOT NULL COMMENT 'uploadedBy',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'deleted',
    created_at TIMESTAMP NOT NULL COMMENT 'createdAt',
    updated_at TIMESTAMP NOT NULL COMMENT 'updatedAt',
    PRIMARY KEY (id),
    KEY idx_asset_file_resource (resource_type, resource_id),
    KEY idx_asset_file_uploaded_by (uploaded_by),
    CONSTRAINT fk_asset_file_uploaded_by
        FOREIGN KEY (uploaded_by) REFERENCES `User` (id)
) COMMENT='AssetFile';

CREATE TABLE `Post` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    title VARCHAR(100) NOT NULL COMMENT 'title',
    content TEXT NOT NULL COMMENT 'content',
    author_id BIGINT NOT NULL COMMENT 'authorId',
    category_id BIGINT NOT NULL COMMENT 'categoryId',
    thumbnail_file_id BIGINT NULL COMMENT 'thumbnailFileId',
    linked_request_id BIGINT NULL COMMENT 'linkedRequestId',
    view_count BIGINT NOT NULL DEFAULT 0 COMMENT 'viewCount',
    like_count BIGINT NOT NULL DEFAULT 0 COMMENT 'likeCount',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'deleted',
    created_at TIMESTAMP NOT NULL COMMENT 'createdAt',
    updated_at TIMESTAMP NOT NULL COMMENT 'updatedAt',
    PRIMARY KEY (id),
    KEY idx_post_author (author_id),
    KEY idx_post_category (category_id),
    KEY idx_post_thumbnail_file (thumbnail_file_id),
    KEY idx_post_linked_request (linked_request_id),
    CONSTRAINT fk_post_author
        FOREIGN KEY (author_id) REFERENCES `User` (id),
    CONSTRAINT fk_post_category
        FOREIGN KEY (category_id) REFERENCES `Category` (id),
    CONSTRAINT fk_post_thumbnail_file
        FOREIGN KEY (thumbnail_file_id) REFERENCES `AssetFile` (id)
) COMMENT='Post';

CREATE TABLE `Tag` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    name VARCHAR(30) NOT NULL COMMENT 'name',
    created_at TIMESTAMP NOT NULL COMMENT 'createdAt',
    updated_at TIMESTAMP NOT NULL COMMENT 'updatedAt',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_name (name)
) COMMENT='Tag';

CREATE TABLE `PostTag` (
    post_id BIGINT NOT NULL COMMENT 'postId',
    tag_id BIGINT NOT NULL COMMENT 'tagId',
    PRIMARY KEY (post_id, tag_id),
    CONSTRAINT fk_post_tag_post
        FOREIGN KEY (post_id) REFERENCES `Post` (id),
    CONSTRAINT fk_post_tag_tag
        FOREIGN KEY (tag_id) REFERENCES `Tag` (id)
) COMMENT='PostTag';

CREATE TABLE `PostLike` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    user_id BIGINT NOT NULL COMMENT 'userId',
    post_id BIGINT NOT NULL COMMENT 'postId',
    created_at TIMESTAMP NOT NULL COMMENT 'createdAt',
    updated_at TIMESTAMP NOT NULL COMMENT 'updatedAt',
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_like_user_post (user_id, post_id),
    KEY idx_post_like_post (post_id),
    CONSTRAINT fk_post_like_user
        FOREIGN KEY (user_id) REFERENCES `User` (id),
    CONSTRAINT fk_post_like_post
        FOREIGN KEY (post_id) REFERENCES `Post` (id)
) COMMENT='PostLike';

CREATE TABLE `Comment` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    post_id BIGINT NOT NULL COMMENT 'postId',
    author_id BIGINT NOT NULL COMMENT 'authorId',
    parent_id BIGINT NULL COMMENT 'parentId',
    content VARCHAR(2000) NOT NULL COMMENT 'content',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'deleted',
    created_at TIMESTAMP NOT NULL COMMENT 'createdAt',
    updated_at TIMESTAMP NOT NULL COMMENT 'updatedAt',
    PRIMARY KEY (id),
    KEY idx_comment_post (post_id),
    KEY idx_comment_author (author_id),
    KEY idx_comment_parent (parent_id),
    CONSTRAINT fk_comment_post
        FOREIGN KEY (post_id) REFERENCES `Post` (id),
    CONSTRAINT fk_comment_author
        FOREIGN KEY (author_id) REFERENCES `User` (id),
    CONSTRAINT fk_comment_parent
        FOREIGN KEY (parent_id) REFERENCES `Comment` (id)
) COMMENT='Comment';

CREATE TABLE `RequestPost` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    title VARCHAR(100) NOT NULL COMMENT 'title',
    content TEXT NOT NULL COMMENT 'content',
    asset_type VARCHAR(60) NULL COMMENT 'assetType',
    preferred_style VARCHAR(60) NULL COMMENT 'preferredStyle',
    engine VARCHAR(60) NULL COMMENT 'engine',
    deadline DATE NULL COMMENT 'deadline',
    status VARCHAR(20) NOT NULL COMMENT 'status',
    requester_id BIGINT NOT NULL COMMENT 'requesterId',
    assignee_id BIGINT NULL COMMENT 'assigneeId',
    linked_post_id BIGINT NULL COMMENT 'linkedPostId',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'deleted',
    created_at TIMESTAMP NOT NULL COMMENT 'createdAt',
    updated_at TIMESTAMP NOT NULL COMMENT 'updatedAt',
    PRIMARY KEY (id),
    KEY idx_request_post_requester (requester_id),
    KEY idx_request_post_assignee (assignee_id),
    KEY idx_request_post_linked_post (linked_post_id),
    CONSTRAINT fk_request_post_requester
        FOREIGN KEY (requester_id) REFERENCES `User` (id),
    CONSTRAINT fk_request_post_assignee
        FOREIGN KEY (assignee_id) REFERENCES `User` (id),
    CONSTRAINT fk_request_post_linked_post
        FOREIGN KEY (linked_post_id) REFERENCES `Post` (id)
) COMMENT='RequestPost';

ALTER TABLE `Post`
    ADD CONSTRAINT fk_post_linked_request
        FOREIGN KEY (linked_request_id) REFERENCES `RequestPost` (id);

CREATE TABLE `RequestComment` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    request_id BIGINT NOT NULL COMMENT 'requestId',
    author_id BIGINT NOT NULL COMMENT 'authorId',
    parent_id BIGINT NULL COMMENT 'parentId',
    content VARCHAR(2000) NOT NULL COMMENT 'content',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'deleted',
    created_at TIMESTAMP NOT NULL COMMENT 'createdAt',
    updated_at TIMESTAMP NOT NULL COMMENT 'updatedAt',
    PRIMARY KEY (id),
    KEY idx_request_comment_request (request_id),
    KEY idx_request_comment_author (author_id),
    KEY idx_request_comment_parent (parent_id),
    CONSTRAINT fk_request_comment_request
        FOREIGN KEY (request_id) REFERENCES `RequestPost` (id),
    CONSTRAINT fk_request_comment_author
        FOREIGN KEY (author_id) REFERENCES `User` (id),
    CONSTRAINT fk_request_comment_parent
        FOREIGN KEY (parent_id) REFERENCES `RequestComment` (id)
) COMMENT='RequestComment';

CREATE TABLE `Message` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    sender_id BIGINT NOT NULL COMMENT 'senderId',
    receiver_id BIGINT NOT NULL COMMENT 'receiverId',
    content TEXT NOT NULL COMMENT 'content',
    is_read BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'isRead',
    created_at TIMESTAMP NOT NULL COMMENT 'createdAt',
    updated_at TIMESTAMP NOT NULL COMMENT 'updatedAt',
    PRIMARY KEY (id),
    KEY idx_message_sender (sender_id),
    KEY idx_message_receiver (receiver_id),
    CONSTRAINT fk_message_sender
        FOREIGN KEY (sender_id) REFERENCES `User` (id),
    CONSTRAINT fk_message_receiver
        FOREIGN KEY (receiver_id) REFERENCES `User` (id)
) COMMENT='Message';

CREATE TABLE `Feedback` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    user_id BIGINT NOT NULL COMMENT 'userId',
    title VARCHAR(100) NOT NULL COMMENT 'title',
    content VARCHAR(2000) NOT NULL COMMENT 'content',
    user_nickname VARCHAR(30) NOT NULL COMMENT 'userNickname',
    status VARCHAR(20) NOT NULL COMMENT 'status',
    created_at TIMESTAMP NOT NULL COMMENT 'createdAt',
    updated_at TIMESTAMP NOT NULL COMMENT 'updatedAt',
    PRIMARY KEY (id),
    KEY idx_feedback_user (user_id),
    CONSTRAINT fk_feedback_user
        FOREIGN KEY (user_id) REFERENCES `User` (id)
) COMMENT='Feedback';
