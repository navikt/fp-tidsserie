package no.nav.fpsak.tidsserie;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

public class KnekkpunktIteratorTest {

    LocalDate today = LocalDate.now();

    @Test
    void skal_ha_knekkpunkt_på_start_og_dagen_etter_slutt() {
        NavigableSet<LocalDate> fomDatoer = new TreeSet<>(Set.of(LocalDate.of(2022, 12, 26), LocalDate.of(2022, 12, 29)));
        NavigableSet<LocalDate> tomDatoer = new TreeSet<>(Set.of(LocalDate.of(2022, 12, 28), LocalDate.of(2022, 12, 31)));

        LocalDateTimeline.KnekkpunktIterator iterator = new LocalDateTimeline.KnekkpunktIterator(fomDatoer, tomDatoer);
        Assertions.assertThat(iterator.hasNext()).isTrue();
        Assertions.assertThat(iterator.next()).isEqualTo(LocalDate.of(2022, 12, 26));
        Assertions.assertThat(iterator.hasNext()).isTrue();
        Assertions.assertThat(iterator.next()).isEqualTo(LocalDate.of(2022, 12, 29));
        Assertions.assertThat(iterator.hasNext()).isTrue();
        Assertions.assertThat(iterator.next()).isEqualTo(LocalDate.of(2023, 1, 1));
        Assertions.assertThat(iterator.hasNext()).isFalse();

    }

    @Test
    void startpunktItertatorTest() {
        LocalDate start1 = LocalDate.of(2022, 12, 27);
        LocalDate slutt1 = LocalDate.of(2022, 12, 29);
        LocalDate start2 = LocalDate.of(2023, 1, 2);
        LocalDate slutt2 = LocalDate.of(2023, 1, 4);
        List<LocalDateSegment<String>> segmenter = List.of(
                new LocalDateSegment<>(start1, slutt1, "x"),
                new LocalDateSegment<>(start2, slutt2, "x")
        );
        var iterator = new LocalDateTimeline.StartdatoIterator<>(segmenter, List.of());
        Assertions.assertThat(iterator.hasNext()).isTrue();
        Assertions.assertThat(iterator.next()).isEqualTo(start1);
        Assertions.assertThat(iterator.hasNext()).isTrue();
        Assertions.assertThat(iterator.next()).isEqualTo(start2);
        Assertions.assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void startpunktItertator2Test() {
        LocalDate start1 = LocalDate.of(2022, 1, 1);
        LocalDate slutt1 = LocalDate.of(2022, 2, 1);
        LocalDate start2 = LocalDate.of(2022, 1, 2);
        LocalDate slutt2 = LocalDate.of(2022, 2, 2);
        List<LocalDateSegment<String>> segmenter1 = List.of(new LocalDateSegment<>(start1, slutt1, "x"));
        List<LocalDateSegment<String>> segmenter2 = List.of(new LocalDateSegment<>(start2, slutt2, "x"));
        var iterator = new LocalDateTimeline.StartdatoIterator<>(segmenter1, segmenter2);
        Assertions.assertThat(iterator.hasNext()).isTrue();
        Assertions.assertThat(iterator.next()).isEqualTo(start1);
        Assertions.assertThat(iterator.hasNext()).isTrue();
        Assertions.assertThat(iterator.next()).isEqualTo(start2);
        Assertions.assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void name() {
        LocalDateTimeline<String> timeline = basicDiscontinuousTimeline();
        LocalDateTimeline<String> annenTimeline = new LocalDateTimeline<>(today.plusDays(1), today.plusDays(5), "bye");

        var startdatoIterator = new LocalDateTimeline.StartdatoIterator<>(timeline.segmenter(), annenTimeline.segmenter());
        var sluttdatoIterator = new LocalDateTimeline.SluttdatoIterator<>(timeline.segmenter(), annenTimeline.segmenter());

        LocalDateTimeline.KnekkpunktIterator knekkpunktIterator = new LocalDateTimeline.KnekkpunktIterator(startdatoIterator, sluttdatoIterator);

        for (LocalDateSegment<String> segment : timeline.segmenter()) {
            System.out.println("segment: " + segment);
        }
        while (knekkpunktIterator.hasNext()) {
            System.out.println(knekkpunktIterator.next());
        }


    }

    @Test
    void name2() {
        LocalDateTimeline<String> timeline1 = new LocalDateTimeline<>(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 2, 1), "s");
        LocalDateTimeline<String> timeline2 = new LocalDateTimeline<>(LocalDate.of(2022, 1, 2), LocalDate.of(2022, 2, 2), "s");

        var startdatoIterator = new LocalDateTimeline.StartdatoIterator<>(timeline1.segmenter(), timeline2.segmenter());
        var sluttdatoIterator = new LocalDateTimeline.SluttdatoIterator<>(timeline1.segmenter(), timeline2.segmenter());

        while (startdatoIterator.hasNext()) {
            System.out.println("startdato: " + startdatoIterator.next());
        }
        startdatoIterator = new LocalDateTimeline.StartdatoIterator<>(timeline1.segmenter(), timeline2.segmenter());
        LocalDateTimeline.KnekkpunktIterator knekkpunktIterator = new LocalDateTimeline.KnekkpunktIterator(startdatoIterator, sluttdatoIterator);

        for (LocalDateSegment<String> segment : timeline1.segmenter()) {
            System.out.println("tidsline1 segment: " + segment);
        }
        for (LocalDateSegment<String> segment : timeline2.segmenter()) {
            System.out.println("tidsline2 segment: " + segment);
        }
        while (knekkpunktIterator.hasNext()) {
            System.out.println(knekkpunktIterator.next());
        }


    }

    private LocalDateTimeline<String> basicDiscontinuousTimeline() {
        LocalDate d1 = today;
        LocalDate d2 = d1.plusDays(2);

        LocalDateSegment<String> ds1 = new LocalDateSegment<>(d1, d2, "hello");
        LocalDateSegment<String> ds2 = new LocalDateSegment<>(d2.plusDays(4), d2.plusDays(6), "world");

        LocalDateTimeline<String> tidslinje = new LocalDateTimeline<>(Arrays.asList(ds1, ds2));
        return tidslinje;
    }
}
