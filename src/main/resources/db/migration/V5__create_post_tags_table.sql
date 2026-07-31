CREATE TABLE post_tags (
    post_id BIGINT NOT NULL,
    hashtag VARCHAR(255),
    CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE
);

-- 기존 잘못 생성된 post_hashtags 테이블이 있다면 안전하게 삭제 (선택적)
DROP TABLE IF EXISTS post_hashtags;
