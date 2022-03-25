package no.nav.fpsak.tidsserie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;

class LocalDateTimelineSammenlignMotGammelImplementasjonTest {

    private final LocalDate today = LocalDate.now();

    @Test
    void random_ulike_tidslinjer() {
        //tester stort antall tidslinjer for gammel vs ny implementasjon for å verifisere likhet
        Random random = new Random(1523465520807167939L);

        doTest(random, 100, StandardCombinators::concat, JoinStyle.CROSS_JOIN);
        doTest(random, 100, StandardCombinators::concat, JoinStyle.LEFT_JOIN);
        doTest(random, 100, StandardCombinators::concat, JoinStyle.RIGHT_JOIN);
        doTest(random, 100, StandardCombinators::concat, JoinStyle.DISJOINT);
        doTest(random, 100, StandardCombinators::concat, JoinStyle.INNER_JOIN);
    }

    private void doTest(Random random, int antall, LocalDateSegmentCombinator<String, String, String> combinator, JoinStyle combinationStyle) {
        for (int i = 1; i < antall; i++) {
            LocalDateTimeline<String> timelineA = randomTidslinje(random, i, 17, 7);
            LocalDateTimeline<String> timelineB = randomTidslinje(random, i, 17, 7);

            LocalDateTimeline<String> timelineAB1 = timelineA.combine(timelineB, combinator, combinationStyle);
            LocalDateTimeline<String> timelineAB2 = timelineA.combineGammel(timelineB, combinator, combinationStyle);

            assertThat(timelineAB1).isEqualTo(timelineAB2);
        }
    }


    private LocalDateTimeline<String> randomTidslinje(Random random, int lengde, int maxIntervallLengdeDager, int maxMellomromLengdeDager) {

        List<LocalDateSegment<String>> resultat = new ArrayList<>(lengde);

        LocalDate dato = today;
        for (int i = 0; i < lengde; i++) {
            int intervallMellomromDager = random.nextInt(maxMellomromLengdeDager - 1) + 1;
            dato = dato.plusDays(intervallMellomromDager);
            int intervallDager = random.nextInt(maxIntervallLengdeDager - 1) + 1;
            String verdi = Integer.toString(random.nextInt(256), 16);
            resultat.add(new LocalDateSegment<>(dato, dato.plusDays(intervallDager - 1), verdi));
            dato = dato.plusDays(intervallDager);
        }
        return new LocalDateTimeline<>(resultat);
    }

}
