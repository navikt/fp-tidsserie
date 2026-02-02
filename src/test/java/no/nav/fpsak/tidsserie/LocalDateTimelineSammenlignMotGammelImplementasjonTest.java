package no.nav.fpsak.tidsserie;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.gammel.GammelLocalDateTimeline;

/**
 * Intensjonen med testen er å teste mange ulike kombinasjoner av tidslinjer for å verifisere at ny implementasjon ikke har endret oppførsel
 */
class LocalDateTimelineSammenlignMotGammelImplementasjonTest {

    private final LocalDate today = LocalDate.now();

    @Test
    void oppførsel_når_overlapp_ikke_tillates() {
        List<LocalDateSegment<Boolean>> segmenter = List.of(
                new LocalDateSegment<>(today, today.plusDays(10), true),
                new LocalDateSegment<>(today.plusDays(10), today.plusDays(11), false));


        assertThatThrownBy(() -> new GammelLocalDateTimeline<>(segmenter), "forventet exception").isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Overlapp");
        assertThatThrownBy(() -> new LocalDateTimeline<>(segmenter), "forventet exception").isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Overlapp");
    }

    @Test
    void random_ulike_tidslinjer() {
        //tester stort antall tidslinjer for gammel vs ny implementasjon for å verifisere likhet
        Random random = new Random(1523465520807167939L); //hardkodet seed for å ha stabil test

        doTest(random, 100, StandardCombinators::concat, JoinStyle.CROSS_JOIN);
        doTest(random, 100, StandardCombinators::concat, JoinStyle.LEFT_JOIN);
        doTest(random, 100, StandardCombinators::concat, JoinStyle.RIGHT_JOIN);
        doTest(random, 100, StandardCombinators::concat, JoinStyle.DISJOINT);
        doTest(random, 100, StandardCombinators::concat, JoinStyle.INNER_JOIN);
    }

    private void doTest(Random random, int antall, LocalDateSegmentCombinator<String, String, String> combinator, JoinStyle combinationStyle) {
        for (int i = 1; i < antall; i++) {
            List<LocalDateSegment<String>> segmenterA = randomSegmenter(random, i, 17, 7);
            List<LocalDateSegment<String>> segmenterB = randomSegmenter(random, i, 17, 7);

            LocalDateTimeline<String> timelineA = new LocalDateTimeline<>(segmenterA);
            LocalDateTimeline<String> timelineB = new LocalDateTimeline<>(segmenterB);

            GammelLocalDateTimeline<String> gammelTimelineA = new GammelLocalDateTimeline<>(segmenterA);
            GammelLocalDateTimeline<String> gammelTimelineB = new GammelLocalDateTimeline<>(segmenterB);

            LocalDateTimeline<String> timelineAB = timelineA.combine(timelineB, combinator, combinationStyle);
            GammelLocalDateTimeline<String> gammelTimelineAB = gammelTimelineA.combine(gammelTimelineB, combinator, mapJoinStyle(combinationStyle));

            assertLike(timelineAB.toSegments(), gammelTimelineAB.toSegments());
        }
    }

    @Test
    void test_ekstrem_overlapp() {
        int antall = 2500;
        int antallDagerPrIntervall = 2000;

        List<LocalDateSegment<String>> segmenter = new ArrayList<>();
        for (int j = 0; j < antall; j++) {
            LocalDate fom = today.plusDays(j);
            segmenter.add(new LocalDateSegment<>(fom, fom.plusDays(antallDagerPrIntervall), "A"));
        }

        LocalDateTimeline<String> ny = new LocalDateTimeline<>(segmenter, StandardCombinators::coalesceRightHandSide);
        GammelLocalDateTimeline<String> gammel = new GammelLocalDateTimeline<String>(segmenter, StandardCombinators::coalesceRightHandSide);
        assertLike(ny.segmenter(), gammel.toSegments());
    }

    private static <T> void assertLike(Collection<LocalDateSegment<T>> a, Collection<LocalDateSegment<T>> b) {
        Iterator<LocalDateSegment<T>> iteratorA = a.iterator();
        Iterator<LocalDateSegment<T>> iteratorB = b.iterator();
        int index = 0;
        while (iteratorA.hasNext() && iteratorB.hasNext()) {
            LocalDateSegment<T> segA = iteratorA.next();
            LocalDateSegment<T> segB = iteratorB.next();
            if (!Objects.equals(segA, segB)) {
                throw new AssertionError("Ikke like segmenter i posisjon " + index + " har " + segA + " og " + segB);
            }
        }

        if (iteratorA.hasNext() || iteratorB.hasNext()) {
            throw new AssertionError("Ikke like mange segmenter i " + a + " og " + b);
        }

    }

    private static GammelLocalDateTimeline.JoinStyle mapJoinStyle(LocalDateTimeline.JoinStyle joinStyle) {
        return switch (joinStyle) {
            case CROSS_JOIN -> GammelLocalDateTimeline.JoinStyle.CROSS_JOIN;
            case DISJOINT -> GammelLocalDateTimeline.JoinStyle.DISJOINT;
            case INNER_JOIN -> GammelLocalDateTimeline.JoinStyle.INNER_JOIN;
            case LEFT_JOIN -> GammelLocalDateTimeline.JoinStyle.LEFT_JOIN;
            case RIGHT_JOIN -> GammelLocalDateTimeline.JoinStyle.RIGHT_JOIN;
        };
    }

    private List<LocalDateSegment<String>> randomSegmenter(Random random, int lengde, int maxIntervallLengdeDager, int maxMellomromLengdeDager) {
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
        return resultat;
    }

}
