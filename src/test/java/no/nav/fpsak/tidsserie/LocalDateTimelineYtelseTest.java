package no.nav.fpsak.tidsserie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;

public class LocalDateTimelineYtelseTest {

    private final LocalDate today = LocalDate.now();

    @Test
    public void lange_tidslinjer() throws Exception {
        // bruker tall til å referer relative dager til today

        for (int i = 0; i < 3000; i++) {
            int antall = 1000;
            int antallDagerPrIntervall = 5;
            int antallDagerMellomIntervall = 2;

            LocalDateTimeline<String> timelineA = lagTidslinje(today, antallDagerPrIntervall, antallDagerMellomIntervall, antall, "A");
            LocalDateTimeline<String> timelineB = lagTidslinje(today, antallDagerPrIntervall, antallDagerMellomIntervall, antall, "B");
            LocalDateTimeline<String> timelineAB = lagTidslinje(today, antallDagerPrIntervall, antallDagerMellomIntervall, antall, "AB");

            LocalDateTimeline<String> timelineCrossJoined = timelineA.combine(timelineB, StandardCombinators::concat, JoinStyle.CROSS_JOIN);

            assertThat(timelineCrossJoined).isEqualTo(timelineAB);

        }
    }

    @Test
    public void korte_tidslinjer() throws Exception {
        // bruker tall til å referer relative dager til today

        for (int i = 0; i < 200000; i++) {

            int antall = 10;
            int antallDagerPrIntervall = 5;
            int antallDagerMellomIntervall = 2;

            LocalDateTimeline<String> timelineA = lagTidslinje(today, antallDagerPrIntervall, antallDagerMellomIntervall, antall, "A");
            LocalDateTimeline<String> timelineB = lagTidslinje(today, antallDagerPrIntervall, antallDagerMellomIntervall, antall, "B");
            LocalDateTimeline<String> timelineAB = lagTidslinje(today, antallDagerPrIntervall, antallDagerMellomIntervall, antall, "AB");

            LocalDateTimeline<String> timelineCrossJoined = timelineA.combine(timelineB, StandardCombinators::concat, JoinStyle.CROSS_JOIN);

            assertThat(timelineCrossJoined).isEqualTo(timelineAB);
        }
    }

    private static <T> LocalDateTimeline<T> lagTidslinje(LocalDate startdato, int dagerPerIntervall, int dagerMellomIntervall, int antall, T verdi) {
        LocalDate fom = startdato;
        List<LocalDateSegment<T>> segmenter = new ArrayList<>();
        for (int i = 0; i < antall; i++) {
            var tom = fom.plusDays(dagerPerIntervall - 1);
            segmenter.add(new LocalDateSegment<>(fom, tom, verdi));
            fom = tom.plusDays(dagerMellomIntervall + 1);
        }
        return new LocalDateTimeline<>(segmenter);
    }

}
