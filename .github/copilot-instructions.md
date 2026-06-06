# fp-tidsserie

Library for periodized data represented as timelines of date segments. Not intended for statistical time-series analysis.

## Shared context

- Source of truth for shared domain, architecture, and conventions: `navikt/fp-context`
- Copilot Space: `navikt/TeamForeldrepenger`
- Consumer view of team libraries: `fp-context/architecture/team-libraries.md`

## Core types

| Type | Purpose                                                       |
|---|---------------------------------------------------------------|
| `LocalDateSegment<V>` | Single fom-tom interval with a value                          |
| `LocalDateTimeline<V>` | Ordered, non-overlapping set of segments                      |
| `LocalDateInterval` | fom-tom helper; utils for workdays, weekends, adjacency/abuts |

## Operations

| Category       | Examples |
|----------------|----------|
| Set operations | union, intersection, disjoint, minus |
| Combine        | Merge two timelines |
| Arithmetic     | sum, negate, multiply, divide per period |
| Boolean        | per-period boolean result |
| Compress       | merge adjacent segments with equal values |

Creating and operating on `LocalDateTimeline<V>` may split segments. Some common options
- `LocalDateSegmentCombinator<T, V, R>` - method for handling time-overlapping segments 
- `SegmentSplitter<V>` - custom splitting of segments during operations, needed to rescale duration-dependent values to new interval 
- `JoinStyle` - how to combine timelines using predicates on presence of segments in the timelines

Recommend using combinators from the collection `StandardCombinators` over local implementations of `LocalDateSegmentCombinator<T, V, R>`.

Method `compress` merges adjacent and equal segments, and can be called with custom adjacency and equality predicates, and a custom merge-method to combine duration-dependent values

## Aggregate or duration-dependent values

When a segment value is an aggregate tied to interval duration, splits and merges do not automatically rescale the value. Custom combiners or mappers must recompute the aggregate after segmentation changes.
- Safe categories: rate-like values such as dagsats, boolean, enums or categorical values
- Risky categories: total amounts and sums, accumulated days

## Release and use

SemVer release; version not included in `fp-bom`; imported directly by many repos in the foreldrepenger ecosystem
