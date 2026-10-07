# Task backup format

Sliding Tasks exports and automatic backups use a UTF-8 JSON document. The
current format version is `1`; the root object contains `formatVersion: 1`.
Dates use ISO 8601 calendar dates (`YYYY-MM-DD`), event timestamps use ISO 8601
instants, and enum values are their uppercase names shown below.

## Version 1 schema

The root object contains:

- `formatVersion`: integer, currently `1`.
- `activeDate`: optional date string; absent when no date is selected.
- `tasks`: array of task objects.
- `cards`: array of task card objects.
- `events`: array of history event objects.

Task objects contain `id`, `title`, `type`, `schedule`, `active`, `createdOn`,
and `resolved`. `type` is `ROUTINE` or `ONE_TIME`. A schedule contains `kind`,
`interval`, `days`, and `startsOn`; `kind` is `ONCE`, `DAILY`,
`WEEKLY_DAYS`, or `ONCE_PER_WEEK`; `days` contains Java `DayOfWeek` names
(`MONDAY` through `SUNDAY`). `interval` is a positive integer. `days` is used
for `WEEKLY_DAYS` and `ONCE_PER_WEEK`; `startsOn` is the first eligible date.

Card objects contain `id`, `taskId`, `boardDate`, `title`, `type`, `status`,
`touches`, and `skipScope`. `status` is `PENDING`, `DONE`, `DISMISSED`, or
`MISSED`; `skipScope` is `TODAY`, `WEEK`, or `TASK`; `touches` is a
non-negative integer.

Event objects contain `id`, `type`, `occurredAt`, `taskId`, optional `cardId`,
and `title`. `cardId` is omitted when the event is not associated with a card.

## Compatibility and evolution

Readers reject unknown `formatVersion` values. A document without
`formatVersion` is treated as a legacy import and may use the historical task
recurrence fields. Readers retain that migration path so existing user files
remain importable.

Do not change the meaning or required fields of version 1 in place. A change
that cannot be read with version 1 semantics must introduce a new format
version. Readers should continue to accept supported earlier versions and
legacy imports when practical; writers emit only the current version.

These files contain private task titles and history. Applications and users
must treat exported and automatic backup files as sensitive data.
