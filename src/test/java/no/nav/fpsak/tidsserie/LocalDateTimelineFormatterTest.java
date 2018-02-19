package no.nav.fpsak.tidsserie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonCreator;

public class LocalDateTimelineFormatterTest {

    @Test
    public void serialiser_deserialiser_LocalDateInterval() throws Exception {
        LocalDate fom = LocalDate.of(1970, 10, 15);
        LocalDate tom = LocalDate.of(1970, 12, 15);

        LocalDateInterval dateInterval = new LocalDateInterval(fom, tom);

        LocalDateTimelineFormatter formatter = new LocalDateTimelineFormatter();

        String json = formatter.formatJson(dateInterval);

        assertThat(json).isEqualTo("[ \"1970-10-15\", \"1970-12-15\" ]");

        LocalDateInterval output = formatter.fromJson(json, LocalDateInterval.class);
        assertThat(output.getFomDato()).isEqualTo(fom);
        assertThat(output.getTomDato()).isEqualTo(tom);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serialiser_deserialiser_LocalDateSegment() throws Exception {
        LocalDate fom = LocalDate.of(1970, 10, 15);
        LocalDate tom = LocalDate.of(1970, 12, 15);

        LocalDateInterval dateInterval = new LocalDateInterval(fom, tom);

        LocalDateTimelineFormatter formatter = new LocalDateTimelineFormatter();

        LocalDateSegment<?> seg = new LocalDateSegment<>(dateInterval, new Heisann());

        String json = formatter.formatJson(seg);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        LocalDateSegment<Heisann> output = formatter.fromJson(json, LocalDateSegment.class);
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

        LocalDateTimelineFormatter formatter = new LocalDateTimelineFormatter();

        LocalDateSegment<?> seg = new LocalDateSegment<>(dateInterval, "hello");

        String json = formatter.formatJson(seg);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        LocalDateSegment<String> output = formatter.fromJson(json, LocalDateSegment.class);

        assertThat(output.getValue()).isEqualTo("hello");
    }

    @Test
    public void serialiser_deserialiser_LocalDateTimline() throws Exception {
        LocalDate fom = LocalDate.of(1970, 10, 15);
        LocalDate tom = LocalDate.of(1970, 12, 15);
        LocalDate fom2 = LocalDate.of(1971, 10, 15);
        LocalDate tom2 = LocalDate.of(1971, 12, 15);

        LocalDateTimelineFormatter formatter = new LocalDateTimelineFormatter();

        LocalDateSegment<Heisann> seg1 = new LocalDateSegment<>(new LocalDateInterval(fom, tom), new Heisann());
        LocalDateSegment<Heisann> seg2 = new LocalDateSegment<>(new LocalDateInterval(fom2, tom2), new Heisann());
        LocalDateTimeline<Heisann> timeline = new LocalDateTimeline<>(Arrays.asList(seg1, seg2));

        MyTimeline th = new MyTimeline(timeline);
        String json = formatter.formatJson(th);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        MyTimeline output = formatter.fromJson(json, MyTimeline.class);

        assertThat(output.timeline.toSegments().first()).isEqualTo(seg1);
    }

    @SuppressWarnings("rawtypes")
    public static class MyTimeline {
        private LocalDateTimeline timeline;

        @JsonCreator
        public MyTimeline() {
        }

        public MyTimeline(LocalDateTimeline timeline) {
            this.timeline = timeline;
        }

        @Override
        public boolean equals(Object obj) {
            MyTimeline other = (MyTimeline) obj;
            return Objects.equals(timeline, other.timeline);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timeline);
        }
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
