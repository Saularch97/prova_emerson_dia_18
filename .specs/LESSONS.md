# LESSONS - auto-maintained by scripts/lessons.py

> Machine-owned. Do NOT hand-edit. Changes are overwritten on the next `lessons.py` write.
> Canonical state lives in `.specs/lessons.json`. Edit lessons only via the script.
> promote_threshold=2 distinct features · window_days=45 · quarantine_threshold=2

## Confirmed (load these at Plan/Checks)

Corroborated across multiple features. Safe to apply as guidance.

_none_

## Candidates (under observation - do NOT load as guidance yet)

Seen once or not yet corroborated. Tracked, not trusted.

### L-001 - Test every field's own error message with several invalid fields in one submit, not one bad field at a time
- signal: `weak_test` · recurrence: 1 feature(s) · scope: `web` · harmful: 0
- features: plataforma-cursos
- evidence: verification.md C21,C33 (F1) (web)
- last seen: 2026-11-12T23:59:25Z

### L-002 - Name a requirement ID on a test only when it asserts the full value the criterion fixes, including order, limit and resulting state
- signal: `weak_test` · recurrence: 1 feature(s) · scope: `tests` · harmful: 0
- features: plataforma-cursos
- evidence: verification.md C38,C44 (F3) (tests)
- last seen: 2026-11-12T23:59:25Z

### L-003 - Count text length in characters (code points), not UTF-16 units, wherever the spec states a character limit
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `validation` · harmful: 0
- features: plataforma-cursos
- evidence: verification.md F4 (validation)
- last seen: 2026-11-12T23:59:25Z

### L-004 - Do not give a test a requirement ID when it only proves a value is preserved by code that never touches it
- signal: `weak_test` · recurrence: 1 feature(s) · scope: `tests` · harmful: 0
- features: plataforma-cursos
- evidence: verification.md F5 (tests)
- last seen: 2026-11-12T23:59:25Z

### L-005 - Test each foreign-key relation separately with a record bound to only that relation
- signal: `weak_test` · recurrence: 1 feature(s) · scope: `persistence` · harmful: 0
- features: plataforma-cursos
- evidence: verification.md C63 (F6) (persistence)
- last seen: 2026-11-12T23:59:25Z

### L-006 - Give each validation bound its own message and test both sides of the boundary
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `validation` · harmful: 0
- features: plataforma-cursos
- evidence: verification.md F2 (validation)
- last seen: 2026-11-12T23:59:26Z

### L-007 - Register the JDBC driver explicitly when the app runs in a servlet container; tests with the driver on the classpath do not reproduce a missing driver
- signal: `gate_fail` · recurrence: 1 feature(s) · scope: `docker` · harmful: 0
- features: plataforma-cursos
- evidence: T56 docker run (docker)
- last seen: 2026-11-12T23:59:26Z

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
