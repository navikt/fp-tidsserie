package no.nav.fpsak.tidsserie.json;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled //kan kjøres lokalt for å få inntrykk av ytelse ved endringer på implementasjon
class LocalDateTimelineYtelseTest {

    private final LocalDate today = LocalDate.now();

    @Test
    void kjør() {
        //opprettelse av tidsserier
        bruk_constructor_sorterte_ikkeoverlappende_segmenter(100);
        bruk_constructor_sorterte_ikkeoverlappende_segmenter(1000);
        bruk_constructor_sorterte_overlappende_segmenter(100);
        bruk_constructor_sorterte_overlappende_segmenter(1000);
        bruk_constructor_tilfeldige_overlappende_segmenter(100);
        bruk_constructor_tilfeldige_overlappende_segmenter(1000);
        //Worstcase for constructor. Input hvor hvert nye segment overlapper med alle tidligere segmenter i tidslinjen. Det ender i O(n^2) segmenter, så vanskelig å unngå O(n^2)
        bruk_constructor_ekstrem_overlapp(100);
        bruk_constructor_ekstrem_overlapp(1000);

        //bygging av tidsserier iterativt (isdf å bruke konstruktor med alle segmenter)
        //denne bruken av API er normalt suboptimal ifht eksekveringstid siden den er O(n^2), mens bruk av konstruktor gir nomalt (n ln n) (men er O(n^2) worst case)
        //I praksis er de ofte er denne bruken som påvirker ytelse mest, derfor bra å ha med her
        bygg_iterativt_kombinere_sorterte_ikkeoverlappende_segmenter(100);
        bygg_iterativt_kombinere_sorterte_ikkeoverlappende_segmenter(1000);
        bygg_iterativt_kombiner_tilfeldige_overlappende_segmenter(100);
        bygg_iterativt_kombiner_tilfeldige_overlappende_segmenter(1000);
        bygg_iterativt_ekstrem_overlapp(100);
        bygg_iterativt_ekstrem_overlapp(1000);

        //noe annen bruk
        bruk_combine_uten_å_få_splitt(100);
        bruk_combine_uten_å_få_splitt(1000);
        compress(100);
        compress(1000);

    }

    private void executeTest(String name, Runnable codeUnderTest) {
        //warmup kan økes for å redusere JIT-kompilering underveis i selve testen, men gir lengre samlet testtid
        long warmupNanos = Duration.ofMillis(50).toNanos();

        long experimentTargetDurationNanos = Duration.ofMillis(300).toNanos();
        int minIterations = 10;
        int maxIteratons = 1000;

        //warmup
        long t0warmup = System.nanoTime();
        while (System.nanoTime() - t0warmup < warmupNanos) {
            codeUnderTest.run();
        }

        int iterations = 0;
        double totalTime = 0.0;
        double totalTimeSquared = 0.0;
        long t0init = System.nanoTime();
        while (iterations < maxIteratons && (iterations < minIterations || System.nanoTime() < t0init + experimentTargetDurationNanos)) {
            long t0 = System.nanoTime();
            codeUnderTest.run();
            long time = System.nanoTime() - t0;
            totalTime += time;
            totalTimeSquared += time * time;
            iterations++;
        }
        double mean = totalTime / (double) iterations;
        double stdev = Math.sqrt((iterations * totalTimeSquared - totalTime * totalTime)) / iterations;

        System.out.printf("%-70s :  %6.3f +/- %3.3f ms/operasjon (iterasjoner: %4d)\n", name, mean / 1_000_000, stdev / 1_000_000, iterations);
    }

