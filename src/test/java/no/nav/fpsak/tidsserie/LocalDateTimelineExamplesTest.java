package no.nav.fpsak.tidsserie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;

public class LocalDateTimelineExamplesTest {

    private final LocalDate today = LocalDate.now();

    @Test
    public void eksempel_slå_sammen_string_verdier() throws Exception {
        // bruker tall til å referer relative dager til today

        LocalDateTimeline<String> timelineA = toTimeline(new Object[][] {
                { 0, 5, "A" },
                { 7, 9, "A" }
        });

        LocalDateTimeline<String> timelineB = toTimeline(new Object[][] {
                { 3, 6, "B" }
        });

        // intersection (inner join): A ∩ B
        assertThat(timelineA.intersection(timelineB, StandardCombinators::concat)).isEqualTo(toTimeline(new Object[][] {
                { 3, 5, "AB" },
        }));

        // cartesian product (cross join): A ∪ B
        assertThat(timelineA.combine(timelineB, StandardCombinators::concat, JoinStyle.CROSS_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, "A" },
                { 3, 5, "AB" },
                { 6, 6, "B" },
                { 7, 9, "A" },
        }));

        // cartesian product (cross join) med Alle Verdier: A ∪ B
        assertThat(timelineA.combine(timelineB, StandardCombinators::bothValues, JoinStyle.CROSS_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, Arrays.asList("A") },
                { 3, 5, Arrays.asList("A", "B") },
                { 6, 6, Arrays.asList("B") },
                { 7, 9, Arrays.asList("A") },
        }));

        // relative complement (disjoint): A - B
        assertThat(timelineA.combine(timelineB, StandardCombinators::concat, JoinStyle.DISJOINT)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, "A" },
                { 7, 9, "A" },
        }));

        // relative complement (disjoint): B - A
        assertThat(timelineB.combine(timelineA, StandardCombinators::concat, JoinStyle.DISJOINT)).isEqualTo(toTimeline(new Object[][] {
                { 6, 6, "B" },
        }));

        // (left join): All objects belonging to A, including intersection with B, but not non-intersecting B
        assertThat(timelineA.combine(timelineB, StandardCombinators::concat, JoinStyle.LEFT_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, "A" },
                { 3, 5, "AB" },
                { 7, 9, "A" },
        }));

        // motsatt av left join:
        // (right join): All objects belonging to B, including intersection with A, but not non-intersecting A
        assertThat(timelineA.combine(timelineB, StandardCombinators::concat, JoinStyle.RIGHT_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 3, 5, "AB" },
                { 6, 6, "B" },
        }));
    }

    @Test
    public void eksempel_slå_sammen_begge_verdier() throws Exception {
        // bruker tall til å referer relative dager til today

        LocalDateTimeline<String> timelineA = toTimeline(new Object[][] {
                { 0, 5, "A" },
                { 7, 9, "A" }
        });

        LocalDateTimeline<String> timelineB = toTimeline(new Object[][] {
                { 3, 6, "B" }
        });

        // intersection (inner join): A ∩ B
        assertThat(timelineA.intersection(timelineB, StandardCombinators::bothValues)).isEqualTo(toTimeline(new Object[][] {
                { 3, 5, List.of("A", "B") },
        }));

        // cartesian product (cross join) med Alle Verdier: A ∪ B
        assertThat(timelineA.combine(timelineB, StandardCombinators::bothValues, JoinStyle.CROSS_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, Arrays.asList("A") },
                { 3, 5, Arrays.asList("A", "B") },
                { 6, 6, Arrays.asList("B") },
                { 7, 9, Arrays.asList("A") },
        }));

        // relative complement (disjoint): A - B
        assertThat(timelineA.combine(timelineB, StandardCombinators::bothValues, JoinStyle.DISJOINT)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, List.of("A") },
                { 7, 9, List.of("A") },
        }));

        // relative complement (disjoint): B - A
        assertThat(timelineB.combine(timelineA, StandardCombinators::bothValues, JoinStyle.DISJOINT)).isEqualTo(toTimeline(new Object[][] {
                { 6, 6, List.of("B") },
        }));

        // (left join): All objects belonging to A, including intersection with B, but not non-intersecting B
        assertThat(timelineA.combine(timelineB, StandardCombinators::bothValues, JoinStyle.LEFT_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, List.of("A") },
                { 3, 5, List.of("A", "B") },
                { 7, 9, List.of("A") },
        }));

        // motsatt av left join:
        // (right join): All objects belonging to B, including intersection with A, but not non-intersecting A
        assertThat(timelineA.combine(timelineB, StandardCombinators::bothValues, JoinStyle.RIGHT_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 3, 5, List.of("A", "B") },
                { 6, 6, List.of("B") },
        }));
    }

    @Test
    public void eksempel_slå_sammen_tall_verdier() throws Exception {
        // bruker tall til å referer relative dager til today

        double A = 15d;
        double B = 20d;
        double AB = A + B;

        LocalDateTimeline<Double> timelineA = toTimeline(new Object[][] {
                { 0, 5, A },
                { 7, 9, A }
        });

        LocalDateTimeline<Double> timelineB = toTimeline(new Object[][] {
                { 3, 6, B }
        });

        // intersection (inner join): A ∩ B
        assertThat(timelineA.intersection(timelineB, StandardCombinators::sum)).isEqualTo(toTimeline(new Object[][] {
                { 3, 5, AB },
        }));

        // cartesian product (cross join): A ∪ B
        LocalDateTimeline<Double> timelineAXB = timelineA.combine(timelineB, StandardCombinators::sum, JoinStyle.CROSS_JOIN);
        assertThat(timelineAXB).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, A },
                { 3, 5, AB },
                { 6, 6, B },
                { 7, 9, A },
        }));

        // relative complement (disjoint): A - B
        assertThat(timelineA.combine(timelineB, StandardCombinators::sum, JoinStyle.DISJOINT)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, A },
                { 7, 9, A },
        }));

        // relative complement (disjoint): B - A
        assertThat(timelineB.combine(timelineA, StandardCombinators::sum, JoinStyle.DISJOINT)).isEqualTo(toTimeline(new Object[][] {
                { 6, 6, B },
        }));

        // (left join): All objects belonging to A, including intersection with B, but not non-intersecting B
        assertThat(timelineA.combine(timelineB, StandardCombinators::sum, JoinStyle.LEFT_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, A },
                { 3, 5, AB },
                { 7, 9, A },
        }));

        // motsatt av left join:
        // (right join): All objects belonging to B, including intersection with A, but not non-intersecting A
        assertThat(timelineA.combine(timelineB, StandardCombinators::sum, JoinStyle.RIGHT_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 3, 5, AB },
                { 6, 6, B },
        }));
    }

    @Test
    public void eksempel_splitt_av_tidsserie_ved_bruk_av_kvoter() throws Exception {

        class Kvoter<V> implements Function<LocalDateSegment<V>, List<LocalDateSegment<V>>> {
            // Enkle kvoter knyttet til verdi i segmentet.
            private final Map<V, Long> kvoter = new HashMap<>();

            public Kvoter(Map<V, Long> kvoter) {
                this.kvoter.putAll(kvoter);
            }

            @Override
            public List<LocalDateSegment<V>> apply(LocalDateSegment<V> seg) {
                List<LocalDateSegment<V>> result;

                Long kvote = kvoter.get(seg.getValue());
                if (kvote != null && !Objects.equals(kvote, 0L)) {
                    LocalDateInterval intervall = seg.getLocalDateInterval();
                    long nyKvote = Math.max(0, kvote - intervall.days());
                    result = Arrays.asList(new LocalDateSegment<>(seg.getFom(), seg.getFom().plusDays(kvote - nyKvote - 1), seg.getValue()));
                    kvoter.put(seg.getValue(), nyKvote);
                } else {
                    // vi er strenge her, har vi ingen kvote eller kvote==0 får du ingenting tilbake.
                    result = Collections.emptyList();
                }
                return result;
            }

        }

        LocalDateTimeline<String> timeline = toTimeline(new Object[][] {
                { 0, 5, "A" }, // (har ikke A kvoter)
                { 6, 6, "B" }, // (B bruker 1 dag av kvoten)
                { 7, 9, "A" }, // (har ikke A kvoter)
                { 15, 20, "B" }, // (B bruker 2 dager, kvoten deretter tom)
                { 30, 40, "B" } // (B har ingen kvoter igjen)
        });

        Map<String, Long> kvoter = new HashMap<>();
        kvoter.put("A", 0L);
        kvoter.put("B", 3L);
        LocalDateTimeline<String> mappedTimeline = timeline.map(new Kvoter<>(kvoter));

        assertThat(mappedTimeline).isEqualTo(toTimeline(new Object[][] {
                { 6, 6, "B" }, // B bruker 1 dager
                { 15, 16, "B" } // B bruker 2 dager
        }));
    }

    // convenience metode for å opprette en tidsserie enkelt
    @SuppressWarnings("unchecked")
    private <V> LocalDateTimeline<V> toTimeline(Object[][] data) {
        List<LocalDateSegment<V>> segments = Arrays.stream(data).map(arr -> {
            return new LocalDateSegment<>(today.plusDays((int) arr[0]), today.plusDays((int) arr[1]), (V) arr[2]);
        })
            .collect(Collectors.toList());

        return new LocalDateTimeline<>(segments);
    }
}
