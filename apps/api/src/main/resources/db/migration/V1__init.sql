CREATE TABLE xtv_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(16) NOT NULL,
  member_until TIMESTAMP NULL,
  CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE TABLE series (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  cover_key VARCHAR(300) NULL,
  status VARCHAR(16) NOT NULL,
  slug VARCHAR(120) NOT NULL,
  CONSTRAINT uk_series_slug UNIQUE (slug)
);

CREATE TABLE category (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  slug VARCHAR(120) NOT NULL,
  sort_order INT NOT NULL,
  show_in_nav BOOLEAN NOT NULL,
  show_on_home BOOLEAN NOT NULL,
  CONSTRAINT uk_category_slug UNIQUE (slug)
);

CREATE TABLE tag (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  slug VARCHAR(120) NOT NULL,
  sort_order INT NOT NULL,
  show_in_nav BOOLEAN NOT NULL,
  show_on_home BOOLEAN NOT NULL,
  CONSTRAINT uk_tag_slug UNIQUE (slug)
);

CREATE TABLE person (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  avatar_key VARCHAR(300) NULL,
  kind VARCHAR(16) NOT NULL,
  slug VARCHAR(120) NOT NULL,
  CONSTRAINT uk_person_slug UNIQUE (slug)
);

CREATE TABLE video (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(300) NOT NULL,
  cover_key VARCHAR(300) NULL,
  duration_ms BIGINT NULL,
  status VARCHAR(32) NOT NULL,
  featured BOOLEAN NOT NULL,
  view_count BIGINT NOT NULL,
  published_at TIMESTAMP NULL,
  series_id BIGINT NULL,
  episode_no INT NULL,
  access_type VARCHAR(16) NOT NULL,
  price_usdt DECIMAL(18, 6) NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE video_asset (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NOT NULL,
  storage_key VARCHAR(400) NOT NULL,
  size_bytes BIGINT NOT NULL,
  checksum VARCHAR(128) NULL,
  CONSTRAINT uk_asset_video UNIQUE (video_id)
);

CREATE TABLE video_rendition (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NOT NULL,
  height INT NOT NULL,
  bandwidth INT NOT NULL,
  playlist_key VARCHAR(400) NOT NULL
);

CREATE TABLE transcode_job (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NOT NULL,
  state VARCHAR(16) NOT NULL,
  progress INT NOT NULL,
  error_message VARCHAR(2000) NULL,
  retry_count INT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE video_category (
  video_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  PRIMARY KEY (video_id, category_id)
);

CREATE TABLE video_tag (
  video_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  PRIMARY KEY (video_id, tag_id)
);

CREATE TABLE video_person (
  video_id BIGINT NOT NULL,
  person_id BIGINT NOT NULL,
  PRIMARY KEY (video_id, person_id)
);

CREATE TABLE video_view_daily (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NOT NULL,
  view_date DATE NOT NULL,
  view_count BIGINT NOT NULL,
  CONSTRAINT uk_view_day UNIQUE (video_id, view_date)
);

CREATE TABLE content_report (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NULL,
  contact VARCHAR(200) NOT NULL,
  message VARCHAR(2000) NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE platform_config (
  id BIGINT NOT NULL PRIMARY KEY,
  storage_mode VARCHAR(16) NOT NULL,
  delivery_mode VARCHAR(16) NOT NULL,
  origin_base VARCHAR(300) NOT NULL,
  cdn_base VARCHAR(300) NULL
);

INSERT INTO platform_config (id, storage_mode, delivery_mode, origin_base, cdn_base)
VALUES (1, 'LOCAL', 'ORIGIN', 'http://localhost:8080', '');

CREATE TABLE membership_plan (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  duration_days INT NOT NULL,
  price_usdt DECIMAL(18, 6) NOT NULL,
  enabled BOOLEAN NOT NULL
);

CREATE TABLE pay_order (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  order_type VARCHAR(16) NOT NULL,
  plan_id BIGINT NULL,
  video_id BIGINT NULL,
  list_price DECIMAL(18, 6) NOT NULL,
  pay_amount DECIMAL(18, 6) NOT NULL,
  payment_method VARCHAR(32) NOT NULL,
  state VARCHAR(16) NOT NULL,
  tx_hash VARCHAR(128) NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_order_tx ON pay_order (tx_hash);

CREATE TABLE video_entitlement (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  video_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  CONSTRAINT uk_entitlement UNIQUE (user_id, video_id)
);

CREATE TABLE advertisement (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  slot VARCHAR(32) NOT NULL,
  image_key VARCHAR(300) NOT NULL,
  target_url VARCHAR(500) NOT NULL,
  enabled BOOLEAN NOT NULL,
  start_at TIMESTAMP NULL,
  end_at TIMESTAMP NULL,
  sort_order INT NOT NULL
);
