package no.nav.fpsak.tidsserie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.NavigableSet;

import org.junit.jupiter.api.Test;

class LocalDateIntervalTest {

    @Test
    void skal_overlappe() {
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(3);
        LocalDate d3 = d2;
        LocalDate d4 = d3.plusDays(3);
        
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        assertThat(int1.overlaps(int2)).isTrue();
        assertThat(int1.overlap(int2)).hasValue(new LocalDateInterval(d2, d3));

        LocalDate d3_1 = d2.minusDays(2);

        LocalDateInterval int3 = new LocalDateInterval(d1, d2);
        LocalDateInterval int4 = new LocalDateInterval(d3_1, d4);

        assertThat(int3.overlaps(int4)).isTrue();
        assertThat(int3.overlap(int4)).hasValue(new LocalDateInterval(d3_1, d3));

    }

    @Test
    void skal_ikke_overlappe() {
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(3);
        LocalDate d3 = d2.plusDays(1);
        LocalDate d4 = d3.plusDays(3);
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        assertThat(int1.overlaps(int2)).isFalse();
        assertThat(int1.overlap(int2)).isEmpty();

    }

    @Test
    void skal_inneholde() {
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(5);
        LocalDate d3 = d1.plusDays(3);
        LocalDate d4 = d3.plusDays(2);
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        assertThat(int1.contains(int2)).isTrue();

        assertThat(int2.contains(int1)).isFalse();

    }

    @Test
    void skal_ikke_inneholde() {
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(2);
        LocalDate d3 = d2.plusDays(1);
        LocalDate d4 = d3.plusDays(2);
        assertThat(new LocalDateInterval(d3, d4).contains(new LocalDateInterval(d1, d2))).isFalse();

        assertThat(new LocalDateInterval(d1, d3).contains(new LocalDateInterval(d3, d4))).isFalse();

    }

    @Test
    void skal_splitte_intervall_på_lik_grense_fom_tom() {
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(3);
        LocalDate d3 = d2;
        LocalDate d4 = d3.plusDays(3);
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        NavigableSet<LocalDateInterval> splittet = int1.splitAll(int2);
        assertThat(splittet).isEqualTo(int2.splitAll(int1)); // symmetrisk splitt

        assertThat(splittet).containsExactly(
                new LocalDateInterval(d1, d2.minusDays(1)),
                new LocalDateInterval(d2, d3),
                new LocalDateInterval(d3.plusDays(1), d4));

    }

    @Test
    void skal_splitte_intervall_som_grenser_til_hverandre() {
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(3);
        LocalDate d3 = d2.plusDays(1);
        LocalDate d4 = d3.plusDays(3);
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        NavigableSet<LocalDateInterval> splittet = int1.splitAll(int2);
        assertThat(splittet).isEqualTo(int2.splitAll(int1)); // symmetrisk splitt

        assertThat(splittet).containsExactly(
                new LocalDateInterval(d1, d2),
                new LocalDateInterval(d3, d4));

    }

    @Test
    void skal_ikke_splitte_intervall_som_ikke_overlapper_eller_ligger_inntil_hverandre() {
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(3);
        LocalDate d3 = d2.plusDays(3);
        LocalDate d4 = d3.plusDays(3);
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        NavigableSet<LocalDateInterval> splittet = int1.splitAll(int2);
        assertThat(splittet).isEqualTo(int2.splitAll(int1)); // symmetrisk splitt

        assertThat(splittet).containsExactly(
                new LocalDateInterval(d1, d2),
                new LocalDateInterval(d3, d4));

    }

    @Test
    void skal_dele_opp_intervall_når_det_trekkes_fra_annet() {
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(3);
        LocalDate d3 = d1.plusDays(1);
        LocalDate d4 = d3.plusDays(3);
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        NavigableSet<LocalDateInterval> splittet = int1.except(int2);
        assertThat(splittet).isNotEqualTo(int2.splitAll(int1)); // ikke symmetrisk splitt

        assertThat(splittet).containsExactly(new LocalDateInterval(d1, d1));
    }

    @Test
    void skal_dele_opp_intervall_når_det_trekkes_fra_annet_i_midten() {
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(3);
        LocalDate d3 = d1.plusDays(1);
        LocalDate d4 = d3.plusDays(1);
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        NavigableSet<LocalDateInterval> splittet = int1.except(int2);
        assertThat(splittet).isNotEqualTo(int2.splitAll(int1)); // ikke symmetrisk splitt

        assertThat(splittet).containsExactly(new LocalDateInterval(d1, d1), new LocalDateInterval(d2, d2));
    }

    @Test
    void skal_dele_opp_intervall_når_det_trekkes_fra_annet_fra_start() {
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(3);
        LocalDate d3 = d1.minusDays(2);
        LocalDate d4 = d2.minusDays(1);
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        NavigableSet<LocalDateInterval> splittet = int1.except(int2);
        assertThat(splittet).isNotEqualTo(int2.splitAll(int1)); // ikke symmetrisk splitt

        assertThat(splittet).containsExactly(new LocalDateInterval(d4.plusDays(1), d2));
    }

    @Test
    void skal_expande_intervall_når_det_ligger_inntil() {

        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(3);
        LocalDate d3 = d2.plusDays(1);
        LocalDate d4 = d3.plusDays(1);
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        LocalDateInterval expanded = int1.expand(int2);
        assertThat(expanded).isEqualTo(new LocalDateInterval(d1, d4));
    }

    @Test
    void skal_kaste_exception_når_forsøker_expanded_interval_som_ikke_er_abut() {

        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(3);
        LocalDate d3 = d2.plusDays(2);
        LocalDate d4 = d3.plusDays(1);
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        assertThrows(IllegalArgumentException.class, () -> int1.expand(int2));
    }

    @Test
    void skal_expande_intervall_når_de_overlapper() {

        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(3);
        LocalDate d3 = d1.plusDays(2);
        LocalDate d4 = d3.plusDays(1);
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        LocalDateInterval expanded = int1.expand(int2);
        assertThat(expanded).isEqualTo(new LocalDateInterval(d1, d4));
    }

}
