USE llm;
SELECT id, username, name, type, phone, post_id, status, is_deleted FROM system_user WHERE username = 'admin' AND is_deleted = 0;
DELETE FROM system_user WHERE id = 6 AND username = 'admin';
SELECT id, username, name, post_id, status FROM system_user WHERE username = 'admin' AND is_deleted = 0;
ALTER TABLE system_user ADD CONSTRAINT uk_system_user_username UNIQUE (username);
