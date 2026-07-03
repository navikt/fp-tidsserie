package no.nav.fpsak.tidsserie;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

public class KnekkpunktIteratorTest {

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
    void skal_fungere_når_begge_tidslinjer_slutter_på_LocaldateMax() {
        NavigableSet<LocalDate> fomDatoer = new TreeSet<>(Set.of(LocalDate.of(2022, 12, 26), LocalDate.of(2022, 12, 29)));
        NavigableSet<LocalDate> tomDatoer = new TreeSet<>(Set.of(LocalDate.MAX));

        LocalDateTimeline.KnekkpunktIterator iterator = new LocalDateTimeline.KnekkpunktIterator(fomDatoer, tomDatoer);
        Assertions.assertThat(iterator.hasNext()).isTrue();
        Assertions.assertThat(iterator.next()).isEqualTo(LocalDate.of(2022, 12, 26));
        Assertions.assertThat(iterator.hasNext()).isTrue();
        Assertions.assertThat(iterator.next()).isEqualTo(LocalDate.of(2022, 12, 29));
        Assertions.assertThat(iterator.hasNext()).isTrue();
        Assertions.assertThat(iterator.next()).isNull(); //null representerer at neste startpunkt er LocalDate.MAX+1
        Assertions.assertThat(iterator.hasNext()).isFalse();

        Assertions.assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class).hasMessageContaining("Ikke flere verdier igjen");
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

}
