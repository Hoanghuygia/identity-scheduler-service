ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN IF NOT EXISTS provider_subject VARCHAR(255),
    ADD COLUMN IF NOT EXISTS replaced_by_token_id UUID;

UPDATE refresh_tokens
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE refresh_tokens
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT NOW();

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_replaced_by
    FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens(id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked ON refresh_tokens(user_id, revoked);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_provider ON refresh_tokens(provider);
