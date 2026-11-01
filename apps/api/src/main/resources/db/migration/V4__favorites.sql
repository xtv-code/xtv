CREATE TABLE user_favorite (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  video_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_favorite UNIQUE (user_id, video_id)
);
