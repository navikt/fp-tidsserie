package no.nav.fpsak.tidsserie.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDateTimelineFormattersTest {
    LocalDate fom = LocalDate.of(1970, 10, 15);
    LocalDate tom = LocalDate.of(1970, 12, 15);
    LocalDate fom2 = LocalDate.of(1971, 10, 15);
    LocalDate tom2 = LocalDate.of(1971, 12, 15);
    JsonTimelineFormatter formatter = new JsonTimelineFormatter();

    @Test
    void serialiser_deserialiser_wrapped_LocalDateTimline() {
        LocalDateSegment<Heisann> seg1 = new LocalDateSegment<>(new LocalDateInterval(fom, tom), new Heisann());
        LocalDateSegment<Heisann> seg2 = new LocalDateSegment<>(new LocalDateInterval(fom2, tom2), new Heisann());
        LocalDateTimeline<Heisann> timeline = new LocalDateTimeline<>(Arrays.asList(seg1, seg2));

        MyTimeline th = new MyTimeline(timeline);
        String json = formatter.formatJson(th);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        MyTimeline output = formatter.fromJson(json, MyTimeline.class);

        assertThat(output.timeline.segmenter().getFirst()).isEqualTo(seg1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void serialiser_deserialiser_primitiv_Long_LocalDateTimline() {

        LocalDateSegment<Long> seg1 = new LocalDateSegment<>(new LocalDateInterval(fom, tom), 1L);
        LocalDateSegment<Long> seg2 = new LocalDateSegment<>(new LocalDateInterval(fom2, tom2), 99L);
        LocalDateTimeline<Long> timeline = new LocalDateTimeline<>(Arrays.asList(seg1, seg2));

        String json = formatter.formatJson(timeline);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        LocalDateTimeline<Long> output = formatter.fromJson(json, LocalDateTimeline.class, Long.class);

        assertThat(output.segmenter().getFirst()).isEqualTo(seg1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void serialiser_deserialiser_List_LocalDateTimeline() {

        LocalDateSegment<List<String>> seg1 = new LocalDateSegment<>(new LocalDateInterval(fom, tom), List.of("abc", "def"));
        LocalDateSegment<List<String>> seg2 = new LocalDateSegment<>(new LocalDateInterval(fom2, tom2), List.of("ghi", "jdk"));
        LocalDateTimeline<List<String>> timeline = new LocalDateTimeline<>(Arrays.asList(seg1, seg2));

        String json = formatter.formatJson(timeline);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        LocalDateTimeline<List<String>> output1 = formatter.fromJson(json, LocalDateTimeline.class, List.class);
        LocalDateTimeline<List<String>> output2 = formatter.fromJson(json, LocalDateTimeline.class, List.class, String.class);

        assertThat(output1.segmenter().getFirst()).isEqualTo(seg1);
        assertThat(output2.segmenter().getLast()).isEqualTo(seg2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void serialiser_deserialiser_BigDecimal_LocalDateTimline() {

        LocalDateSegment<BigDecimal> seg1 = new LocalDateSegment<>(new LocalDateInterval(fom, tom), new BigDecimal(22L));
        LocalDateSegment<BigDecimal> seg2 = new LocalDateSegment<>(new LocalDateInterval(fom2, tom2), new BigDecimal("33"));
        LocalDateTimeline<BigDecimal> timeline = new LocalDateTimeline<>(Arrays.asList(seg1, seg2));

        String json = formatter.formatJson(timeline);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        LocalDateTimeline<BigDecimal> output = formatter.fromJson(json, LocalDateTimeline.class, BigDecimal.class);

        assertThat(output.segmenter().getFirst()).isEqualTo(seg1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void serialiser_deserialiser_String_LocalDateTimline() {

        LocalDateSegment<String> seg1 = new LocalDateSegment<>(new LocalDateInterval(fom, tom), "hei");
        LocalDateSegment<String> seg2 = new LocalDateSegment<>(new LocalDateInterval(fom2, tom2), "hallo");
        LocalDateTimeline<String> timeline = new LocalDateTimeline<>(Arrays.asList(seg1, seg2));

        String json = formatter.formatJson(timeline);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        LocalDateTimeline<String> output = formatter.fromJson(json, LocalDateTimeline.class, String.class);

        assertThat(output.segmenter().getFirst()).isEqualTo(seg1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void serialiser_deserialiser_LocalDateTimline() {

        LocalDateSegment<Heisann> seg1 = new LocalDateSegment<>(new LocalDateInterval(fom, tom), new Heisann());
        LocalDateSegment<Heisann> seg2 = new LocalDateSegment<>(new LocalDateInterval(fom2, tom2), new Heisann());
        LocalDateTimeline<Heisann> timeline = new LocalDateTimeline<>(Arrays.asList(seg1, seg2));

        String json = formatter.formatJson(timeline);

        assertThat(json).contains("[ \"1970-10-15\", \"1970-12-15\", ");

        LocalDateTimeline<Heisann> output = formatter.fromJson(json, LocalDateTimeline.class, Heisann.class);

        assertThat(output.segmenter().getFirst()).isEqualTo(seg1);
    }

    public static class MyTimeline {
        private LocalDateTimeline<Heisann> timeline;

        @JsonCreator
        public MyTimeline() {
        }

        public MyTimeline(LocalDateTimeline<Heisann> timeline) {
            this.timeline = timeline;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MyTimeline other && Objects.equals(timeline, other.timeline);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timeline);
        }
    }

    public static class HeisannWrapper {
        private final LocalDateSegment<Heisann> heisann;

        public HeisannWrapper(LocalDateSegment<Heisann> heisann) {
            this.heisann = heisann;
        }

        public LocalDateSegment<Heisann> getHeisann() {
            return heisann;
        }
    }

    public static class Heisann {
        private final String hello = "hello";
        private final String bye = "bye";

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Heisann hei && Objects.equals(hello, hei.hello) && Objects.equals(bye, hei.bye);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hello, bye);
        }
    }
}
