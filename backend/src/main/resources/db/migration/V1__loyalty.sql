CREATE TABLE app_users (
 id UUID PRIMARY KEY, login VARCHAR(100) NOT NULL UNIQUE, name VARCHAR(120) NOT NULL,
 password_hash VARCHAR(100) NOT NULL, active BOOLEAN NOT NULL, must_change_password BOOLEAN NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE user_roles (user_id UUID NOT NULL REFERENCES app_users(id), role VARCHAR(20) NOT NULL CHECK(role IN ('VIEWER','EDITOR','ADMIN')), PRIMARY KEY(user_id,role));
CREATE TABLE refresh_sessions (id UUID PRIMARY KEY,user_id UUID NOT NULL REFERENCES app_users(id),token_hash VARCHAR(64) NOT NULL UNIQUE,expires_at TIMESTAMP WITH TIME ZONE NOT NULL,revoked BOOLEAN NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL);
CREATE INDEX refresh_user_idx ON refresh_sessions(user_id);
CREATE TABLE segments (id UUID PRIMARY KEY,name VARCHAR(80) NOT NULL UNIQUE,active BOOLEAN NOT NULL,version BIGINT NOT NULL DEFAULT 0);
CREATE TABLE customers (
 id UUID PRIMARY KEY,code VARCHAR(64) NOT NULL UNIQUE,name VARCHAR(120) NOT NULL,email VARCHAR(254),phone VARCHAR(30),description VARCHAR(2000),segment_id UUID NOT NULL REFERENCES segments(id),status VARCHAR(20) NOT NULL CHECK(status IN ('ACTIVE','BLOCKED','INACTIVE')),total_purchases NUMERIC(15,2) NOT NULL CHECK(total_purchases>=0),marketing_consent BOOLEAN NOT NULL,archived BOOLEAN NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,updated_at TIMESTAMP WITH TIME ZONE NOT NULL,version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX customers_name_idx ON customers(name);
CREATE INDEX customers_email_idx ON customers(email);
CREATE INDEX customers_segment_idx ON customers(segment_id);
CREATE INDEX customers_status_archive_idx ON customers(status,archived);
CREATE INDEX customers_amount_idx ON customers(total_purchases);
CREATE INDEX customers_created_idx ON customers(created_at,id);
CREATE TABLE customer_tags (customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,tag VARCHAR(40) NOT NULL,PRIMARY KEY(customer_id,tag));
CREATE INDEX customer_tags_tag_idx ON customer_tags(tag);
CREATE TABLE notes (id UUID PRIMARY KEY,customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,author_id UUID NOT NULL REFERENCES app_users(id),text VARCHAR(4000) NOT NULL,pinned BOOLEAN NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,updated_at TIMESTAMP WITH TIME ZONE NOT NULL,version BIGINT NOT NULL DEFAULT 0);
CREATE INDEX notes_customer_idx ON notes(customer_id,created_at);
CREATE TABLE audit_events (id UUID PRIMARY KEY,user_id UUID,username VARCHAR(100),event VARCHAR(60),entity_type VARCHAR(60),entity_id UUID,created_at TIMESTAMP WITH TIME ZONE NOT NULL,description VARCHAR(500),correlation_id VARCHAR(80));
CREATE INDEX audit_created_idx ON audit_events(created_at);
CREATE INDEX audit_user_idx ON audit_events(user_id);
CREATE INDEX audit_event_idx ON audit_events(event);
CREATE INDEX audit_correlation_idx ON audit_events(correlation_id);
CREATE TABLE import_jobs (id UUID PRIMARY KEY,user_id UUID NOT NULL REFERENCES app_users(id),payload TEXT NOT NULL,mode VARCHAR(20) NOT NULL CHECK(mode IN ('NEW_ONLY','UPSERT')),expires_at TIMESTAMP WITH TIME ZONE NOT NULL,applied BOOLEAN NOT NULL,version BIGINT NOT NULL DEFAULT 0);
CREATE TABLE login_attempts (key VARCHAR(64) PRIMARY KEY,failures INTEGER NOT NULL,window_start TIMESTAMP WITH TIME ZONE NOT NULL,locked_until TIMESTAMP WITH TIME ZONE);
