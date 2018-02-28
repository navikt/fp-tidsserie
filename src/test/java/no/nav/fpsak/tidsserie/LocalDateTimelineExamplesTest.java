package no.nav.fpsak.tidsserie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;

public class LocalDateTimelineExamplesTest {

    private final LocalDate today = LocalDate.now();

    @Test
    public void skal_slå_sammen_string_verdier() throws Exception {
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
    public void skal_slå_sammen_tall_verdier() throws Exception {
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
