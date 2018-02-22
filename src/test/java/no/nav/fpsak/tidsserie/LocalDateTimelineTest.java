package no.nav.fpsak.tidsserie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        LocalDateTimeline<String> compressedTimeline = timeline.compress();

        // Assert
        assertThat(compressedTimeline.size()).isEqualTo(1);
        assertThat(compressedTimeline).isEqualTo(new LocalDateTimeline<>(d1, d4, "hello"));

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
    public void skal_samle_tomme_intervaller_kortere_enn_2_uker_og_med_foregående_intervall_lenger_enn_4_uker() throws Exception {

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
