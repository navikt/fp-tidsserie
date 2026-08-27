package no.nav.fpsak.tidsserie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;


class LocalDateTimelineMaxDatoRegresjonTest {

    private final LocalDate fom1 = LocalDate.of(2024, 1, 1);
    private final LocalDate fom2 = LocalDate.of(2024, 1, 11);

    @Test
    void inner_join_skal_gi_kombinert_segment_når_begge_tidslinjer_er_åpne_til_max() {
        LocalDateTimeline<String> a = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom1, LocalDate.MAX, "A")));
        LocalDateTimeline<String> b = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom2, LocalDate.MAX, "B")));

        LocalDateTimeline<String> resultat = a.combine(b, StandardCombinators::concat, JoinStyle.INNER_JOIN);

        // BUG: resultatet blir tomt i 2.7.5, korrekt skal være ett segment fra fom2 til MAX
        assertThat(resultat.segmenter()).containsExactly(
                new LocalDateSegment<>(fom2, LocalDate.MAX, "AB"));
    }

    @Test
    void right_join_skal_gi_kombinert_segment_når_begge_tidslinjer_er_åpne_til_max() {
        LocalDateTimeline<String> a = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom1, LocalDate.MAX, "A")));
        LocalDateTimeline<String> b = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom2, LocalDate.MAX, "B")));

        LocalDateTimeline<String> resultat = a.combine(b, StandardCombinators::concat, JoinStyle.RIGHT_JOIN);

        // BUG: resultatet blir tomt i 2.7.5
        assertThat(resultat.segmenter()).containsExactly(
                new LocalDateSegment<>(fom2, LocalDate.MAX, "AB"));
    }

    @Test
    void cross_join_skal_beholde_alle_tre_periodene_når_begge_tidslinjer_er_åpne_til_max() {
        LocalDateTimeline<String> a = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom1, LocalDate.MAX, "A")));
        LocalDateTimeline<String> b = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom2, LocalDate.MAX, "B")));

        LocalDateTimeline<String> resultat = a.combine(b, StandardCombinators::concat, JoinStyle.CROSS_JOIN);

        // BUG: siste periode (fom2 -> MAX, verdi AB) forsvinner i 2.7.5 — hele tidslinjen blir
        // liggende igjen som ett udelt "A"-segment fra fom1 til MAX.
        assertThat(resultat.segmenter()).containsExactly(
                new LocalDateSegment<>(fom1, fom2.minusDays(1), "A"),
                new LocalDateSegment<>(fom2, LocalDate.MAX, "AB"));
    }

    @Test
    void intersection_skal_gi_riktig_snitt_når_egen_tidslinje_er_åpen_til_max() {
        LocalDateTimeline<String> a = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom1, LocalDate.MAX, "A")));
        LocalDateTimeline<String> annen = new LocalDateTimeline<>(
                List.of(new LocalDateSegment<>(fom2, fom2.plusDays(9), "B")));

        LocalDateTimeline<String> resultat = a.intersection(annen, StandardCombinators::concat);

        assertThat(resultat.segmenter()).containsExactly(
                new LocalDateSegment<>(fom2, fom2.plusDays(9), "AB"));
    }
}
