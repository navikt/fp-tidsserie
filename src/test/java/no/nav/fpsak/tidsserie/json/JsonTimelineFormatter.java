package no.nav.fpsak.tidsserie.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.util.TimeZone;


/** Formatter og output en LocalDateTimeline som JSON struktur, med custom output for mer kompakt og lesbar JSON. */
public class JsonTimelineFormatter {

    private static final JsonMapper OM = JsonMapper.builder()
            .defaultTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
            .changeDefaultVisibility(v -> v
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.ANY)
                    .withScalarConstructorVisibility(JsonAutoDetect.Visibility.ANY))
            .build();

    public String formatJson(Object obj) {
        try {
            return OM.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(String.format("Kunne ikke serialiseres til json: %s", obj), e);
        }
    }

    public <T> T fromJson(String src, Class<T> resultClass) {
        try {
            return OM.readValue(src, resultClass);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(String.format("Kunne ikke deserialisere json til [%s]: %s", resultClass.getName(), src), e);
        }
    }
    
    /** For å deserialisere klasser med generic parametere.*/
    public <T> T fromJson(String src, Class<T> resultClass, Class<?> parameterClass) {
        try {
            JavaType parametricType = OM.getTypeFactory().constructParametricType(resultClass, parameterClass);
            return OM.readValue(src, parametricType);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(String.format("Kunne ikke deserialisere json til [%s]: %s", resultClass.getName(), src), e);
        }
    }

}
