package no.nav.fpsak.tidsserie.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

/** Formatter og output en LocalDateTimeline som JSON struktur, med custom output for mer kompakt og lesbar JSON. */
public class JsonTimelineFormatter {

    private static final ObjectMapper OM;

    static {
        OM = new ObjectMapper();

        OM.registerModule(new JavaTimeModule());
        SimpleModule module = new SimpleModule();

        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE));
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        OM.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
        OM.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
        OM.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        OM.registerModule(module);

    }

    public String formatJson(Object obj) {
        return formatJson(OM, obj);
    }

    public <T> T fromJson(String src, Class<T> resultClass) {
        try {
            return OM.readValue(src, resultClass);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Kunne ikke deserialisere json til [%s]: %s", resultClass.getName(), src), e);
        }
    }
    
    /** For å deserialisere klasser med generic parametere.*/
    public <T> T fromJson(String src, Class<T> resultClass, Class<?> parameterClass) {
        try {
            JavaType parametricType = OM.getTypeFactory().constructParametricType(resultClass, parameterClass);
            return OM.readValue(src, parametricType);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Kunne ikke deserialisere json til [%s]: %s", resultClass.getName(), src), e);
        }
    }

    public InputStream toInputStream(CharSequence str) {
        try {
            return new ByteArrayInputStream(str.toString().getBytes(StandardCharsets.UTF_8.name()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Kunne ikke konvertere til InputStream: " + str, e);
        }
    }

    private String formatJson(ObjectMapper objectMapper, Object obj) {
        StringWriter sw = new StringWriter(1000);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(sw, obj);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Kunne ikke serialiseres til json: %s", obj), e);
        }
        return sw.toString();
    }

}
