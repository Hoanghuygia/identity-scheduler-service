ALTER TABLE email_verification_tokens
ADD COLUMN purpose VARCHAR(50);

UPDATE email_verification_tokens
SET purpose = 'REGISTER_VERIFICATION'
WHERE purpose IS NULL;

ALTER TABLE email_verification_tokens
ALTER COLUMN purpose SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_purpose_used
ON email_verification_tokens (user_id, purpose, used);
