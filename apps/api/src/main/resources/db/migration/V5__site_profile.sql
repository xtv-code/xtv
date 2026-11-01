ALTER TABLE platform_config ADD COLUMN site_name VARCHAR(40) NOT NULL DEFAULT 'XTV';
ALTER TABLE platform_config ADD COLUMN site_tagline VARCHAR(200) NOT NULL DEFAULT '点播平台。本地转码、会员订阅与单集购买，面向 Web 与 H5。';
ALTER TABLE platform_config ADD COLUMN catalog_seeded BOOLEAN NOT NULL DEFAULT FALSE;
