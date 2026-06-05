-- Generic account lifecycle notification events.
-- The starter ships with local mock email delivery only.

create table notification_events (
    id uuid primary key,
    organization_id uuid not null,
    workspace_id uuid not null,
    source_type varchar(32) not null,
    source_id uuid not null,
    event_type varchar(64) not null,
    recipient_type varchar(32) not null,
    recipient_id uuid,
    recipient_display_name varchar(160) not null,
    recipient_email varchar(320),
    channel varchar(32) not null,
    provider varchar(64) not null,
    delivery_status varchar(32) not null,
    provider_message_id varchar(160),
    failure_reason varchar(500),
    created_at timestamptz not null default now(),
    sent_at timestamptz,
    updated_at timestamptz not null default now(),
    constraint fk_notification_events_organization foreign key (organization_id) references organizations (id),
    constraint fk_notification_events_workspace foreign key (workspace_id)
        references workspaces (id),
    constraint ck_notification_events_source_type check (source_type in ('USER')),
    constraint ck_notification_events_event_type check (
        event_type in (
            'USER_INVITED',
            'USER_INVITE_RESENT',
            'USER_ACTIVATED',
            'USER_SUSPENDED',
            'USER_REACTIVATED',
            'USER_ARCHIVED',
            'USER_ROLE_CHANGED',
            'PASSWORD_RESET_REQUESTED',
            'PASSWORD_CHANGED',
            'EMAIL_VERIFICATION_REQUESTED',
            'EMAIL_VERIFIED'
        )
    ),
    constraint ck_notification_events_recipient_type check (recipient_type in ('USER')),
    constraint ck_notification_events_channel check (channel in ('EMAIL')),
    constraint ck_notification_events_delivery_status check (
        delivery_status in ('PENDING', 'SENT', 'FAILED', 'SKIPPED')
    ),
    constraint ck_notification_events_sent_at check (
        sent_at is null or delivery_status = 'SENT'
    ),
    constraint ck_notification_events_failure_reason check (
        failure_reason is null or delivery_status = 'FAILED'
    )
);

create index ix_notification_events_organization_status
    on notification_events (organization_id, workspace_id, delivery_status, created_at desc);

create index ix_notification_events_user_lifecycle
    on notification_events (organization_id, workspace_id, source_id, event_type, created_at desc)
    where source_type = 'USER';
