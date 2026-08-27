package no.nav.fpsak.tidsserie.json;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Custom serializer/deserializer for LocalDateSement for å håndtere deserialisering av nøstede objekter uten å forurense json struktur
 * med class name eller andre koder. Ved serialisering er indre objekt kjent. Ved deserialisering angis dette fra generic parameter på felt
 * (evt. fra LocalDateTimeline).
 */
public class LocalDateSegmentFormatters {
    private LocalDateSegmentFormatters() {
        /* This utility class should not be instantiated */
    }

    public static class Deserializer extends StdDeserializer<LocalDateSegment<?>>  {
        private JavaType valueType;

        public Deserializer() {
            super(LocalDateSegment.class);
        }

        private Deserializer(JavaType valueType) {
            this();
            this.valueType = valueType;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public LocalDateSegment deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
            JsonToken t = FormatterUtils.assertStartArrayGetNextToken(p, ctx, this);
            if (t == JsonToken.END_ARRAY) {
                return null;
            }
            LocalDateSegment segment = FormatterUtils.deserializeLocalDateSegment(p, ctx, valueType);

            FormatterUtils.assertEndArray(p, ctx, this);
            return segment;
        }


        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext ctx, BeanProperty property) throws JacksonException {
            return new Deserializer(FormatterUtils.getJavaType(ctx, property));
        }
    }

    @SuppressWarnings("rawtypes")
    public static class Serializer extends StdSerializer<LocalDateSegment> {
        public Serializer() {
            super(LocalDateSegment.class);
        }

        @Override
        public void serialize(LocalDateSegment value, JsonGenerator g, SerializationContext provider)
                throws JacksonException {
            FormatterUtils.serializeLocalDateSegment(value, g);
        }

    }

}
