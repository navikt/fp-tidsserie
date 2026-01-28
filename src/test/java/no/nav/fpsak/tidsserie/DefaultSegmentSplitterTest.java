package no.nav.fpsak.tidsserie;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultSegmentSplitterTest {

    LocalDate dag1 = LocalDate.now();
    LocalDate dag2 = dag1.plusDays(1);

    @Test
    void skal_splitte_segment_med_en_enkel_verdi_i_to() {
        LocalDateSegment<String> segment = new LocalDateSegment<>(dag1, dag2, "A");

        LocalDateTimeline.DefaultSegmentSplitter<String> splitter = new LocalDateTimeline.DefaultSegmentSplitter<>();
        LocalDateSegment<String> splittetDag1 = splitter.apply(new LocalDateInterval(dag1, dag1), segment);
        LocalDateSegment<String> splittetDag2 = splitter.apply(new LocalDateInterval(dag2, dag2), segment);
        assertThat(splittetDag1).isEqualTo(new LocalDateSegment<>(dag1, dag1, "A"));
        assertThat(splittetDag2).isEqualTo(new LocalDateSegment<>(dag2, dag2, "A"));
    }

    @Test
    void skal_splitte_segment_med_en_map_i_to_og_hver_del_skal_ha_en_map_som_ikke_er_det_samme_objektet() {
        Map<String, Integer> inputMap = Map.of("A", 1, "B", 2);
        LocalDateSegment<Map<String, Integer>> segment = new LocalDateSegment<>(dag1, dag2, inputMap);

        LocalDateTimeline.DefaultSegmentSplitter<Map<String, Integer>> splitter = new LocalDateTimeline.DefaultSegmentSplitter<>();
        LocalDateSegment<Map<String, Integer>> splittetDag1 = splitter.apply(new LocalDateInterval(dag1, dag1), segment);
        LocalDateSegment<Map<String, Integer>> splittetDag2 = splitter.apply(new LocalDateInterval(dag2, dag2), segment);
        assertThat(splittetDag1).isEqualTo(new LocalDateSegment<>(dag1, dag1, Map.of("A", 1, "B", 2)));
        assertThat(splittetDag2).isEqualTo(new LocalDateSegment<>(dag2, dag2, Map.of("A", 1, "B", 2)));

        assertThat(splittetDag1.getValue()).isNotSameAs(splittetDag2.getValue());
    }

    @Test
    void skal_lage_kopi_av_tree_set_for_å_beholde_rekkefølge() {
        Set<String> inputSet = new TreeSet<>(Comparator.reverseOrder());
        inputSet.add("foo");
        inputSet.add("bar");

        LocalDateSegment<Set<String>> segment = new LocalDateSegment<>(dag1, dag2, inputSet);

        LocalDateTimeline.DefaultSegmentSplitter<Set<String>> splitter = new LocalDateTimeline.DefaultSegmentSplitter<>();
        LocalDateSegment<Set<String>> splittetDag1 = splitter.apply(new LocalDateInterval(dag1, dag1), segment);
        LocalDateSegment<Set<String>> splittetDag2 = splitter.apply(new LocalDateInterval(dag2, dag2), segment);

        assertThat(splittetDag1).isEqualTo(new LocalDateSegment<>(dag1, dag1, inputSet));
        assertThat(splittetDag2).isEqualTo(new LocalDateSegment<>(dag2, dag2, inputSet));

        assertThat(splittetDag1.getValue()).isNotSameAs(splittetDag2.getValue());
        assertThat(splittetDag1.getValue()).isInstanceOf(TreeSet.class);
        assertThat(splittetDag2.getValue()).isInstanceOf(TreeSet.class);

        //foo skal være først siden comparator er reverse order
        assertThat(splittetDag1.getValue().iterator().next()).isEqualTo("foo");
        assertThat(splittetDag2.getValue().iterator().next()).isEqualTo("foo");

    }

    @Test
    void skal_lage_kopi_av_linked_hash_set_for_å_beholde_rekkefølge() {
        Set<Integer> inputSet = new LinkedHashSet<>();
        inputSet.add(10000);
        for (int i = 0; i < 100; i++) {
            inputSet.add(i);
        }

        LocalDateSegment<Set<Integer>> segment = new LocalDateSegment<>(dag1, dag2, inputSet);

        LocalDateTimeline.DefaultSegmentSplitter<Set<Integer>> splitter = new LocalDateTimeline.DefaultSegmentSplitter<>();
        LocalDateSegment<Set<Integer>> splittetDag1 = splitter.apply(new LocalDateInterval(dag1, dag1), segment);
        LocalDateSegment<Set<Integer>> splittetDag2 = splitter.apply(new LocalDateInterval(dag2, dag2), segment);

        assertThat(splittetDag1).isEqualTo(new LocalDateSegment<>(dag1, dag1, inputSet));
        assertThat(splittetDag2).isEqualTo(new LocalDateSegment<>(dag2, dag2, inputSet));

        assertThat(splittetDag1.getValue()).isNotSameAs(splittetDag2.getValue());
        assertThat(splittetDag1.getValue()).isInstanceOf(LinkedHashSet.class);
        assertThat(splittetDag2.getValue()).isInstanceOf(LinkedHashSet.class);

        //10000 skal være først siden det ble først satt inn
        assertThat(splittetDag1.getValue().iterator().next()).isEqualTo(10000);
        assertThat(splittetDag2.getValue().iterator().next()).isEqualTo(10000);

    }

    @Test
    void skal_lage_kopi_av_enum_set() {
        Set<TestEnum> inputSet = EnumSet.of(TestEnum.VERDI_A, TestEnum.VERDI_B);

        LocalDateSegment<Set<TestEnum>> segment = new LocalDateSegment<>(dag1, dag2, inputSet);

        LocalDateTimeline.DefaultSegmentSplitter<Set<TestEnum>> splitter = new LocalDateTimeline.DefaultSegmentSplitter<>();
        LocalDateSegment<Set<TestEnum>> splittetDag1 = splitter.apply(new LocalDateInterval(dag1, dag1), segment);
        LocalDateSegment<Set<TestEnum>> splittetDag2 = splitter.apply(new LocalDateInterval(dag2, dag2), segment);

        assertThat(splittetDag1).isEqualTo(new LocalDateSegment<>(dag1, dag1, inputSet));
        assertThat(splittetDag2).isEqualTo(new LocalDateSegment<>(dag2, dag2, inputSet));

        assertThat(splittetDag1.getValue()).isNotSameAs(splittetDag2.getValue());
        assertThat(splittetDag1.getValue()).isInstanceOf(EnumSet.class);
        assertThat(splittetDag2.getValue()).isInstanceOf(EnumSet.class);
    }

    enum TestEnum {
        VERDI_A,
        VERDI_B
    }


}
