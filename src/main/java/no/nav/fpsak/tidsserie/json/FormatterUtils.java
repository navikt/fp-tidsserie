package no.nav.fpsak.tidsserie.json;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.jdk.UntypedObjectDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;

/** Common (De)Serialization utilities. */
class FormatterUtils {

    private FormatterUtils() {
    }

    static JsonToken assertStartArrayGetNextToken(JsonParser p, DeserializationContext ctx, StdDeserializer<?> deser) throws JacksonException {
        if (p.isExpectedStartArrayToken()) {
            return p.nextToken();
        }
        throw ctx.wrongTokenException(p, deser.handledType(), JsonToken.VALUE_STRING, "Expected array or string.");
    }

    static void assertEndArray(JsonParser p, DeserializationContext ctx, StdDeserializer<?> deser) throws JacksonException {
        JsonToken t = p.nextToken();
        if (t != JsonToken.END_ARRAY) {
            throw ctx.wrongTokenException(p, deser.handledType(), JsonToken.END_ARRAY, "Expected array to end");
        }
    }

    static LocalDateInterval deserializeLocalDateInterval(JsonParser p) throws JacksonException {
        String fom = null;
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            fom = p.getString().trim();
        }
        p.nextToken();
        String tom = null;
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            tom = p.getString().trim();
        }
        return LocalDateInterval.parseFrom(fom, tom);
    }

    static void serializeLocalDateInterval(LocalDateInterval value, JsonGenerator g) throws JacksonException {
        g.writeString(LocalDateInterval.formatDate(value.getFomDato(), "-"));
        g.writeString(LocalDateInterval.formatDate(value.getTomDato(), "-"));
    }

    private static Object getValue(JsonParser p, DeserializationContext ctx, JavaType valueType) throws JacksonException {
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

    @SuppressWarnings("rawtypes")
    static LocalDateSegment deserializeLocalDateSegment(JsonParser p, DeserializationContext ctx, JavaType valueType) throws JacksonException {
        LocalDateInterval dateInterval = FormatterUtils.deserializeLocalDateInterval(p);
        p.nextToken();
        Object val = FormatterUtils.getValue(p, ctx, valueType);
        return new LocalDateSegment<>(dateInterval, val);
    }

    @SuppressWarnings("rawtypes")
    static void serializeLocalDateSegment(LocalDateSegment value, JsonGenerator g) throws JacksonException {
        g.writeStartArray();
        serializeLocalDateInterval(value.getLocalDateInterval(), g);
        if (value.getValue() != null) {
            g.writePOJO(value.getValue());
        }
        g.writeEndArray();
    }

    static JavaType getJavaType(DeserializationContext ctx, BeanProperty property) throws JacksonException {
        JavaType wrapperType;
        if (property == null) {
            wrapperType = ctx.getContextualType();
        } else {
            wrapperType = property.getType();
        }
        return wrapperType.containedType(0);

    }

}
