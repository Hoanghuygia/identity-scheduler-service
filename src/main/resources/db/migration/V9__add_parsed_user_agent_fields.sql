-- Add parsed user agent fields to auth_audit_logs table
ALTER TABLE auth_audit_logs 
ADD COLUMN browser_name VARCHAR(100),
ADD COLUMN browser_version VARCHAR(50), 
ADD COLUMN operating_system VARCHAR(100),
ADD COLUMN device_type VARCHAR(50);

-- Add indexes for common queries
CREATE INDEX idx_auth_audit_logs_browser_name ON auth_audit_logs(browser_name);
CREATE INDEX idx_auth_audit_logs_operating_system ON auth_audit_logs(operating_system);  
CREATE INDEX idx_auth_audit_logs_device_type ON auth_audit_logs(device_type);

-- Add index for IP address queries
CREATE INDEX idx_auth_audit_logs_ip_address ON auth_audit_logs(ip_address);