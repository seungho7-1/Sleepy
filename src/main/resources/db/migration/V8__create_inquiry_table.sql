-- V8: Create inquiry table
CREATE TABLE IF NOT EXISTS inquiry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    reply TEXT,
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6),
    CONSTRAINT fk_inquiry_member FOREIGN KEY (member_id) REFERENCES member(id)
);
