package no.nav.fpsak.tidsserie.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

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
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

/**
 * Custom serializer/deserializer for LocalDateTimeline for å håndtere deserialisering av nøstede objekter uten å forurense json struktur
 * med class name eller andre koder.
 */
public class LocalDateTimelineFormatters {

    @SuppressWarnings("rawtypes")
    public static class Deserializer extends StdDeserializer<LocalDateTimeline> implements ContextualDeserializer {

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
        public LocalDateTimeline deserialize(JsonParser p, DeserializationContext ctx) throws IOException, JsonProcessingException {
            if (p.isExpectedStartArrayToken()) {
                JsonToken t = p.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    return null;
                }
                
                JavaType parametricType = ctx.getTypeFactory().constructParametricType(LocalDateSegment.class, valueType);

                Iterator<LocalDateSegment> iterator = p.getCodec().readValues(p, parametricType);
                ArrayList<LocalDateSegment> list = new ArrayList<>();
                for (; iterator.hasNext();) {
                    list.add(iterator.next());
                }

                return new LocalDateTimeline(list);
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
    public static class Serializer extends StdSerializer<LocalDateTimeline> {
        private static final JsonTimelineFormatter FORMATTER = new JsonTimelineFormatter();

        public Serializer() {
            super(LocalDateTimeline.class);
        }

        @Override
        public void serialize(LocalDateTimeline value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            String json = FORMATTER.formatJson(value.toSegments());
            gen.writeRawValue(json);
        }

    }

}
