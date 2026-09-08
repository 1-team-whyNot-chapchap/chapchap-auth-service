# Kafka topic contract

Auth uses the `msa4-team1.` team prefix, aligned with the Subscription and Delivery contracts as of 2026-09-08.

| Direction | Topic | Dead-letter topic |
|---|---|---|
| Publish user events | `msa4-team1.auth.user-events.v1` | `msa4-team1.auth.user-events.v1.DLT` |
| Consume default address events | `msa4-team1.subscription.address-events.v1` | `msa4-team1.subscription.address-events.v1.DLT` |
| Consume subscription status events | `msa4-team1.subscription.subscription-events.v1` | `msa4-team1.subscription.subscription-events.v1.DLT` |

Values are configured under `app.kafka.topics` in `src/main/resources/application.yaml`. Consumer failures retain the existing retry policy and use the original topic plus `.DLT`. The user-event DLT name is a contract setting; this change does not add producer delivery recovery.

Before deployment, provision/verify the new topics and ACLs, align producer/consumer configuration overrides, and plan how to drain the old topics and select initial offsets for the new ones. Renaming configuration does not migrate existing messages or consumer offsets. Consumer groups, message keys, envelopes and payloads are unchanged. No dual publishing or automatic replay is introduced.

`ADMIN_ACCOUNT_ENABLED` and the Delivery administrator payload alignment remain a separate change. Local configuration and producer tests verify routing names; they do not prove connectivity to a live Kafka cluster.
