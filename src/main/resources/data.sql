-- Add person for admin user
insert into person_t(id, create_by, create_date, update_by, update_date, email, first_name, last_name)
select 1000, 'system', current_date, 'system', null, 'admin@bootforum2.dev', 'Admin', 'User'
WHERE NOT EXISTS (
    SELECT 1 FROM person_t WHERE id = 1000
);

-- Add admin user, encrypted password is 'secret'
insert into user_t(id, create_by, create_date, update_by, update_date, password, user_name, user_role, person_id, account_status)
select 1000, 'system', current_date, 'system', null, '$2a$10$p1ynqUG2xafPk.QHU6lM7.w5ytRolbovWcib7dC.T5Y2dyq39ELrq', 'admin', 'ADMIN', 1000, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM user_t WHERE id = 1000
);


