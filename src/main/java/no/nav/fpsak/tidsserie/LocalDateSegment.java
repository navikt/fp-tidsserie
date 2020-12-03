package no.nav.fpsak.tidsserie;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import no.nav.fpsak.tidsserie.json.LocalDateSegmentFormatters;

/**
 * Segment representerer en verdi for et enkelt dato intervall.
 * 
 * @param <V> type verdi
 */
@JsonSerialize(using = LocalDateSegmentFormatters.Serializer.class)
@JsonDeserialize(using = LocalDateSegmentFormatters.Deserializer.class)
public class LocalDateSegment<V> implements Comparable<LocalDateSegment<V>>, Serializable {

    private LocalDateInterval datoInterval;

    private V value;

    public LocalDateSegment(LocalDate fom, LocalDate tom, V value) {
        this(new LocalDateInterval(fom, tom), value);
    }

    public LocalDateSegment(LocalDateInterval datoInterval, V value) {
        Objects.requireNonNull(datoInterval, "datoInterval");
        this.datoInterval = datoInterval;
        this.value = value;
    }

    public static <V> LocalDateSegment<V> emptySegment(LocalDate fom, LocalDate tom) {
        return new LocalDateSegment<>(new LocalDateInterval(fom, tom), null);
    }

    @Override
    public int compareTo(LocalDateSegment<V> o) {
        return datoInterval.compareTo(o.datoInterval);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        LocalDateSegment other = (LocalDateSegment) obj;
        return Objects.equals(datoInterval, other.datoInterval)
            && equalValues(other);

    }

    private boolean equalValues(LocalDateSegment<?> other) {
        if (value instanceof BigDecimal && other.value instanceof BigDecimal) {
            // special case
            return ((BigDecimal) value).compareTo((BigDecimal) other.value) == 0;
        } else {
            return Objects.equals(value, other.value);
        }
    }

    public LocalDate getFom() {
        return datoInterval.getFomDato();
    }

    public LocalDateInterval getLocalDateInterval() {
        return datoInterval;
    }

    public LocalDate getTom() {
        return datoInterval.getTomDato();
    }

    public V getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(datoInterval, value);
    }

    public boolean overlapper(@SuppressWarnings("rawtypes") LocalDateSegment o) {
        return datoInterval.overlaps(o.datoInterval);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<datoIntervall=" + datoInterval + ", verdi=" + value + ">";
    }

}
