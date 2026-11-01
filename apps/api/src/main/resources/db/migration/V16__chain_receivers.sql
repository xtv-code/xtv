CREATE TABLE chain_receiver (
  code VARCHAR(32) NOT NULL PRIMARY KEY,
  address VARCHAR(128) NOT NULL DEFAULT ''
);

INSERT INTO chain_receiver (code, address) VALUES
  ('ETH', ''),
  ('POLYGON', ''),
  ('ARBITRUM', ''),
  ('AVALANCHE', ''),
  ('SOLANA', ''),
  ('TON', '');
