package no.nav.fpsak.tidsserie.json;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

import no.nav.fpsak.tidsserie.LocalDateInterval;

/** Custom serialisering, deserialisering av LocalDateInterval. Json struktur blir en array med fom, tom dato på ISO format. */
public class LocalDateIntervalFormatters {

    public static class Deserializer extends StdDeserializer<LocalDateInterval> {
        public Deserializer() {
            super(LocalDateInterval.class);
        }

        @Override
        public LocalDateInterval deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
            if (p.isExpectedStartArrayToken()) {
                JsonToken t = p.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    return null;
                }
                String fom = null;
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    fom = p.getString().trim();
                }
                t = p.nextToken();
                String tom = null;
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    tom = p.getString().trim();
                }
                LocalDateInterval dateInterval = LocalDateInterval.parseFrom(fom, tom);

                t = p.nextToken();
                if (t != JsonToken.END_ARRAY) {
                    throw ctx.wrongTokenException(p, handledType(), JsonToken.END_ARRAY, "Expected array to end");
                }

                return dateInterval;
            }
            throw ctx.wrongTokenException(p, handledType(), JsonToken.VALUE_STRING, "Expected array or string.");
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
            g.writeString(LocalDateInterval.formatDate(value.getFomDato(), "-"));
            g.writeString(LocalDateInterval.formatDate(value.getTomDato(), "-"));
            g.writeEndArray();
        }
    }

}