    void bruk_combine_uten_å_få_splitt(int antall) {
        int antallDagerPrIntervall = 5;
        int antallDagerMellomIntervall = 2;
        LocalDateTimeline<String> timelineA = lagTidslinje(today, antallDagerPrIntervall, antallDagerMellomIntervall, antall, "A");
        LocalDateTimeline<String> timelineB = lagTidslinje(today, antallDagerPrIntervall, antallDagerMellomIntervall, antall, "B");
        LocalDateTimeline<String> timelineAB = lagTidslinje(today, antallDagerPrIntervall, antallDagerMellomIntervall, antall, "AB");

        executeTest("combine " + antall + " segmenter", () -> {
            LocalDateTimeline<String> timelineCrossJoined = timelineA.combine(timelineB, StandardCombinators::concat, JoinStyle.CROSS_JOIN);
            assertThat(timelineCrossJoined).isEqualTo(timelineAB);
        });
    }

    void bruk_constructor_sorterte_overlappende_segmenter(int antall) {
        int antallDagerPrIntervall = 5;
        int antallDagerMellomIntervall = -1;
        List<LocalDateSegment<String>> segmenter = lagSegmenter(today, antallDagerPrIntervall, antallDagerMellomIntervall, antall, "A");

        executeTest("constructor, " + antall + " sorterte overlappende segmenter", () -> {
            LocalDateTimeline<String> tidslinje = new LocalDateTimeline<>(segmenter, StandardCombinators::coalesceRightHandSide);
            assertThat(tidslinje.size()).isGreaterThan(antall);
        });
    }


    void compress(int antall) {
        Random random = new Random(3152354623l);
        for (int i = 0; i < 1000; i++) {
            random.nextInt();
        }
        List<LocalDateSegment<Integer>> segmenter = new ArrayList<>();
        for (int j = 0; j < antall; j++) {
            LocalDate fom = today.plusDays(j);
            LocalDate tom = fom;
            segmenter.add(new LocalDateSegment<>(fom, tom, random.nextInt(3)));
        }
        LocalDateTimeline<Integer> tidslinje = new LocalDateTimeline<>(segmenter, StandardCombinators::coalesceRightHandSide);

        executeTest("compress " + antall + " segmenter", () -> {
            LocalDateTimeline<Integer> komprimert = tidslinje.compress();
            assertThat(komprimert.size()).isLessThan(tidslinje.size());
        });
    }

    void bygg_iterativt_kombinere_sorterte_ikkeoverlappende_segmenter(int antall) {
        int antallDagerPrIntervall = 5;
        int antallDagerMellomIntervall = 0;
        List<LocalDateSegment<String>> segmenter = lagSegmenter(today, antallDagerPrIntervall, antallDagerMellomIntervall, antall, "A");

        executeTest("bygge tidslinje iterativt, " + antall + " sorterte ikke-overlappende segmenter", () -> {
            LocalDateTimeline<String> tidslinje = LocalDateTimeline.empty();
            for (LocalDateSegment<String> segment : segmenter) {
                var segmentLinje = new LocalDateTimeline<>(List.of(segment));
                tidslinje = tidslinje.combine(segmentLinje, StandardCombinators::coalesceRightHandSide, JoinStyle.CROSS_JOIN);
            }
            assertThat(tidslinje.size()).isEqualTo(antall);
        });
    }

    void bruk_constructor_sorterte_ikkeoverlappende_segmenter(int antall) {
        int antallDagerPrIntervall = 5;
        int antallDagerMellomIntervall = 0;
        List<LocalDateSegment<String>> segmenter = lagSegmenter(today, antallDagerPrIntervall, antallDagerMellomIntervall, antall, "A");

        executeTest("constructor, " + antall + " sorterte ikke-ovelappende segmenter", () -> {
            LocalDateTimeline<String> tidslinje = new LocalDateTimeline<String>(segmenter, StandardCombinators::coalesceRightHandSide);
            assertThat(tidslinje.size()).isEqualTo(antall);
        });
    }

