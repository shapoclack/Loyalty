CREATE TABLE roles (name VARCHAR(20) PRIMARY KEY CHECK(name IN ('VIEWER','EDITOR','ADMIN')));
INSERT INTO roles(name) VALUES ('VIEWER'),('EDITOR'),('ADMIN');
ALTER TABLE user_roles ADD CONSTRAINT user_roles_role_fk FOREIGN KEY(role) REFERENCES roles(name);
ALTER TABLE audit_events ADD CONSTRAINT audit_user_fk FOREIGN KEY(user_id) REFERENCES app_users(id) ON DELETE SET NULL;
