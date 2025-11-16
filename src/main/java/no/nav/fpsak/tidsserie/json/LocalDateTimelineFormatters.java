package no.nav.fpsak.tidsserie.json;

import java.util.ArrayList;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

/**
 * Custom serializer/deserializer for LocalDateTimeline for å håndtere deserialisering av nøstede objekter uten å forurense json struktur
 * med class name eller andre koder.
 */
public class LocalDateTimelineFormatters {

    @SuppressWarnings("rawtypes")
    public static class Deserializer extends StdDeserializer<LocalDateTimeline> {

        private JavaType valueType;

        public Deserializer() {
            super(LocalDateTimeline.class);
        }

        private Deserializer(JavaType valueType) {
            this();
            this.valueType = valueType;
        }

        @SuppressWarnings("unchecked")
        @Override
        public LocalDateTimeline deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
            if (p.isExpectedStartArrayToken()) {
                JsonToken t = p.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    return null;
                }
                
                JavaType parametricType = ctx.getTypeFactory().constructParametricType(LocalDateSegment.class, valueType);

                ArrayList<LocalDateSegment> list = new ArrayList<>();
                while (t != JsonToken.END_ARRAY) {
                    list.add(p.readValueAs(parametricType));
                    t = p.nextToken();
                }

                return new LocalDateTimeline(list);
            }
            throw ctx.wrongTokenException(p, handledType(), JsonToken.VALUE_STRING, "Expected array or string.");
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
    public static class Serializer extends StdSerializer<LocalDateTimeline> {
        private static final JsonTimelineFormatter FORMATTER = new JsonTimelineFormatter();

        public Serializer() {
            super(LocalDateTimeline.class);
        }

        @Override
        public void serialize(LocalDateTimeline value, JsonGenerator gen, SerializationContext provider) {
            String json = FORMATTER.formatJson(value.toSegments());
            gen.writeRawValue(json);
        }

    }

}
