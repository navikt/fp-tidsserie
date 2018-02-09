package no.nav.fpsak.tidsserie;

/**
 * Funksjonelt interface for å kombinere verdi fra 2 segmenter i tidsserien for et gitt intervall (og dermed lage et nytt segment med dette intervallet).
 * Brukes til å håndtere kombinasjon av tidsserier ved overlapp osv.
 * <p>
 * Kan angis fullt og helt av bruker av biblioteket.  
 *  
 * @param <T> Java type for segment 1
 * @param <V> Java type for segment 2
 * @param <R> Java type for returnert segment
 */
@FunctionalInterface
public interface LocalDateSegmentCombinator<T, V, R> {

    LocalDateSegment<R> combine(LocalDateInterval datoInterval, LocalDateSegment<T> datoSegment, LocalDateSegment<V> datoSegment2);
}
