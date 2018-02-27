package no.nav.fpsak.tidsserie.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Objects;

import org.junit.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;

public class LocalDateSegmentFormattersTest {

    @Test
    public void serialiser_deserialiser_LocalDateSegment() throws Exception {
        LocalDate fom = LocalDate.of(1970, 10, 15);
        LocalDate tom = LocalDate.of(1970, 12, 15);

        LocalDateInterval dateInterval = new LocalDateInterval(fom, tom);

        JsonTimelineFormatter formatter = new JsonTimelineFormatter();

        LocalDateSegment<Heisann> seg = new LocalDateSegment<>(dateInterval, new Heisann());

        String json = formatter.formatJson(seg);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        @SuppressWarnings("unchecked")
        LocalDateSegment<Heisann> output = formatter.fromJson(json, LocalDateSegment.class, Heisann.class);
        
        assertThat(output.getFom()).isEqualTo(fom);
        assertThat(output.getTom()).isEqualTo(tom);

        assertThat(output.getValue()).isEqualTo(new Heisann());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serialiser_deserialiser_primitiv_LocalDateSegment() throws Exception {
        LocalDate fom = LocalDate.of(1970, 10, 15);
        LocalDate tom = LocalDate.of(1970, 12, 15);

        LocalDateInterval dateInterval = new LocalDateInterval(fom, tom);

        JsonTimelineFormatter formatter = new JsonTimelineFormatter();

        LocalDateSegment<?> seg = new LocalDateSegment<>(dateInterval, "hello");

        String json = formatter.formatJson(seg);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        LocalDateSegment<String> output = formatter.fromJson(json, LocalDateSegment.class);

        assertThat(output.getValue()).isEqualTo("hello");
    }

    public static class Heisann {
        private String hello = "hello";
        private String bye = "bye";

        @Override
        public boolean equals(Object obj) {
            Heisann hei = (Heisann) obj;
            return Objects.equals(hello, hei.hello)
                && Objects.equals(bye, hei.bye);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hello, bye);
        }
    }
}
