-- 새 빈 DB를 선택한 뒤 최초 1회 수동 실행하세요. 기존 DB에는 실행하지 마세요.
-- 더미 데이터와 Flyway 의존성은 없으며, 서비스 시작 시 자동 실행되지 않습니다.
-- Chapchap Auth - initial schema for a NEW, EMPTY MySQL 8 database.
-- Run manually after selecting the service database: SOURCE sql/schema.sql;
-- Never run against an existing database. No seed data, database creation or cleanup.
-- This file is outside classpath resources and is not automatically executed.
-- Foreign keys reflect JPA relationships only; other service IDs remain logical references.
-- No cascading deletes. Timestamps/default values are supplied by the application.
-- Binary collation keeps opaque identifiers and unique keys case-sensitive.
-- ddl-auto=validate remains enabled. Later changes require separate reviewed ALTER SQL.

create table admin_credentials (
    failed_login_count integer not null,
    must_change_password bit not null,
    admin_credential_id bigint not null auto_increment,
    created_at datetime(6) not null,
    disabled_at datetime(6),
    last_failed_at datetime(6),
    last_login_at datetime(6),
    locked_until datetime(6),
    updated_at datetime(6) not null,
    user_id bigint not null,
    username varchar(50) not null,
    password_hash varchar(255) not null,
    primary key (admin_credential_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table audit_logs (
    actor_user_id bigint,
    audit_log_id bigint not null auto_increment,
    created_at datetime(6) not null,
    target_id bigint,
    trace_id varchar(64),
    action_type enum ('ADMIN_CREATED','ADMIN_DISABLED','ADMIN_LOCKED','ADMIN_LOGIN_FAILED','ADMIN_LOGIN_SUCCEEDED','ADMIN_PASSWORD_CHANGED','ADMIN_PASSWORD_RESET','ADMIN_UNLOCKED','RIDER_ROLE_GRANTED','RIDER_ROLE_REVOKED','TOKEN_REUSE_DETECTED','USER_WITHDRAWN') not null,
    detail json,
    result enum ('BLOCKED','FAILURE','SUCCESS') not null,
    target_type enum ('ADMIN_CREDENTIAL','AUTH_SESSION','USER') not null,
    primary key (audit_log_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table auth_sessions (
    absolute_expires_at datetime(6) not null,
    created_at datetime(6) not null,
    idle_expires_at datetime(6) not null,
    last_used_at datetime(6) not null,
    revoked_at datetime(6),
    session_id bigint not null auto_increment,
    user_id bigint not null,
    session_type enum ('ADMIN','USER') not null,
    primary key (session_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table policies (
    is_active bit not null,
    is_required bit not null,
    created_at datetime(6) not null,
    effective_at datetime(6) not null,
    policy_id bigint not null auto_increment,
    version varchar(20) not null,
    title varchar(100) not null,
    content TEXT not null,
    policy_type enum ('MARKETING_EMAIL','PRIVACY_POLICY','TERMS_OF_SERVICE') not null,
    primary key (policy_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table refresh_tokens (
    consumed_at datetime(6),
    created_at datetime(6) not null,
    expires_at datetime(6) not null,
    refresh_token_id bigint not null auto_increment,
    session_id bigint not null,
    token_hash varchar(44) not null,
    primary key (refresh_token_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table signup_sessions (
    consumed_at datetime(6),
    created_at datetime(6) not null,
    expires_at datetime(6) not null,
    updated_at datetime(6) not null,
    verified_at datetime(6),
    signup_session_id varchar(36) not null,
    identity_key varchar(64),
    identity_verification_id varchar(191),
    provider_user_id varchar(191) not null,
    provider enum ('GOOGLE','KAKAO') not null,
    status enum ('COMPLETED','EXPIRED','FAILED','IDENTITY_VERIFIED','PENDING') not null,
    primary key (signup_session_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table social_accounts (
    created_at datetime(6) not null,
    social_account_id bigint not null auto_increment,
    user_id bigint not null,
    provider_user_id varchar(191) not null,
    provider enum ('GOOGLE','KAKAO') not null,
    primary key (social_account_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table user_policy_consents (
    consent_id bigint not null auto_increment,
    created_at datetime(6) not null,
    decided_at datetime(6) not null,
    policy_id bigint not null,
    updated_at datetime(6) not null,
    user_id bigint not null,
    consent_status enum ('AGREED','DECLINED','WITHDRAWN') not null,
    primary key (consent_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table users (
    created_at datetime(6) not null,
    default_address_id bigint,
    default_address_version bigint not null,
    identity_verified_at datetime(6),
    subscription_version bigint not null,
    updated_at datetime(6) not null,
    user_id bigint not null auto_increment,
    withdrawn_at datetime(6),
    phone varchar(20),
    name varchar(50) not null,
    identity_key varchar(64),
    email varchar(255),
    profile_image_key varchar(255),
    role enum ('ADMIN','CUSTOMER','RIDER','SUPER_ADMIN') not null,
    status enum ('ACTIVE','SUSPENDED','WITHDRAWN') not null,
    subscription_status enum ('ACTIVE','INACTIVE','UNKNOWN') not null,
    primary key (user_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

alter table admin_credentials
   add constraint uk_admin_credentials_user unique (user_id);

alter table admin_credentials
   add constraint uk_admin_credentials_username unique (username);

alter table policies
   add constraint uk_policies_policy_type_version unique (policy_type, version);

alter table refresh_tokens
   add constraint uk_refresh_tokens_token_hash unique (token_hash);

alter table signup_sessions
   add constraint uk_signup_sessions_identity_verification_id unique (identity_verification_id);

alter table social_accounts
   add constraint uk_social_accounts_provider_user_id unique (provider, provider_user_id);

alter table social_accounts
   add constraint uk_social_accounts_user_provider unique (user_id, provider);

alter table user_policy_consents
   add constraint uk_user_policy_consents_user_policy unique (user_id, policy_id);

alter table users
   add constraint uk_users_identity_key unique (identity_key);

alter table admin_credentials
   add constraint fk_admin_credentials_user_id foreign key (user_id)
   references users (user_id);

alter table auth_sessions
   add constraint fk_auth_sessions_user_id foreign key (user_id)
   references users (user_id);

alter table refresh_tokens
   add constraint fk_refresh_tokens_session_id foreign key (session_id)
   references auth_sessions (session_id);

alter table social_accounts
   add constraint fk_social_accounts_user_id foreign key (user_id)
   references users (user_id);

alter table user_policy_consents
   add constraint fk_user_policy_consents_policy_id foreign key (policy_id)
   references policies (policy_id);

alter table user_policy_consents
   add constraint fk_user_policy_consents_user_id foreign key (user_id)
   references users (user_id);
