package no.nav.fpsak.tidsserie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
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
                { 3, 5, List.of("A", "B") }
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
    public void eksempel_slå_sammen_med_alle_verdier() throws Exception {
        // bruker tall til å referer relative dager til today

        LocalDateTimeline<List<String>> timelineA = toTimeline(new Object[][] {
                { 0, 5, List.of("A") },
                { 7, 9, List.of("A") }
        });

        LocalDateTimeline<String> timelineB = toTimeline(new Object[][] {
                { 3, 6, "B" }
        });

        // intersection (inner join): A ∩ B
        assertThat(timelineA.intersection(timelineB, StandardCombinators::allValues)).isEqualTo(toTimeline(new Object[][] {
                { 3, 5, List.of("A", "B") },
        }));

        // cartesian product (cross join) med Alle Verdier: A ∪ B
        assertThat(timelineA.combine(timelineB, StandardCombinators::allValues, JoinStyle.CROSS_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, Arrays.asList("A") },
                { 3, 5, Arrays.asList("A", "B") },
                { 6, 6, Arrays.asList("B") },
                { 7, 9, Arrays.asList("A") },
        }));

        // relative complement (disjoint): A - B
        assertThat(timelineA.combine(timelineB, StandardCombinators::allValues, JoinStyle.DISJOINT)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, List.of("A") },
                { 7, 9, List.of("A") },
        }));

        // relative complement (disjoint): B - A
        // -- GÅR IKKE HER DA A OG B HAR FORSKJELLIG TYPE --

        // (left join): All objects belonging to A, including intersection with B, but not non-intersecting B
        assertThat(timelineA.combine(timelineB, StandardCombinators::allValues, JoinStyle.LEFT_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, List.of("A") },
                { 3, 5, List.of("A", "B") },
                { 7, 9, List.of("A") },
        }));

        // motsatt av left join:
        // (right join): All objects belonging to B, including intersection with A, but not non-intersecting A
        assertThat(timelineA.combine(timelineB, StandardCombinators::allValues, JoinStyle.RIGHT_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 3, 5, List.of("A", "B") },
                { 6, 6, List.of("B") },
        }));
    }

    @Test
    public void eksempel_kun_venstre_side() throws Exception {
        // bruker tall til å referer relative dager til today

        LocalDateTimeline<List<String>> timelineA = toTimeline(new Object[][] {
                { 0, 5, List.of("A") },
                { 7, 9, List.of("A") }
        });

        LocalDateTimeline<String> timelineB = toTimeline(new Object[][] {
                { 3, 6, "B" }
        });

        // intersection (inner join): A ∩ B
        assertThat(timelineA.intersection(timelineB, StandardCombinators::leftOnly)).isEqualTo(toTimeline(new Object[][] {
                { 3, 5, List.of("A") },
        }));
    }

    @Test
    public void eksempel_kun_høyre_side() throws Exception {
        // bruker tall til å referer relative dager til today

        LocalDateTimeline<List<String>> timelineA = toTimeline(new Object[][] {
                { 0, 5, List.of("A") },
                { 7, 9, List.of("A") }
        });

        LocalDateTimeline<String> timelineB = toTimeline(new Object[][] {
                { 3, 6, "B" }
        });

        // intersection (inner join): A ∩ B
        assertThat(timelineA.intersection(timelineB, StandardCombinators::rightOnly)).isEqualTo(toTimeline(new Object[][] {
                { 3, 5, "B" },
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
    public void eksempel_multipliser_sammen_tall_verdier() throws Exception {
        // bruker tall til å referer relative dager til today

        double A = 15d;
        double B = 20d;
        double AB = A * B;

        LocalDateTimeline<Double> timelineA = toTimeline(new Object[][] {
                { 0, 5, A },
                { 7, 9, A }
        });

        LocalDateTimeline<Double> timelineB = toTimeline(new Object[][] {
                { 3, 6, B }
        });

        // intersection (inner join): A ∩ B
        assertThat(timelineA.intersection(timelineB, StandardCombinators::product)).isEqualTo(toTimeline(new Object[][] {
                { 3, 5, AB },
        }));

        // cartesian product (cross join): A ∪ B
        LocalDateTimeline<Double> timelineAXB = timelineA.combine(timelineB, StandardCombinators::product, JoinStyle.CROSS_JOIN);
        assertThat(timelineAXB).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, 0d },
                { 3, 5, AB },
                { 6, 6, 0d },
                { 7, 9, 0d },
        }));

        // relative complement (disjoint): A - B
        assertThat(timelineA.combine(timelineB, StandardCombinators::product, JoinStyle.DISJOINT)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, 0d },
                { 7, 9, 0d },
        }));

        // relative complement (disjoint): B - A
        assertThat(timelineB.combine(timelineA, StandardCombinators::product, JoinStyle.DISJOINT)).isEqualTo(toTimeline(new Object[][] {
                { 6, 6, 0d },
        }));

        // (left join): All objects belonging to A, including intersection with B, but not non-intersecting B
        assertThat(timelineA.combine(timelineB, StandardCombinators::product, JoinStyle.LEFT_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 0, 2, 0d },
                { 3, 5, AB },
                { 7, 9, 0d },
        }));

        // motsatt av left join:
        // (right join): All objects belonging to B, including intersection with A, but not non-intersecting A
        assertThat(timelineA.combine(timelineB, StandardCombinators::product, JoinStyle.RIGHT_JOIN)).isEqualTo(toTimeline(new Object[][] {
                { 3, 5, AB },
                { 6, 6, 0d },
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

    @Test
    public void eksempel_splitt_av_tidsserie_ved_period_year() throws Exception {

        var timeline = new LocalDateTimeline<>(
            List.of(
                toSegment("2019-12-01", "2020-12-31", "A"),
                toSegment("2017-01-01", "2017-12-31", "B")));

        LocalDate startDate = LocalDate.parse("2016-01-01");
        LocalDate endDate = timeline.getMaxLocalDate();
        var mappedTimeline = timeline.splitAtRegular(startDate, endDate, Period.ofYears(1));

        var expectedTimeline = new LocalDateTimeline<>(
            List.of(
                toSegment("2019-12-01", "2019-12-31", "A"),
                toSegment("2020-01-01", "2020-12-31", "A"),
                toSegment("2017-01-01", "2017-12-31", "B")));

        assertThat(mappedTimeline).isEqualTo(expectedTimeline);
    }
    
    @Test
    public void eksempel_splitt_av_tidsserie_ved_period_day_3() throws Exception {

        var timeline = new LocalDateTimeline<>(
            List.of(
                toSegment("2019-12-01", "2020-01-02", "A")));

        LocalDate startDate = LocalDate.parse("2019-12-01");
        LocalDate endDate = timeline.getMaxLocalDate().plusDays(1); // ta med litt ekstra
        var mappedTimeline = timeline.splitAtRegular(startDate, endDate, Period.ofDays(3));

        var expectedTimeline = new LocalDateTimeline<>(
            List.of(
                toSegment("2019-12-01", "2019-12-03", "A"),
                toSegment("2019-12-04", "2019-12-06", "A"),
                toSegment("2019-12-07", "2019-12-09", "A"),
                toSegment("2019-12-10", "2019-12-12", "A"),
                toSegment("2019-12-13", "2019-12-15", "A"),
                toSegment("2019-12-16", "2019-12-18", "A"),
                toSegment("2019-12-19", "2019-12-21", "A"),
                toSegment("2019-12-22", "2019-12-24", "A"),
                toSegment("2019-12-25", "2019-12-27", "A"),
                toSegment("2019-12-28", "2019-12-30", "A"),
                toSegment("2019-12-31", "2020-01-02", "A")));

        assertThat(mappedTimeline).isEqualTo(expectedTimeline);
    }
    
    @Test
    public void eksempel_splitt_av_tidsserie_ved_period_week_1() throws Exception {

        var timeline = new LocalDateTimeline<>(
            List.of(
                toSegment("2019-12-01", "2020-01-02", "A")));

        LocalDate startDate = LocalDate.parse("2019-12-01");
        LocalDate endDate = timeline.getMaxLocalDate();
        var mappedTimeline = timeline.splitAtRegular(startDate, endDate, Period.ofWeeks(1));

        var expectedTimeline = new LocalDateTimeline<>(
            List.of(
                toSegment("2019-12-01", "2019-12-07", "A"),
                toSegment("2019-12-08", "2019-12-14", "A"),
                toSegment("2019-12-15", "2019-12-21", "A"),
                toSegment("2019-12-22", "2019-12-28", "A"),
                toSegment("2019-12-29", "2020-01-02", "A")));

        assertThat(mappedTimeline).isEqualTo(expectedTimeline);
    }


    private static <V> LocalDateSegment<V> toSegment(String dt1, String dt2, V val) {
        return new LocalDateSegment<>(LocalDate.parse(dt1), LocalDate.parse(dt2), val);
    }
}
