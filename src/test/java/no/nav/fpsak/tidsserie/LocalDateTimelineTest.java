package no.nav.fpsak.tidsserie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;

import no.nav.fpsak.tidsserie.json.JsonTimelineFormatter;

public class LocalDateTimelineTest {

    LocalDate today = LocalDate.now();

    @Test
    public void skal_opprette_kontinuerlig_tidslinje() throws Exception {
        LocalDateTimeline<String> tidslinje = basicContinuousTimeline();
        Assertions.assertThat(tidslinje.isContinuous()).isTrue();
    }

    @Test
    public void skal_opprette_ikke_kontinuerlig_tidslinje() throws Exception {
        LocalDateTimeline<String> tidslinje = basicDiscontinuousTimeline();
        Assertions.assertThat(tidslinje.isContinuous()).isFalse();
    }

    @Test
    public void skal_ha_equal_tidslinje_når_intersecter_seg_selv() throws Exception {
        LocalDateTimeline<String> continuousTimeline = basicContinuousTimeline();
        Assertions
            .assertThat(continuousTimeline.intersection(continuousTimeline, StandardCombinators::coalesceLeftHandSide))
            .isEqualTo(continuousTimeline);

        LocalDateTimeline<String> discontinuousTimeline = basicDiscontinuousTimeline();
        assertThat(discontinuousTimeline.intersection(discontinuousTimeline, StandardCombinators::coalesceLeftHandSide))
            .isEqualTo(discontinuousTimeline);
    }

    @Test
    public void skal_intersecte_annen_tidslinje() throws Exception {

        LocalDateTimeline<String> timeline = basicDiscontinuousTimeline();

        LocalDateTimeline<String> annenTimeline = new LocalDateTimeline<>(today.plusDays(1), today.plusDays(5), "bye");

        LocalDateTimeline<String> intersection = timeline.intersection(annenTimeline,
            StandardCombinators::coalesceRightHandSide);

        assertThat(intersection)
            .isEqualTo(new LocalDateTimeline<>(Arrays.asList(new LocalDateSegment<>(today.plusDays(1), today.plusDays(2), "bye"))));

        LocalDateTimeline<String> intersection2 = timeline.intersection(annenTimeline,
            StandardCombinators::coalesceLeftHandSide);
        assertThat(intersection2)
            .isEqualTo(new LocalDateTimeline<>(Arrays.asList(new LocalDateSegment<>(today.plusDays(1), today.plusDays(2), "hello"))));

    }

    @Test
    public void skal_cross_joine_annen_tidslinje() throws Exception {
        LocalDateTimeline<String> timeline = basicDiscontinuousTimeline();

        LocalDate today = LocalDate.now();
        LocalDateTimeline<String> annenTimeline = new LocalDateTimeline<>(today.plusDays(1), today.plusDays(5), "bye");

        List<LocalDateSegment<String>> expectedSegmenter = Arrays.asList(
            new LocalDateSegment<>(today, today, "hello"),
            new LocalDateSegment<>(today.plusDays(1), today.plusDays(2), "hello"),
            new LocalDateSegment<>(today.plusDays(3), today.plusDays(5), "bye"),
            new LocalDateSegment<>(today.plusDays(6), today.plusDays(8), "world"));

        LocalDateTimeline<String> crossJoined = timeline.crossJoin(annenTimeline,
            StandardCombinators::coalesceLeftHandSide);

        assertThat(crossJoined.toSegments()).as("startdato = " + today).containsAll(expectedSegmenter);

        assertThat(crossJoined).as("startdato = " + today).isEqualTo(new LocalDateTimeline<>(expectedSegmenter));

    }

    @Test
    public void skal_ha_empty_tidslinje_når_disjointer_seg_selv() throws Exception {
        LocalDateTimeline<String> continuousTimeline = basicContinuousTimeline();
        Assertions.assertThat(continuousTimeline.disjoint(continuousTimeline, StandardCombinators::coalesceLeftHandSide))
            .isEqualTo(LocalDateTimeline.EMPTY_TIMELINE);

        LocalDateTimeline<String> discontinuousTimeline = basicDiscontinuousTimeline();
        Assertions
            .assertThat(discontinuousTimeline.intersection(discontinuousTimeline,
                StandardCombinators::coalesceLeftHandSide))
            .isEqualTo(discontinuousTimeline);
    }

