-- Issue #62: DB-backed user credential and security token foundation.
-- This migration is additive and prepares the database for the DB-backed
-- authentication, invitation, password reset, and email verification work.

alter table app_users
    drop constraint ck_app_users_status;

alter table app_users
    add constraint ck_app_users_status
    check (status in ('INVITED', 'ACTIVE', 'SUSPENDED', 'ARCHIVED'));

create table user_credentials (
    user_id uuid primary key,
    password_hash varchar(255),
    password_algorithm varchar(64) not null default 'BCRYPT',
    password_changed_at timestamptz,
    must_change_password boolean not null default false,
    email_verified_at timestamptz,
    failed_login_count integer not null default 0,
    locked_until timestamptz,
    last_login_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_user_credentials_user foreign key (user_id) references app_users (id) on delete cascade,
    constraint ck_user_credentials_password_hash_not_blank
        check (password_hash is null or length(trim(password_hash)) > 0),
    constraint ck_user_credentials_password_algorithm
        check (password_algorithm in ('BCRYPT', 'ARGON2ID')),
    constraint ck_user_credentials_password_changed_requires_hash
        check (password_changed_at is null or password_hash is not null),
    constraint ck_user_credentials_failed_login_count_nonnegative
        check (failed_login_count >= 0)
);

create index ix_user_credentials_locked_until
    on user_credentials (locked_until)
    where locked_until is not null;

create index ix_user_credentials_email_verified_at
    on user_credentials (email_verified_at)
    where email_verified_at is not null;

create table user_security_tokens (
    id uuid primary key,
    user_id uuid not null,
    purpose varchar(64) not null,
    token_hash varchar(255) not null,
    token_hash_algorithm varchar(64) not null default 'SHA256',
    expires_at timestamptz not null,
    consumed_at timestamptz,
    revoked_at timestamptz,
    created_by_user_id uuid,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_user_security_tokens_token_hash unique (token_hash),
    constraint fk_user_security_tokens_user foreign key (user_id) references app_users (id) on delete cascade,
    constraint fk_user_security_tokens_created_by_user foreign key (created_by_user_id) references app_users (id) on delete set null,
    constraint ck_user_security_tokens_purpose
        check (purpose in ('INVITATION', 'PASSWORD_RESET', 'EMAIL_VERIFICATION')),
    constraint ck_user_security_tokens_hash_not_blank
        check (length(trim(token_hash)) > 0),
    constraint ck_user_security_tokens_hash_algorithm
        check (token_hash_algorithm in ('SHA256', 'SHA512')),
    constraint ck_user_security_tokens_expires_after_created
        check (expires_at > created_at),
    constraint ck_user_security_tokens_terminal_state_exclusive
        check (consumed_at is null or revoked_at is null)
);

create unique index uq_user_security_tokens_active_user_purpose
    on user_security_tokens (user_id, purpose)
    where consumed_at is null and revoked_at is null;

create index ix_user_security_tokens_user_purpose_expiry
    on user_security_tokens (user_id, purpose, expires_at);

create index ix_user_security_tokens_expiry
    on user_security_tokens (expires_at)
    where consumed_at is null and revoked_at is null;

create index ix_user_security_tokens_created_by_user
    on user_security_tokens (created_by_user_id)
    where created_by_user_id is not null;
