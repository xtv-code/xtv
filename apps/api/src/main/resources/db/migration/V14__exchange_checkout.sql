ALTER TABLE pay_order ADD COLUMN checkout_url VARCHAR(1024) NOT NULL DEFAULT '';
ALTER TABLE pay_order ADD COLUMN external_id VARCHAR(80) NOT NULL DEFAULT '';

CREATE TABLE exchange_merchant (
  code VARCHAR(16) PRIMARY KEY,
  merchant_id VARCHAR(64) NOT NULL DEFAULT '',
  api_key VARCHAR(200) NOT NULL DEFAULT '',
  api_secret VARCHAR(200) NOT NULL DEFAULT '',
  enabled BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO exchange_merchant (code, merchant_id, api_key, api_secret, enabled) VALUES
  ('BINANCE', '', '', '', FALSE),
  ('GATE', '', '', '', FALSE),
  ('BYBIT', '', '', '', FALSE);