    @Test
    public void skal_formattere_timeline_som_json_output() throws Exception {
        LocalDateTimeline<String> timeline = basicContinuousTimeline();

        CharSequence json = new JsonTimelineFormatter().formatJson(timeline);

        assertThat(json).isNotNull().contains(LocalDate.now().toString());
    }

    @Test
    public void skal_formattere_timeline_som_json_output_uten_verdier() throws Exception {
        LocalDateTimeline<String> timeline = basicContinuousTimeline();

        CharSequence json = new JsonTimelineFormatter().formatJson(timeline);

        assertThat(json).isNotNull().contains(LocalDate.now().toString()).doesNotContain("datoInterval");

        System.out.println(json);
    }

    @Test
    public void skal_compress_en_tidsserie_med_sammenhengende_intervaller_med_samme_verdi() throws Exception {

        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(2);
        LocalDate d3 = d2.plusDays(1);
        LocalDate d4 = d3.plusDays(2);

        // Arrange
        LocalDateSegment<String> ds1 = new LocalDateSegment<>(d1, d2, "hello");
        LocalDateSegment<String> ds2 = new LocalDateSegment<>(d3, d4, "hello");

        LocalDateTimeline<String> timeline = new LocalDateTimeline<>(Arrays.asList(ds1, ds2));
        assertThat(timeline.size()).isEqualTo(2);

        // Act
        LocalDateTimeline<String> compressedTimeline = timeline.compress(String::equals, (i, v) -> new LocalDateSegment<>(i, v + "*2"));

        // Assert
        assertThat(compressedTimeline.size()).isEqualTo(1);
        assertThat(compressedTimeline).isEqualTo(new LocalDateTimeline<>(d1, d4, "hello*2"));

    }

    @Test
    public void skal_ikke_compresse_en_tidsserie_uten_sammenhengende_intervaller_med_samme_verdi() throws Exception {

        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(2);
        LocalDate d3 = d2.plusDays(2);
        LocalDate d4 = d3.plusDays(2);

        // Arrange
        LocalDateSegment<String> ds1 = new LocalDateSegment<>(d1, d2, "hello");
        LocalDateSegment<String> ds2 = new LocalDateSegment<>(d3, d4, "hello");

        LocalDateTimeline<String> timeline = new LocalDateTimeline<>(Arrays.asList(ds1, ds2));
        assertThat(timeline.size()).isEqualTo(2);

        // Act
        LocalDateTimeline<String> compressedTimeline = timeline.compress();

        // Assert
        assertThat(compressedTimeline).isEqualTo(timeline);

    }

    @Test
    public void skal_gruppere_per_segment_periode() throws Exception {
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(2);
        LocalDate d3 = d2.plusDays(1);
        LocalDate d4 = d3.plusDays(2);
        LocalDate d5 = d4.plusDays(2);

        List<LocalDateSegment<String>> segmenterMedOverlapp = List.of(
            new LocalDateSegment<>(d1, d2, "A"),
            new LocalDateSegment<>(d3, d4, "B"),
            new LocalDateSegment<>(d4, d5, "C"),
            new LocalDateSegment<>(d3, d4, "D"));

        var timeline = LocalDateTimeline.buildGroupOverlappingSegments(segmenterMedOverlapp).compress();
        List<LocalDateInterval> intervaller = List.copyOf(timeline.getDatoIntervaller());
        assertThat(intervaller).hasSize(4);
        
        assertThat(timeline.intersection(intervaller.get(0))).isEqualTo(new LocalDateTimeline<>(intervaller.get(0), List.of("A")));

        assertThat(timeline.intersection(intervaller.get(1))).isEqualTo(new LocalDateTimeline<>(intervaller.get(1), List.of("B", "D")));
        
        assertThat(timeline.intersection(intervaller.get(2))).isEqualTo(new LocalDateTimeline<>(intervaller.get(2), List.of("B", "C", "D")));
        
        assertThat(timeline.intersection(intervaller.get(3))).isEqualTo(new LocalDateTimeline<>(intervaller.get(3), List.of("C")));

    }

