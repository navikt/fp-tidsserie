package no.nav.fpsak.tidsserie.json;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDateSegmentFormattersTest {

    @Test
    void serialiser_deserialiser_LocalDateSegment() {
        LocalDate fom = LocalDate.of(1970, 10, 15);
        LocalDate tom = LocalDate.of(1970, 12, 15);

        var dateInterval = new LocalDateInterval(fom, tom);

        var formatter = new JsonTimelineFormatter();

        var seg = new LocalDateSegment<>(dateInterval, new Heisann());

        String json = formatter.formatJson(seg);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        @SuppressWarnings("unchecked")
        var output = formatter.fromJson(json, LocalDateSegment.class, Heisann.class);

        assertThat(output.getFom()).isEqualTo(fom);
        assertThat(output.getTom()).isEqualTo(tom);

        assertThat(output.getValue()).isEqualTo(new Heisann());
    }

    @SuppressWarnings("unchecked")
    @Test
    void serialiser_deserialiser_primitiv_LocalDateSegment() {
        LocalDate fom = LocalDate.of(1970, 10, 15);
        LocalDate tom = LocalDate.of(1970, 12, 15);

        var dateInterval = new LocalDateInterval(fom, tom);

        var formatter = new JsonTimelineFormatter();

        var seg = new LocalDateSegment<>(dateInterval, "hello");

        String json = formatter.formatJson(seg);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        var output = formatter.fromJson(json, LocalDateSegment.class);

        assertThat(output.getValue()).isEqualTo("hello");
    }

    public static class Heisann {
        private final String hello = "hello";
        private final String bye = "bye";

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Heisann hei && Objects.equals(hello, hei.hello)
                    && Objects.equals(bye, hei.bye);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hello, bye);
        }
    }
}
