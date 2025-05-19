-- Deprecated: use seed-data.json instead
-- Add person for admin user
MERGE INTO person_t p
USING (SELECT 1000 AS id FROM dual) src
ON (p.id = src.id)
WHEN NOT MATCHED THEN
  INSERT (id, create_by, create_date, update_by, update_date, email, first_name, last_name)
  VALUES (1000, 'system', SYSDATE, 'system', NULL, 'admin@bootforum2.dev', 'Admin', 'User');

-- Add admin user
MERGE INTO user_t u
USING (SELECT 1000 AS id FROM dual) src
ON (u.id = src.id)
WHEN NOT MATCHED THEN
  INSERT (id, create_by, create_date, update_by, update_date, password, user_name, user_role, person_id, account_status)
  VALUES (1000, 'system', SYSDATE, 'system', NULL,
         '$2a$10$p1ynqUG2xafPk.QHU6lM7.w5ytRolbovWcib7dC.T5Y2dyq39ELrq',
         'admin', 'ADMIN', 1000, 'ACTIVE');
