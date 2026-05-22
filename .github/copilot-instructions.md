# fp-tidsserie

Library for periodised data — segments with fom/tom date range and a value,
collected into timelines supporting set operations.

## Context

- [fp-context](https://github.com/navikt/fp-context) — team domain,
  architecture, conventions. Source of truth.
- Consumer view:
  [`architecture/team-libraries.md`](https://github.com/navikt/fp-context/blob/main/architecture/team-libraries.md).
- Copilot Space: navikt / **TeamForeldrepenger**.

## Core types

| Type | Purpose |
|------|---------|
| `LocalDateSegment<V>` | Single fom-tom interval with value |
| `LocalDateTimeline<V>` | Ordered, non-overlapping set of segments |
| `LocalDateInterval` | fom-tom helper |

## Operations

| Category | Examples |
|----------|----------|
| Set ops | union, intersection, disjoint, minus |
| Combine | Combine two timelines with a custom `LocalDateSegmentCombinator` |
| Arithmetic | sum, negate, multiply, divide (per period) |
| Boolean | per-period boolean result |
| Compress | merge adjacent segments with equal values |

Auto-handles knekkpunkter (split/merge segments as ranges change).

LocalDateInterval provides utilites for workdays and weekends - including adjacent friday-monday handling.

## ⚠️ Aggregate values

When a segment's value is an **aggregate tied to interval duration**
(sums, day counts, totals), splits and merges do **not** automatically
rescale. The custom combiner / mapper must recompute the aggregate after
any segmentation change. Be especially careful with `compress`, `combine`,
and `intersection` on such values.

Safe categories:
- Rate-like values (dagsats, sats) — duration-independent
- Boolean / categorical — duration-independent

Risky categories:
- Total amounts, accumulated days, sums, Beløp/Beloep — duration-dependent

## Out of scope

Not for statistical time-series analysis (no moving averages, no
sampling, no large-dataset analytics).

## Release

SemVer; consumed by fp-uttak, fp-kalkulus, fp-sak and others via fp-bom.

## Tech

Java 25, Maven, no external runtime deps beyond JDK.
