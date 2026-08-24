package no.nav.fpsak.tidsserie.json;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

/** Custom serialisering, deserialisering av LocalDateInterval. Json struktur blir en array med fom, tom dato på ISO format. */
public class LocalDateIntervalFormatters {
    private LocalDateIntervalFormatters() {
        /* This utility class should not be instantiated */
    }

    public static class Deserializer extends StdDeserializer<LocalDateInterval> {
        public Deserializer() {
            super(LocalDateInterval.class);
        }

        @Override
        public LocalDateInterval deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
            JsonToken t = FormatterUtils.assertStartArrayGetNextToken(p, ctx, this);
            if (t == JsonToken.END_ARRAY) {
                return null;
            }
            LocalDateInterval dateInterval = FormatterUtils.deserializeLocalDateInterval(p);

            FormatterUtils.assertEndArray(p, ctx, this);
            return dateInterval;
        }
    }

    public static class Serializer extends StdSerializer<LocalDateInterval> {

        public Serializer() {
            super(LocalDateInterval.class);
        }

        @Override
        public void serialize(LocalDateInterval value, JsonGenerator g, SerializationContext provider)
                throws JacksonException {
            g.writeStartArray();
            FormatterUtils.serializeLocalDateInterval(value, g);
            g.writeEndArray();
        }
    }

}
