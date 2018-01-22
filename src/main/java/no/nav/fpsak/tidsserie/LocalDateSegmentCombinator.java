package no.nav.fpsak.tidsserie;

@FunctionalInterface
public interface LocalDateSegmentCombinator<T, V, R> {

    LocalDateSegment<R> combine(LocalDateInterval datoInterval, LocalDateSegment<T> datoSegment, LocalDateSegment<V> datoSegment2);
}