    @Test
    public void skal_håndtere_overlapp_når_flere_perioder_overlapper_med_hverandre() {
        Set<LocalDateSegment<Boolean>> segementer = new HashSet<>();
        LocalDateInterval førstePeriode = LocalDateInterval.withPeriodAfterDate(LocalDate.of(2015, 1, 1), Period.of(2, 0, 0));
        LocalDateInterval andrePeriode = LocalDateInterval.withPeriodAfterDate(LocalDate.of(2015, 1, 1), Period.of(2, 9, 29));
        LocalDateInterval sistePeriode = LocalDateInterval.withPeriodAfterDate(LocalDate.of(2016, 1, 1), Period.of(2, 3, 30));

        segementer.add(new LocalDateSegment<>(førstePeriode, true));
        segementer.add(new LocalDateSegment<>(andrePeriode, true));
        segementer.add(new LocalDateSegment<>(sistePeriode, true));

        LocalDateTimeline<Boolean> localDateTimeline = new LocalDateTimeline<>(segementer, StandardCombinators::alwaysTrueForMatch);

        LocalDateInterval forventetResultat = new LocalDateInterval(førstePeriode.getFomDato(), sistePeriode.getTomDato());
        assertThat(localDateTimeline.compress().getDatoIntervaller()).contains(forventetResultat);
    }

    @Test
    public void skal_håndtere_overlapp_når_flere_perioder_overlapper_med_hverandre_2() {
        Set<LocalDateSegment<Boolean>> segementer = new HashSet<>();
        LocalDateInterval førstePeriode = LocalDateInterval.withPeriodAfterDate(LocalDate.of(2015, 2, 1), Period.of(0, 1, 1));
        LocalDateInterval andrePeriode = LocalDateInterval.withPeriodAfterDate(LocalDate.of(2015, 4, 1), Period.of(0, 1, 1));
        LocalDateInterval tredjePeriode = LocalDateInterval.withPeriodAfterDate(LocalDate.of(2015, 8, 1), Period.of(0, 1, 1));
        LocalDateInterval fjerdePeriode = LocalDateInterval.withPeriodAfterDate(LocalDate.of(2015, 10, 1), Period.of(0, 1, 1));
        LocalDateInterval sistePeriode = LocalDateInterval.withPeriodAfterDate(LocalDate.of(2014, 1, 1), Period.of(3, 1, 1));

        segementer.add(new LocalDateSegment<>(førstePeriode, true));
        segementer.add(new LocalDateSegment<>(andrePeriode, true));
        segementer.add(new LocalDateSegment<>(tredjePeriode, true));
        segementer.add(new LocalDateSegment<>(fjerdePeriode, true));
        segementer.add(new LocalDateSegment<>(sistePeriode, true));

        LocalDateTimeline<Boolean> localDateTimeline = new LocalDateTimeline<>(segementer, StandardCombinators::alwaysTrueForMatch);

        LocalDateInterval forventetResultat = new LocalDateInterval(sistePeriode.getFomDato(), sistePeriode.getTomDato());
        assertThat(localDateTimeline.compress().getDatoIntervaller()).contains(forventetResultat);
    }

    @Ignore("Micro performance test - kun for spesielt interesserte! Kan brukes til å avsjekke forbedringer i join algoritme")
    @Test
    public void kjapp_ytelse_test() throws Exception {

        List<LocalDateSegment<String>> segmenter = new ArrayList<>();

        LocalDate dag = LocalDate.now();

        for (int i = 0; i < 1000; i++) {
            segmenter.add(new LocalDateSegment<String>(new LocalDateInterval(dag, dag), dag.toString()));
            dag = dag.plusDays(1);
        }

        long start = System.currentTimeMillis();

        LocalDateTimeline<String> timeline = new LocalDateTimeline<>(segmenter);
        LocalDateTimeline<String> intersection = timeline.intersection(new LocalDateTimeline<>(segmenter),
            StandardCombinators::coalesceLeftHandSide);
        assertThat(intersection).isEqualTo(timeline);

        System.out.println(System.currentTimeMillis() - start);
    }

    private LocalDateTimeline<String> basicContinuousTimeline() {
        LocalDate d1 = today;
        LocalDate d2 = d1.plusDays(2);

        LocalDateSegment<String> ds1 = new LocalDateSegment<>(d1, d2, "hello");
        LocalDateSegment<String> ds2 = new LocalDateSegment<>(d2.plusDays(1), d2.plusDays(3), "world");

        LocalDateTimeline<String> tidslinje = new LocalDateTimeline<>(Arrays.asList(ds1, ds2));
        return tidslinje;
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
