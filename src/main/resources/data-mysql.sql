-- MySQL 5.7+ compatible

-- Add person for admin user
INSERT INTO person_t(id, create_by, create_date, update_by, update_date, email, first_name, last_name)
SELECT 1000, 'system', CURDATE(), 'system', NULL, 'admin@bootforum2.dev', 'Admin', 'User'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM person_t WHERE id = 1000
);

-- Add admin user
INSERT INTO user_t(id, create_by, create_date, update_by, update_date, password, user_name, user_role, person_id, account_status)
SELECT 1000, 'system', CURDATE(), 'system', NULL,
       '$2a$10$p1ynqUG2xafPk.QHU6lM7.w5ytRolbovWcib7dC.T5Y2dyq39ELrq',
       'admin', 'ADMIN', 1000, 'ACTIVE'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM user_t WHERE id = 1000
);
