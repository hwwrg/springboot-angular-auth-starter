-- Issue #2: TOTP-based multi-factor authentication with recovery codes.
-- Additive schema: a per-user TOTP secret plus single-use hashed recovery codes.

create table user_mfa_totp (
    user_id uuid primary key,
    secret varchar(255) not null,
    status varchar(32) not null default 'PENDING',
    confirmed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_user_mfa_totp_user foreign key (user_id) references app_users (id) on delete cascade,
    constraint ck_user_mfa_totp_status check (status in ('PENDING', 'ENABLED')),
    constraint ck_user_mfa_totp_secret_not_blank check (length(trim(secret)) > 0),
    constraint ck_user_mfa_totp_enabled_requires_confirm
        check (status <> 'ENABLED' or confirmed_at is not null)
);

create table user_mfa_recovery_codes (
    id uuid primary key,
    user_id uuid not null,
    code_hash varchar(255) not null,
    code_hash_algorithm varchar(64) not null default 'SHA256',
    consumed_at timestamptz,
    created_at timestamptz not null default now(),
    constraint fk_user_mfa_recovery_codes_user foreign key (user_id) references app_users (id) on delete cascade,
    constraint uq_user_mfa_recovery_codes_hash unique (code_hash),
    constraint ck_user_mfa_recovery_codes_hash_not_blank check (length(trim(code_hash)) > 0),
    constraint ck_user_mfa_recovery_codes_hash_algorithm
        check (code_hash_algorithm in ('SHA256', 'SHA512'))
);

create index ix_user_mfa_recovery_codes_user_active
    on user_mfa_recovery_codes (user_id)
    where consumed_at is null;
