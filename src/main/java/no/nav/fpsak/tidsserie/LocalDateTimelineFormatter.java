package no.nav.fpsak.tidsserie;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer.Vanilla;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
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

    public static class LocalDateIntervalDeserializer extends StdDeserializer<LocalDateInterval> {
        public LocalDateIntervalDeserializer() {
            super(LocalDateInterval.class);
        }

        @Override
        public LocalDateInterval deserialize(JsonParser p, DeserializationContext ctx) throws IOException, JsonProcessingException {
            if (p.isExpectedStartArrayToken()) {
                JsonToken t = p.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    return null;
                }
                String fom = null;
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    fom = p.getText().trim();
                }
                t = p.nextToken();
                String tom = null;
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    tom = p.getText().trim();
                }

                t = p.nextToken();
                if (t != JsonToken.END_ARRAY) {
                    throw ctx.wrongTokenException(p, handledType(), JsonToken.END_ARRAY, "Expected array to end");
                }

                return LocalDateInterval.parseFrom(fom, tom);
            }
            throw ctx.wrongTokenException(p, handledType(), JsonToken.VALUE_STRING, "Expected array or string.");
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
                Class<? extends Object> cls = value.getValue().getClass();
                if (!String.class.equals(cls) && !Number.class.isAssignableFrom(cls)) {
                    g.writeStartObject();
                    g.writeStringField("@class", cls.getName());
                    g.writeObjectField("value", value.getValue());
                    g.writeEndObject();
                } else {
                    g.writeObject(value.getValue());
                }
            }
            g.writeEndArray();
        }
    }

    public static class LocalDateSegmentDeserializer extends StdDeserializer<LocalDateSegment<?>> {
        public LocalDateSegmentDeserializer() {
            super(LocalDateSegment.class);
        }

        @Override
        public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
            // TODO Auto-generated method stub
            return super.deserializeWithType(p, ctxt, typeDeserializer);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public LocalDateSegment deserialize(JsonParser p, DeserializationContext ctx) throws IOException, JsonProcessingException {
            if (p.isExpectedStartArrayToken()) {
                JsonToken t = p.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    return null;
                }
                String fom = null;
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    fom = p.getText().trim();
                }
                t = p.nextToken();
                String tom = null;
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    tom = p.getText().trim();
                }

                LocalDateInterval dateInterval = LocalDateInterval.parseFrom(fom, tom);

                Object val = null;
                t = p.nextToken();
                if (p.hasToken(JsonToken.START_OBJECT)) {

                    t = p.nextToken();

                    if (p.hasToken(JsonToken.FIELD_NAME)) {
                        if (!p.getCurrentName().equals("@class")) {
                            throw ctx.wrongTokenException(p, handledType(), JsonToken.FIELD_NAME, "Expected FIELD_NAME: 'value'");
                        }
                    }

                    String clsName = p.nextTextValue();
                    Class<?> targetClass = null;
                    try {
                        targetClass = Class.forName(clsName);
                    } catch (ClassNotFoundException e) {
                        ctx.reportInputMismatch(handledType(), "Kan ikke finne klasse for @class=%s", clsName);
                    }
                    t = p.nextToken();
                    if (p.hasToken(JsonToken.FIELD_NAME)) {
                        if (!p.getCurrentName().equals("value")) {
                            throw ctx.wrongTokenException(p, handledType(), JsonToken.FIELD_NAME, "Expected FIELD_NAME: 'value'");
                        }
                        t = p.nextToken();
                    }
                    val = p.readValueAs(targetClass);
                    t = p.nextToken();

                    if (!p.hasToken(JsonToken.END_OBJECT)) {
                        throw ctx.wrongTokenException(p, handledType(), JsonToken.END_OBJECT, "Expected end of object");
                    }
                } else {
                    val = new Vanilla().deserialize(p, ctx);
                }

                t = p.nextToken();
                if (t != JsonToken.END_ARRAY) {
                    throw ctx.wrongTokenException(p, handledType(), JsonToken.END_ARRAY, "Expected array to end");
                }

                return new LocalDateSegment<>(dateInterval, val);
            }
            throw ctx.wrongTokenException(p, handledType(), JsonToken.VALUE_STRING, "Expected array or string.");
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

    @SuppressWarnings("rawtypes")
    public static class LocalDateTimelineDeserializer extends StdDeserializer<LocalDateTimeline> {
        public LocalDateTimelineDeserializer() {
            super(LocalDateTimeline.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public LocalDateTimeline deserialize(JsonParser p, DeserializationContext ctx) throws IOException, JsonProcessingException {
            if (p.isExpectedStartArrayToken()) {
                JsonToken t = p.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    return null;
                }

                Iterator<LocalDateSegment> iterator = p.readValuesAs(LocalDateSegment.class);
                ArrayList<LocalDateSegment> list = new ArrayList<>();
                for(;iterator.hasNext();) {
                    list.add(iterator.next());
                }
                
                return new LocalDateTimeline(list);
            }
            throw ctx.wrongTokenException(p, handledType(), JsonToken.VALUE_STRING, "Expected array or string.");
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

    public <T> T fromJson(String src, Class<T> resultClass) {
        try {
            return OM.readValue(src, resultClass);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Kunne ikke deserialisere json til [%s]: %s", resultClass.getName(), src), e);
        }
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
