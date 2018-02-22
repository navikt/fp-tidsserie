package no.nav.fpsak.tidsserie.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.fpsak.tidsserie.LocalDateInterval;

/** Custom serialisering, deserialisering av LocalDateInterval. Json struktur blir en array med fom, tom dato på ISO format. */
public class LocalDateIntervalFormatters {

    public static class Deserializer extends StdDeserializer<LocalDateInterval> {
        public Deserializer() {
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
        public void serialize(LocalDateInterval value, JsonGenerator g, SerializerProvider provider)
                throws IOException {
            g.writeStartArray();
            g.writeObject(LocalDateInterval.formatDate(value.getFomDato(), "-"));
            g.writeObject(LocalDateInterval.formatDate(value.getTomDato(), "-"));
            g.writeEndArray();
        }
    }

}
