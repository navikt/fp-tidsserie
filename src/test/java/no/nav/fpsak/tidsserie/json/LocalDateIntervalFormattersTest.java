package no.nav.fpsak.tidsserie.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public class LocalDateIntervalFormattersTest {

    @Test
    public void serialiser_deserialiser_LocalDateInterval() throws Exception {
        LocalDate fom = LocalDate.of(1970, 10, 15);
        LocalDate tom = LocalDate.of(1970, 12, 15);

        LocalDateInterval dateInterval = new LocalDateInterval(fom, tom);

        JsonTimelineFormatter formatter = new JsonTimelineFormatter();

        String json = formatter.formatJson(dateInterval);

        assertThat(json).isEqualTo("[ \"1970-10-15\", \"1970-12-15\" ]");

        LocalDateInterval output = formatter.fromJson(json, LocalDateInterval.class);
        assertThat(output.getFomDato()).isEqualTo(fom);
        assertThat(output.getTomDato()).isEqualTo(tom);
    }
}
