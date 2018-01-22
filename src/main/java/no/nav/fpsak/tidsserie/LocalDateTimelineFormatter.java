package no.nav.fpsak.tidsserie;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

/** Formatter og output en LocalDateTimeline som JSON struktur, med custom output for mer kompakt og lesbar JSON. */
public class LocalDateTimelineFormatter {
    public static class LocalDateIntervalSerializer extends StdSerializer<LocalDateInterval> {
        public LocalDateIntervalSerializer() {
            super(LocalDateInterval.class);
        }

        @Override
        public void serialize(LocalDateInterval value, JsonGenerator g, SerializerProvider provider)
                throws IOException {
            g.writeStartArray();
            g.writeObject(LocalDateInterval.formatDate(value.getFomDato(), "-"));
            g.writeObject(LocalDateInterval.formatDate(value.getTomDato(), "-"));
            g.writeEndArray();
        }
    }
    @SuppressWarnings("rawtypes")
    public static class LocalDateSegmentSerializer extends StdSerializer<LocalDateSegment> {
        public LocalDateSegmentSerializer() {
            super(LocalDateSegment.class);
        }

        @Override
        public void serialize(LocalDateSegment value, JsonGenerator g, SerializerProvider provider)
                throws IOException {
            g.writeStartArray();
            LocalDateInterval dateInterval = value.getLocalDateInterval();
            g.writeObject(LocalDateInterval.formatDate(dateInterval.getFomDato(), "-"));
            g.writeObject(LocalDateInterval.formatDate(dateInterval.getTomDato(), "-"));

            if (value.getValue() != null) {
                g.writeObject(value.getValue());
            }
            g.writeEndArray();
        }
    }

    @SuppressWarnings("rawtypes")
    public static class LocalDateTimelineSerializer extends StdSerializer<LocalDateTimeline> {
        private static final LocalDateTimelineFormatter FORMATTER = new LocalDateTimelineFormatter();

        public LocalDateTimelineSerializer() {
            super(LocalDateTimeline.class);
        }

        @Override
        public void serialize(LocalDateTimeline value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            String json = FORMATTER.formatJson(value.toSegments());
            gen.writeRawValue(json);
        }

    }

    private static final ObjectMapper OM;

    static {
        OM = new ObjectMapper();

        OM.registerModule(new JavaTimeModule());
        SimpleModule module = new SimpleModule();

        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE));
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        OM.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
        OM.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
        OM.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        OM.registerModule(module);

    }

    public String formatJson(Object obj) {
        return formatJson(OM, obj);
    }

    public InputStream toInputStream(CharSequence str) {
        try {
            return new ByteArrayInputStream(str.toString().getBytes(StandardCharsets.UTF_8.name()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Kunne ikke konvertere til InputStream: " + str, e);
        }
    }

    private String formatJson(ObjectMapper objectMapper, Object obj) {
        StringWriter sw = new StringWriter(1000);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(sw, obj);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Kunne ikke serialiseres til json: %s", obj), e);
        }
        return sw.toString();
    }

}
