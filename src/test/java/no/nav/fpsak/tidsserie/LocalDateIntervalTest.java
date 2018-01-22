package no.nav.fpsak.tidsserie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.NavigableSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public class LocalDateIntervalTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void skal_overlappe() throws Exception {
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
    public void skal_ikke_overlappe() throws Exception {
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
    public void skal_inneholde() throws Exception {
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
    public void skal_ikke_inneholde() throws Exception {
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(2);
        LocalDate d3 = d2.plusDays(1);
        LocalDate d4 = d3.plusDays(2);
        assertThat(new LocalDateInterval(d3, d4).contains(new LocalDateInterval(d1, d2))).isFalse();

        assertThat(new LocalDateInterval(d1, d3).contains(new LocalDateInterval(d3, d4))).isFalse();

    }

    @Test
    public void skal_splitte_intervall_på_lik_grense_fom_tom() throws Exception {
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
    public void skal_splitte_intervall_som_grenser_til_hverandre() throws Exception {
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
    public void skal_ikke_splitte_intervall_som_ikke_overlapper_eller_ligger_inntil_hverandre() throws Exception {
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
    public void skal_dele_opp_intervall_når_det_trekkes_fra_annet() throws Exception {
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
    public void skal_dele_opp_intervall_når_det_trekkes_fra_annet_i_midten() throws Exception {
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
    public void skal_dele_opp_intervall_når_det_trekkes_fra_annet_fra_start() throws Exception {
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
    public void skal_expande_intervall_når_det_ligger_inntil() throws Exception {

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
    public void skal_kaste_exception_når_forsøker_expanded_interval_som_ikke_er_abut() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        
        LocalDate d1 = LocalDate.now();
        LocalDate d2 = d1.plusDays(3);
        LocalDate d3 = d2.plusDays(2);
        LocalDate d4 = d3.plusDays(1);
        LocalDateInterval int1 = new LocalDateInterval(d1, d2);
        LocalDateInterval int2 = new LocalDateInterval(d3, d4);

        int1.expand(int2);
    }
    

    @Test
    public void skal_expande_intervall_når_de_overlapper() throws Exception {
        
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