    void bygg_iterativt_kombiner_tilfeldige_overlappende_segmenter(int antall) {
        Random random = new Random(3152354623l);
        for (int i = 0; i < 1000; i++) {
            random.nextInt();
        }
        int kandidatdager = 1000;
        int antallDagerPrIntervall = 5;

        List<LocalDateSegment<Integer>> segmenter = new ArrayList<>();
        for (int j = 0; j < antall; j++) {
            LocalDate fom = today.plusDays(random.nextInt(kandidatdager));
            segmenter.add(new LocalDateSegment<>(fom, fom.plusDays(antallDagerPrIntervall), j));
        }
        executeTest("bygge tidslinje iterativt, " + antall + " segmenter, randomisert med overlapp", () -> {
            LocalDateTimeline<Integer> tidslinje = LocalDateTimeline.empty();
            for (LocalDateSegment<Integer> segment : segmenter) {
                var segmentLinje = new LocalDateTimeline<>(List.of(segment));
                tidslinje = tidslinje.combine(segmentLinje, StandardCombinators::coalesceRightHandSide, JoinStyle.CROSS_JOIN);
            }
        });

    }

    void bruk_constructor_tilfeldige_overlappende_segmenter(int antall) {
        Random random = new Random(3152354623l);
        for (int i = 0; i < 1000; i++) {
            random.nextInt();
        }
        int kandidatdager = 1000;
        int antallDagerPrIntervall = 5;

        List<LocalDateSegment<Integer>> segmenter = new ArrayList<>();
        for (int j = 0; j < antall; j++) {
            LocalDate fom = today.plusDays(random.nextInt(kandidatdager));
            segmenter.add(new LocalDateSegment<>(fom, fom.plusDays(antallDagerPrIntervall), j));
        }
        executeTest("constructor, randomisert med overlapp " + antall + " segmenter", () -> {
            LocalDateTimeline<Integer> tidslinje = new LocalDateTimeline<>(segmenter, StandardCombinators::coalesceRightHandSide);
        });
    }

    void bruk_constructor_ekstrem_overlapp(int antall) {
        List<LocalDateSegment<String>> segmenter = new ArrayList<>();
        for (int j = 0; j < antall; j++) {
            LocalDate fom = today.minusDays(j);
            LocalDate tom = today.plusDays(j);
            segmenter.add(new LocalDateSegment<>(fom, tom, "A"));
        }

        executeTest("constructor, ekstrem overlapp " + antall + " segmenter", () -> {
            LocalDateTimeline<String> tidslinje = new LocalDateTimeline<>(segmenter, StandardCombinators::concat);
        });
    }

    void bygg_iterativt_ekstrem_overlapp(int antall) {
        List<LocalDateSegment<String>> segmenter = new ArrayList<>();
        for (int j = 0; j < antall; j++) {
            LocalDate fom = today.minusDays(j);
            LocalDate tom = today.plusDays(j);
            segmenter.add(new LocalDateSegment<>(fom, tom, "A"));
        }

        executeTest("bygge tidslinje iterativt, ekstrem overlapp " + antall + " segmenter", () -> {
            LocalDateTimeline<String> tidslinje = LocalDateTimeline.empty();
            for (LocalDateSegment<String> segment : segmenter) {
                var segmentLinje = new LocalDateTimeline<>(List.of(segment));
                tidslinje = tidslinje.combine(segmentLinje, StandardCombinators::concat, JoinStyle.CROSS_JOIN);
            }
        });
    }

    private static <T> LocalDateTimeline<T> lagTidslinje(LocalDate startdato, int dagerPerIntervall, int dagerMellomIntervall, int antall, T verdi) {
        List<LocalDateSegment<T>> segmenter = lagSegmenter(startdato, dagerPerIntervall, dagerMellomIntervall, antall, verdi);
        return new LocalDateTimeline<>(segmenter);
    }

    private static <T> List<LocalDateSegment<T>> lagSegmenter(LocalDate startdato, int dagerPerIntervall, int dagerMellomIntervall, int antall, T verdi) {
        LocalDate fom = startdato;
        List<LocalDateSegment<T>> segmenter = new ArrayList<>();
        for (int i = 0; i < antall; i++) {
            var tom = fom.plusDays(dagerPerIntervall - 1);
            segmenter.add(new LocalDateSegment<>(fom, tom, verdi));
            fom = tom.plusDays(dagerMellomIntervall + 1);
        }
        return segmenter;
    }

}
