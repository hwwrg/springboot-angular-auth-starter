-- Generic multi-organization user and RBAC foundation.
-- Organization is the reusable account boundary for the starter.
-- Workspace is the technical isolation scope owned by an organization.

create table workspaces (
    id uuid primary key,
    code varchar(80) not null,
    name varchar(160) not null,
    status varchar(32) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_workspaces_code unique (code),
    constraint ck_workspaces_status check (status in ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
);

create table organizations (
    id uuid primary key,
    workspace_id uuid not null,
    legal_name varchar(200) not null,
    display_name varchar(160) not null,
    status varchar(32) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_organizations_workspace unique (workspace_id),
    constraint fk_organizations_workspace foreign key (workspace_id) references workspaces (id),
    constraint ck_organizations_status check (status in ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
);

create table app_users (
    id uuid primary key,
    email varchar(320) not null,
    display_name varchar(160) not null,
    status varchar(32) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_app_users_email unique (email),
    constraint ck_app_users_email_lowercase check (email = lower(email)),
    constraint ck_app_users_status check (status in ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
);

create table organization_memberships (
    id uuid primary key,
    organization_id uuid not null,
    user_id uuid not null,
    role varchar(32) not null,
    status varchar(32) not null,
    primary_membership boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_organization_memberships_organization_user unique (organization_id, user_id),
    constraint fk_organization_memberships_organization foreign key (organization_id) references organizations (id),
    constraint fk_organization_memberships_user foreign key (user_id) references app_users (id),
    constraint ck_organization_memberships_role check (role in ('SUPERADMIN', 'ORG_ADMIN', 'USER')),
    constraint ck_organization_memberships_status check (status in ('ACTIVE', 'INVITED', 'SUSPENDED', 'ARCHIVED'))
);

create unique index uq_organization_memberships_primary_user
    on organization_memberships (user_id)
    where primary_membership;

create index ix_organization_memberships_user_status
    on organization_memberships (user_id, status);

create index ix_organization_memberships_organization_status
    on organization_memberships (organization_id, status);

insert into workspaces (id, code, name, status)
values (
    '10000000-0000-4000-8000-000000000001',
    'local-authstarter',
    'Local Auth Starter workspace',
    'ACTIVE'
);

insert into organizations (id, workspace_id, legal_name, display_name, status)
values (
    '20000000-0000-4000-8000-000000000001',
    '10000000-0000-4000-8000-000000000001',
    'Auth Starter Local Organization',
    'Auth Starter Local',
    'ACTIVE'
);

insert into app_users (id, email, display_name, status)
values (
    '30000000-0000-4000-8000-000000000001',
    'operator@authstarter.local',
    'Baseline Operator',
    'ACTIVE'
);

insert into organization_memberships (id, organization_id, user_id, role, status, primary_membership)
values (
    '40000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000001',
    '30000000-0000-4000-8000-000000000001',
    'SUPERADMIN',
    'ACTIVE',
    true
);
