package no.nav.fpsak.tidsserie.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer.Vanilla;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;

/**
 * Custom serializer/deserializer for LocalDateSement for å håndtere deserialisering av nøstede objekter uten å forurense json struktur
 * med class name eller andre koder. Ved serialisering er indre objekt kjent. Ved deserialisering angis dette fra generic parameter på felt
 * (evt. fra LocalDateTimeline).
 */
public class LocalDateSegmentFormatters {

    public static class Deserializer extends StdDeserializer<LocalDateSegment<?>> implements ContextualDeserializer {
        private JavaType valueType;

        public Deserializer() {
            super(LocalDateSegment.class);
        }

        private Deserializer(JavaType valueType) {
            this();
            this.valueType = valueType;
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
                    val = p.getCodec().readValue(p, valueType);
                } else {
                    if (valueType != null) {
                        val = p.getCodec().readValue(p, valueType);
                    } else {
                        val = new Vanilla().deserialize(p, ctx);
                    }
                }

                t = p.nextToken();
                if (t != JsonToken.END_ARRAY) {
                    throw ctx.wrongTokenException(p, handledType(), JsonToken.END_ARRAY, "Expected array to end");
                }

                return new LocalDateSegment<>(dateInterval, val);
            }
            throw ctx.wrongTokenException(p, handledType(), JsonToken.VALUE_STRING, "Expected array or string.");
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctx, BeanProperty property) throws JsonMappingException {
            JavaType wrapperType;
            if (property == null) {
                wrapperType = ctx.getContextualType();
            } else {
                wrapperType = property.getType();
            }
            JavaType valueType = wrapperType.containedType(0);
            return new Deserializer(valueType);
        }
    }

    @SuppressWarnings("rawtypes")
    public static class Serializer extends StdSerializer<LocalDateSegment> {
        public Serializer() {
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

}
