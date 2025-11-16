package no.nav.fpsak.tidsserie.json;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.jdk.UntypedObjectDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;

/**
 * Custom serializer/deserializer for LocalDateSement for å håndtere deserialisering av nøstede objekter uten å forurense json struktur
 * med class name eller andre koder. Ved serialisering er indre objekt kjent. Ved deserialisering angis dette fra generic parameter på felt
 * (evt. fra LocalDateTimeline).
 */
public class LocalDateSegmentFormatters {

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
            if (p.isExpectedStartArrayToken()) {
                JsonToken t = p.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    return null;
                }
                String fom = null;
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    fom = p.getString().trim();
                }
                p.nextToken();
                String tom = null;
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    tom = p.getString().trim();
                }

                LocalDateInterval dateInterval = LocalDateInterval.parseFrom(fom, tom);

                p.nextToken();
                Object val = getValue(p, ctx);

                t = p.nextToken();
                if (t != JsonToken.END_ARRAY) {
                    throw ctx.wrongTokenException(p, handledType(), JsonToken.END_ARRAY, "Expected array to end");
                }

                return new LocalDateSegment<>(dateInterval, val);
            }
            throw ctx.wrongTokenException(p, handledType(), JsonToken.VALUE_STRING, "Expected array or string.");
        }

        private Object getValue(JsonParser p, DeserializationContext ctx) throws JacksonException {
            if (p.hasToken(JsonToken.START_OBJECT)) {
                return p.readValueAs(valueType);
            } else {
                if (valueType != null) {
                    return p.readValueAs(valueType);
                } else {
                    return new UntypedObjectDeserializer(null, null).deserialize(p, ctx);
                }
            }
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext ctx, BeanProperty property) throws JacksonException {
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
        public void serialize(LocalDateSegment value, JsonGenerator g, SerializationContext provider)
                throws JacksonException {
            g.writeStartArray();
            LocalDateInterval dateInterval = value.getLocalDateInterval();
            g.writeString(LocalDateInterval.formatDate(dateInterval.getFomDato(), "-"));
            g.writeString(LocalDateInterval.formatDate(dateInterval.getTomDato(), "-"));

            if (value.getValue() != null) {
                g.writePOJO(value.getValue());
            }
            g.writeEndArray();
        }
    }

}
