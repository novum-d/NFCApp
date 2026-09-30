# NFCApp repository rules

Read this file before changing the repository. This is the repository's project-specific rule source; there is no separate rules document to apply.

## Scope and design

- Improve cohesion by separating responsibilities according to why they change. Keep Android NFC API access at the edge and byte-level parsing independently testable where practical.
- Do not introduce UseCase, Repository, CQRS, Value Object, new interfaces, or new modules merely to follow an architecture pattern. Add a boundary only when the current code has a concrete need for it.
- The business ID format is not specified. Do not infer it from a tag UID, memory offset, or NDEF payload.
- Work in small, reviewable steps. Add characterization tests for existing behavior before moving production code when feasible.

## NFC behavior to preserve during refactoring

- Keep Reader Mode flags, lifecycle start/stop points, callback routing, coroutine dispatcher, and duplicate-read behavior unchanged unless a task explicitly requests a behavior change.
- Keep command bytes, page/block ranges, timeout, retries, connection order, and close behavior unchanged. In particular, NTAG213 Raw `NfcA` access precedes the separate Android `Ndef` access.
- Do not guess or silently alter byte signedness, endianness, text encoding, TLV format, or response/error interpretation.
- If a proposed refactor requires changing observable NFC behavior, stop that change and report the reason first.
- Do not write, lock, or otherwise modify physical NFC tags as part of code refactoring. Keep baseline tags unchanged.

## Verification

- Run relevant JVM tests and a Debug build after code changes. For NFC I/O changes, also compare NFC-A and NFC-V behavior on real tags when a device is available.
- Record what was verified and what still requires a real tag or target tablet. Do not treat a simulated or unit-tested connection as proof of real NFC behavior.
