package no.nav.fpsak.tidsserie;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import no.nav.fpsak.tidsserie.json.LocalDateTimelineFormatters;

/**
 * En tidslinje bestående av {@link LocalDateSegment}s med [fom, tom] med tilhørende verdi.
 * <p>
 * Tidslinjen bygges av "unordered" og "disjointed" intervaller, men støtter p.t. ikke overlappende intervaller med
 * mindre en {@link LocalDateSegmentCombinator} funksjon angis.
 * <p>
 * Derimot kan to tidsserier kombineres vha. en {@link LocalDateSegmentCombinator} funksjon.
 * <p>
 * API'et er modellert etter metoder fra java.time og threeten-extra Interval.
 *
 * @param <V> Java type for verdier i tidsserien.
 */
@JsonSerialize(using = LocalDateTimelineFormatters.Serializer.class)
@JsonDeserialize(using = LocalDateTimelineFormatters.Deserializer.class)
public class LocalDateTimeline<V> implements Serializable, Iterable<LocalDateSegment<V>> {

    @SuppressWarnings("rawtypes")
    public static final LocalDateTimeline EMPTY_TIMELINE = new LocalDateTimeline<>(Collections.emptyList());
    /**
     * Left-hand side, konstant for håndtere joins mellom tidsserier.
     */
    private static final int LHS = 1;
    /**
     * Right-hand side.
     */
    private static final int RHS = LHS << 1;
    private final NavigableSet<LocalDateSegment<V>> segments = new TreeSet<>();
    /**
     * Funksjon for å beregne partielle datosegmenter der input interval delvis overlapper segmentets intervall. <br>
     * Begge
     * segmenter får samme verdi.
     * <p>
     * Dersom et segment angir en funksjon som varierer med tid (f.eks. en linær funksjon), må en custom SegmentSplitter angis av utvikler.<br>
     * (tar ikke hensyn til vekting av verdi ifht. størrelse på intervallet hvilket kan være aktuelt for tidsserier der segmenter representerer
     * en funksjon av tid.
     * (eks: <code> f(t) = a*t + b ; t0 =&lt; t &lt;= t1 </code>)
     * <p>
     */
    private final SegmentSplitter<V> segmentSplitter;

    public static <V> LocalDateTimeline<V> empty() {
        return EMPTY_TIMELINE;
    }

    /**
     * Convenience constructor for å opprette timeline med kun et segment
     *
     * @param fomDato - fra dato (fra-og-med)
     * @param tomDato - til dato (til-og-med)
     */
    public LocalDateTimeline(LocalDate fomDato, LocalDate tomDato, V value) {
        this(new LocalDateInterval(fomDato, tomDato), value);
    }

    /**
     * Convenience contstructor for å opprette timeline med kun et segment.
     */
    public LocalDateTimeline(LocalDateInterval datoInterval, V value) {
        this(Arrays.asList(new LocalDateSegment<V>(datoInterval, value)));
    }

    public LocalDateTimeline(Collection<LocalDateSegment<V>> datoSegmenter) {
        this(datoSegmenter, null, null);
    }

    public LocalDateTimeline(Collection<LocalDateSegment<V>> datoSegmenter,
                             SegmentSplitter<V> customSegmentSplitter) {
        this(datoSegmenter, null, customSegmentSplitter);
    }

    /**
     * Constructor
     *
     * @param datoSegmenter     - segmenter i tidsserien
     * @param overlapCombinator - Optional combinator dersom noen segmenter overlapper. Må spesifiseres dersom det er sannsynlig kan skje.
     */
    public LocalDateTimeline(Collection<LocalDateSegment<V>> datoSegmenter,
                             LocalDateSegmentCombinator<V, V, V> overlapCombinator) {
        this(datoSegmenter, overlapCombinator, null);
    }

    /**
     * Constructor
     *
     * @param datoSegmenter         - segmenter i tidsserien
     * @param overlapCombinator     - Optional combinator dersom noen segmenter overlapper. Må spesifiseres dersom det er sannsynlig kan skje.
     * @param customSegmentSplitter - Optional splitter dersom enkelt segmenter må splittes. Default splittes interval med konstant verdi.
     */
    public LocalDateTimeline(Collection<LocalDateSegment<V>> datoSegmenter,
                             LocalDateSegmentCombinator<V, V, V> overlapCombinator,
                             SegmentSplitter<V> customSegmentSplitter) {

        Objects.requireNonNull(datoSegmenter, "datoSegmenter");
        this.segmentSplitter = customSegmentSplitter != null ? customSegmentSplitter : new EqualValueSegmentSplitter<>();
        for (LocalDateSegment<V> ds : datoSegmenter) {
            add(ds, overlapCombinator);
        }
        validateNonOverlapping();
    }

    /**
     * Returnerer ny {@link LocalDateTimeline} som kun består av intervaller som passerer angitt test.
     * <p>
     * NB: Bør sørge for at tidslinjen er 'tettest' mulig før denne kalles (dvs. hvis i tvil, bruke {@link #compress()}
     * først).
     * Hvis ikke det gjøers kan f.eks. sjekker som tar hensyn til tidligere intervaller måtte sjekke om flere av disse er
     * lenket og det går utover ytelsen.
     *
     * @param test        - Angitt predicate tar inn liste av tidligere aksepterte segmenter, samt nytt segment (som kan angi en
     *                    tom verdi) hvis gaps inkluderes.
     * @param includeGaps - hvorvidt gaps testes for seg (vil ha null som verdi).
     */

    public LocalDateTimeline<V> collect(CollectSegmentPredicate<V> test, boolean includeGaps) {

        class CollectEvaluator implements Consumer<LocalDateSegment<V>> {

            private final NavigableSet<LocalDateSegment<V>> segmenter = new TreeSet<>();
            private final NavigableSet<LocalDateSegment<V>> segmenterView = Collections.unmodifiableNavigableSet(segmenter);

            @Override
            public void accept(LocalDateSegment<V> t) {
                if (test.test(segmenterView, t, toSegments().headSet(t, false), toSegments().tailSet(t, false))) {
                    segmenter.add(t);
                }
            }

        }

        CollectEvaluator evaluator = new CollectEvaluator();
        LocalDateSegment<V> prev = segments.isEmpty() ? null : segments.first();

        for (LocalDateSegment<V> seg : segments) {
            if (includeGaps && prev != null) {
                LocalDateInterval prevInterval = prev.getLocalDateInterval();
                LocalDateInterval segInterval = seg.getLocalDateInterval();
                if (!(prevInterval.abuts(segInterval) || prevInterval.equals(segInterval))) {
                    LocalDateSegment<V> emptySegment = LocalDateSegment.emptySegment(prevInterval.getTomDato().plusDays(1),
                            segInterval.getFomDato().minusDays(1));
                    evaluator.accept(emptySegment);
                }
            }
            evaluator.accept(seg);
            prev = seg;
        }
        return new LocalDateTimeline<>(evaluator.segmenter);

    }

    /**
     * Kombinerer denne tidsserien med et enkelt segment (convenience function)
     *
     * @see #combine(LocalDateTimeline, LocalDateSegmentCombinator, JoinStyle)
     */
    public <T, R> LocalDateTimeline<R> combine(final LocalDateSegment<T> other,
                                               final LocalDateSegmentCombinator<V, T, R> combinator,
                                               final JoinStyle combinationStyle) {
        return combine(new LocalDateTimeline<T>(List.of(other)), combinator, combinationStyle);
    }

    /**
     * Kombinerer to tidslinjer, med angitt combinator funksjon og {@link JoinStyle}.
     * <p>
     * NB: Nåværende implementasjon er kun egnet for mindre datasett (feks. &lt; x1000 segmenter).
     * Spesielt join har høyt minneforbruk og O(n^2) ytelse. (potensiale for å forbedre algoritme til O(nlogn)) men øker
     * kompleksitet. Ytelsen er nær uavhengig av type {@link JoinStyle}.
     * <p>
     * beholder inntil videre for å kunne teste ny mot gammel implementasjon
     */
    <T, R> LocalDateTimeline<R> combineGammel(final LocalDateTimeline<T> other, final LocalDateSegmentCombinator<V, T, R> combinator,
                                              final JoinStyle combinationStyle) {

        // Join alle intervaller
        final NavigableMap<LocalDateInterval, Integer> joinDatoInterval = joinLocalDateIntervals(getLocalDateIntervals(),
                other.getLocalDateIntervals());

        // filtrer ut i henhold til combinationStyle
        final List<LocalDateSegment<R>> combinedSegmenter = new ArrayList<>();
        final LocalDateTimeline<V> myTidslinje = this;
        joinDatoInterval.entrySet().stream()
                .filter(e -> combinationStyle.accept((e.getValue() & LHS) > 0, (e.getValue() & RHS) > 0))
                .forEachOrdered(e -> {
                    LocalDateInterval key = e.getKey();
                    LocalDateSegment<R> nyVerdi = combinator.combine(key, myTidslinje.getSegment(key),
                            other.getSegment(key));
                    if (nyVerdi != null) {
                        combinedSegmenter.add(nyVerdi);
                    }
                });

        return new LocalDateTimeline<>(combinedSegmenter);
    }

    /**
     * Kombinerer to tidslinjer, med angitt combinator funksjon og {@link JoinStyle}.
     */
    public <T, R> LocalDateTimeline<R> combine(final LocalDateTimeline<T> other, final LocalDateSegmentCombinator<V, T, R> combinator, final JoinStyle combinationStyle) {
        List<LocalDateSegment<R>> combinedSegmenter = new ArrayList<>();
        Iterator<LocalDateSegment<V>> lhsIterator = this.segments.iterator();
        Iterator<LocalDateSegment<T>> rhsIterator = other.segments.iterator();
        LocalDateSegment<V> lhs = lhsIterator.hasNext() ? lhsIterator.next() : null;
        LocalDateSegment<T> rhs = rhsIterator.hasNext() ? rhsIterator.next() : null;
        KnekkpunktIterator<V, T> startdatoIterator = new KnekkpunktIterator<>(this.segments, other.segments);
        if (!startdatoIterator.hasNext()) {
            return empty(); //begge input-tidslinjer var tomme
        }

        long fomEpochDay = startdatoIterator.nextEpochDay();
        while (startdatoIterator.hasNext()) {
            LocalDate fom = LocalDate.ofEpochDay(fomEpochDay);
            lhs = spolTil(lhs, lhsIterator, fom);
            rhs = spolTil(rhs, rhsIterator, fom);

            boolean harLhs = lhs != null && lhs.getLocalDateInterval().contains(fom);
            boolean harRhs = rhs != null && rhs.getLocalDateInterval().contains(fom);
            long nesteFomEpochDay = startdatoIterator.nextEpochDay();
            if (combinationStyle.accept(harLhs, harRhs)) {
                LocalDateInterval periode = new LocalDateInterval(fom, LocalDate.ofEpochDay(nesteFomEpochDay - 1));
                LocalDateSegment<V> tilpassetLhsSegment = tilpassSegment(harLhs, lhs, periode, this.segmentSplitter);
                LocalDateSegment<T> tilpassetRhsSegment = tilpassSegment(harRhs, rhs, periode, other.segmentSplitter);
                LocalDateSegment<R> nyVerdi = combinator.combine(periode, tilpassetLhsSegment, tilpassetRhsSegment);
                if (nyVerdi != null) {
                    combinedSegmenter.add(nyVerdi);
                }
            }
            fomEpochDay = nesteFomEpochDay;
        }
        return new LocalDateTimeline<>(combinedSegmenter);
    }

    private static <X> LocalDateSegment<X> tilpassSegment(boolean harSegment, LocalDateSegment<X> segment, LocalDateInterval ønsketIntervall, SegmentSplitter<X> segmentSplitter) {
        if (!harSegment) {
            return null;
        }
        if (segment.getLocalDateInterval().equals(ønsketIntervall)) {
            return segment;
        }
        return segmentSplitter.apply(ønsketIntervall, segment);
    }

    /**
     * Fikser opp tidslinjen slik at tilgrensende intervaller med equal verdi får et sammenhengende intervall. Nyttig
     * for å 'redusere' tidslinjen ned til enkleste form før lagring etc.
     */
    public LocalDateTimeline<V> compress() {
        var factory = new CompressorFactory<V>(LocalDateInterval::abuts, Objects::equals, StandardCombinators::leftOnly);
        TimelineCompressor<V> compressor = segments.stream()
                .collect(factory::get, TimelineCompressor::accept, TimelineCompressor::combine);

        return new LocalDateTimeline<>(compressor.segmenter);
    }

    /**
     * Fikser opp tidslinjen ved å slå sammen sammenhengende intervall med "like" verider utover periode.
     *
     * @param e - likhetspredikat for å sammenligne to segment som vurderes slått sammen
     * @param c - combinator for å slå sammen to tids-abut-segmenter som oppfyller e
     */
    public LocalDateTimeline<V> compress(BiPredicate<V, V> e, LocalDateSegmentCombinator<V, V, V> c) {
        var factory = new CompressorFactory<>(LocalDateInterval::abuts, e, c);
        TimelineCompressor<V> compressor = segments.stream()
                .collect(factory::get, TimelineCompressor::accept, TimelineCompressor::combine);

        return new LocalDateTimeline<>(compressor.segmenter);
    }

    /**
     * Fikser opp tidslinjen ved å slå sammen sammenhengende intervall med "like" verider utover periode.
     *
     * @param a - predikat for å vurdere om to intervaller ligger inntil hverandre (helg og helligdag)
     * @param e - likhetspredikat for å sammenligne to segment som vurderes slått sammen
     * @param c - combinator for å slå sammen to tids-abut-segmenter som oppfyller e
     */
    public LocalDateTimeline<V> compress(BiPredicate<LocalDateInterval, LocalDateInterval> a, BiPredicate<V, V> e, LocalDateSegmentCombinator<V, V, V> c) {
        var factory = new CompressorFactory<>(a, e, c);
        TimelineCompressor<V> compressor = segments.stream()
                .collect(factory::get, TimelineCompressor::accept, TimelineCompressor::combine);

        return new LocalDateTimeline<>(compressor.segmenter);
    }

    /**
     * Returnerer timeline der enten denne eller andre (eller begge) har verdier.
     */
    public <T, R> LocalDateTimeline<R> crossJoin(LocalDateTimeline<T> other, LocalDateSegmentCombinator<V, T, R> combinator) {
        return combine(other, combinator, JoinStyle.CROSS_JOIN);
    }

    /**
     * Disjoint this timeline with given interval. Returns all parts of this timeline not overlapping with given
     * interval. Can be used to cut out a given interval from this timeline.
     *
     * @return new timeline without given interval.
     */
    public LocalDateTimeline<V> disjoint(LocalDateInterval datoInterval) {
        LocalDateTimeline<V> intervalTimeline = new LocalDateTimeline<>(Arrays.asList(new LocalDateSegment<V>(datoInterval, null)));
        return this.disjoint(intervalTimeline, StandardCombinators::leftOnly);
    }

    /**
     * Splitter timeline i intervaller fra #startDate inntil #endDate med angitt periode.
     * NB: vær forsiktig dersom det er åpen intervaller (tidenes ende, {@link LocalDate#MIN}, {@link LocalDate#MAX} el.
     */
    public LocalDateTimeline<V> splitAtRegular(LocalDate startDate, LocalDate endDate, Period period) {

        if (LocalDate.MIN.equals(startDate) || LocalDate.MAX.equals(endDate) || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(String.format("kan ikke periodisere tidslinjen mellom angitte datoer: [%s, %s]", startDate, endDate));
        }

        // nye segmenter
        List<LocalDateSegment<V>> segmenter = new ArrayList<>();

        var maxLocalDate = getMaxLocalDate();
        LocalDate dt = startDate;
        while (!dt.isAfter(endDate) && !dt.isAfter(maxLocalDate)) {
            LocalDate nextDt = dt.plus(period);
            // trekk 1 fra nextDt siden vi har fom/tom (ikke fom /til)
            var nesteSegmenter = intersection(new LocalDateInterval(dt, nextDt.minusDays(1))).toSegments();
            segmenter.addAll(nesteSegmenter);
            dt = nextDt;
        }

        return new LocalDateTimeline<>(segmenter);
    }

    /**
     * Returnerer kun intervaller der denne timeline har verdier, men andre ikke har det.
     */
    public <T, R> LocalDateTimeline<R> disjoint(LocalDateTimeline<T> other, final LocalDateSegmentCombinator<V, T, R> combinator) {
        return combine(other, combinator, JoinStyle.DISJOINT);
    }

    /**
     * Returnerer kun intervaller der denne timeline har verdier, men andre ikke har det.
     */
    public <T> LocalDateTimeline<V> disjoint(LocalDateTimeline<T> other) {
        final LocalDateSegmentCombinator<V, T, V> combinator = StandardCombinators::leftOnly;
        return this.disjoint(other, combinator);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        LocalDateTimeline other = (LocalDateTimeline) obj;

        // bruk HashSet til equals slik at vi slipper forstyrrelse pga segments#compareTo (som ikke tar hensyn til verdi)
        return Objects.equals(new HashSet<>(segments), new HashSet<>(other.segments));
    }

    /**
     * Filter timeline based on a predicate on value.
     */
    public LocalDateTimeline<V> filterValue(Predicate<V> predicate) {
        return collect((akseptert, ny, foregående, påfølgende) -> predicate.test(ny.getValue()), false);
    }

    /**
     * @deprecated use {@link #getLocalDateIntervals()}
     */
    @Deprecated(forRemoval = true)
    public NavigableSet<LocalDateInterval> getDatoIntervaller() {
        if (isEmpty()) {
            return Collections.emptyNavigableSet();
        }
        return segments.stream().map(LocalDateSegment::getLocalDateInterval).collect(Collectors.toCollection(TreeSet::new));
    }

    public NavigableSet<LocalDateInterval> getLocalDateIntervals() {
        if (isEmpty()) {
            return Collections.emptyNavigableSet();
        }
        return segments.stream().map(LocalDateSegment::getLocalDateInterval).collect(Collectors.toCollection(TreeSet::new));
    }


    public LocalDate getMaxLocalDate() {
        if (isEmpty()) {
            throw new IllegalStateException("Timeline is empty"); //$NON-NLS-1$
        }
        return segments.last().getTom();
    }

    public LocalDate getMinLocalDate() {
        if (isEmpty()) {
            throw new IllegalStateException("Timeline is empty"); //$NON-NLS-1$
        }
        return segments.first().getFom();
    }

    public LocalDateSegment<V> getSegment(LocalDateInterval datoInterval) {
        LocalDateSegment<V> datoSegment = findOverlappingValue(datoInterval).orElse(null);
        if (datoSegment == null || datoSegment.getLocalDateInterval().equals(datoInterval)) {
            return datoSegment;
        } else {
            return segmentSplitter.apply(datoInterval, datoSegment);
        }
    }

    @Override
    public int hashCode() {
        return segments.hashCode();
    }

    /**
     * Intersection of this timeline and given interval.
     */
    public LocalDateTimeline<V> intersection(LocalDateInterval datoInterval) {
        LocalDateTimeline<V> intervalTimeline = new LocalDateTimeline<>(Arrays.asList(new LocalDateSegment<V>(datoInterval, null)));
        return this.intersection(intervalTimeline, StandardCombinators::leftOnly);
    }

    /**
     * Kombinerer to tidsserier, tar verdier fra begge KUN dersom begge intervaller matcher. (bruker {@link JoinStyle#INNER_JOIN}.
     * Combinator må håndtere sammenstilling av verdi fra begge.
     */
    public <T, R> LocalDateTimeline<R> intersection(LocalDateTimeline<T> other, final LocalDateSegmentCombinator<V, T, R> combinator) {
        return combine(other, combinator, JoinStyle.INNER_JOIN);
    }

    /**
     * Intersection av to tidsserier, returnerer kun dennes tidsseriens verdier ved match.
     */
    public <T> LocalDateTimeline<V> intersection(LocalDateTimeline<T> other) {
        return combine(other, StandardCombinators::leftOnly, JoinStyle.INNER_JOIN);
    }

    /**
     * Intersection av to tidsserier, returnerer ene eller andres verdier (men aldri begges). Ved match returneres dennes verdi.
     */
    public LocalDateTimeline<V> crossJoin(LocalDateTimeline<V> other) {
        if (this.isEmpty()) {
            return other;
        } else if (other.isEmpty()) {
            return this;
        }
        return combine(other, StandardCombinators::coalesceLeftHandSide, JoinStyle.CROSS_JOIN);
    }

    public <T> boolean intersects(LocalDateTimeline<T> other) {
        LocalDateTimeline<V> intersection = intersection(other, (LocalDateInterval di, LocalDateSegment<V> v, LocalDateSegment<T> t) -> v);
        return !intersection.isEmpty();
    }

    /**
     * Kombinerer to tidsserier, tar verdier fra begge uansett om intervaller matcher. (bruker {@link JoinStyle#CROSS_JOIN}.
     * Combinator må håndtere sammenstilling av verdi ved overlapp og dersom ene eller andre har verdi.
     */
    public <T, R> LocalDateTimeline<R> union(LocalDateTimeline<T> other, final LocalDateSegmentCombinator<V, T, R> combinator) {
        return combine(other, combinator, JoinStyle.CROSS_JOIN);
    }

    /**
     * Whether this timeline is continuous and not empty for its entire interval.
     *
     * @return true if continuous
     */
    public boolean isContinuous() {
        return isContinuous(LocalDateInterval::abuts);
    }

    public boolean isContinuous(BiPredicate<LocalDateInterval, LocalDateInterval> abuts) {
        return firstDiscontinuity(abuts) == null;
    }

    /**
     * Return the first interval, if any, not covered by the timeline. Utility when full disjoint is not needed
     *
     * @return null if continuous, first interval
     */
    public LocalDateInterval firstDiscontinuity() {
        return firstDiscontinuity(LocalDateInterval::abuts);
    }

    public LocalDateInterval firstDiscontinuity(BiPredicate<LocalDateInterval, LocalDateInterval> abuts) {
        if (segments.size() == 1) {
            return null;
        }

        LocalDateInterval lastInterval = null;
        for (LocalDateSegment<V> entry : segments) {
            if (lastInterval != null) {
                if (!abuts.test(lastInterval, entry.getLocalDateInterval())) {
                    return new LocalDateInterval(lastInterval.getTomDato().plusDays(1), entry.getLocalDateInterval().getFomDato().minusDays(1));
                }
            }
            lastInterval = entry.getLocalDateInterval();
        }
        return null;
    }

    /**
     * Whether this timeline is continuous in the given interval.
     *
     * @return true if continuous and not empty for whole matchInterval
     */
    public boolean isContinuous(LocalDateInterval matchInterval) {
        LocalDateTimeline<V> intersection = this.intersection(matchInterval);
        return !intersection.isEmpty() && intersection.isContinuous()
                && Objects.equals(intersection.getMinLocalDate(), matchInterval.getFomDato())
                && Objects.equals(intersection.getMaxLocalDate(), matchInterval.getTomDato());
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    /**
     * Map each segment to one or more segments. Can be used to split a timeline for example by a custom function.
     */
    public <R> LocalDateTimeline<R> map(Function<LocalDateSegment<V>, List<LocalDateSegment<R>>> mapper) {
        NavigableSet<LocalDateSegment<R>> newSegments = new TreeSet<>();
        segments.forEach(s -> newSegments.addAll(mapper.apply(s)));
        return new LocalDateTimeline<R>(newSegments);
    }

    /**
     * Map each value to a new value given the specified mapping function.
     */
    public <R> LocalDateTimeline<R> mapValue(Function<V, R> mapper) {
        NavigableSet<LocalDateSegment<R>> newSegments = new TreeSet<>();
        segments.forEach(s -> newSegments.add(new LocalDateSegment<R>(s.getLocalDateInterval(), mapper.apply(s.getValue()))));
        return new LocalDateTimeline<>(newSegments);
    }

    /**
     * Map each value to a new value given the specified mapping function.
     */
    public <R> LocalDateTimeline<R> mapSegment(Function<V, R> mapper) {
        NavigableSet<LocalDateSegment<R>> newSegments = new TreeSet<>();
        segments.forEach(s -> newSegments.add(new LocalDateSegment<R>(s.getLocalDateInterval(), mapper.apply(s.getValue()))));
        return new LocalDateTimeline<>(newSegments);
    }

    /**
     * Reduce timeline to a given value. May be used to accumulate data such a sum of all values.
     */
    public <R> Optional<R> reduce(Reducer<V, R> reducer) {
        AtomicReference<R> result = new AtomicReference<>(null);
        segments.forEach(d -> result.set(reducer.reduce(result.get(), d)));
        return Optional.ofNullable(result.get());
    }

    /**
     * Number of segments in timeline.
     */
    public int size() {
        return segments.size();
    }

    /**
     * Get defensive copy of all segments.
     */
    public NavigableSet<LocalDateSegment<V>> toSegments() {
        return Collections.unmodifiableNavigableSet(segments);
    }

    @Override
    public Iterator<LocalDateSegment<V>> iterator() {
        return toSegments().iterator();
    }

    public Stream<LocalDateSegment<V>> stream() {
        return segments.stream();
    }

    /**
     * Find timeline of unique segments in collection of segments that may overlap.
     */
    public static <V> LocalDateTimeline<List<V>> buildGroupOverlappingSegments(Collection<LocalDateSegment<V>> segmentsWithPossibleOverlaps) {
        @SuppressWarnings({"cast"})
        var uniqueSegments = segmentsWithPossibleOverlaps.stream().map(s -> new LocalDateSegment<>(s.getLocalDateInterval(), (List<V>) new ArrayList<V>()))
                .collect(Collectors.toList());

        var uniqueIntervalTimeline = new LocalDateTimeline<>(uniqueSegments, (interval, lhs, rhs) -> new LocalDateSegment<>(interval, new ArrayList<V>()), LocalDateTimeline::createNewListOnSplit);

        for (var per : uniqueIntervalTimeline.toSegments()) {
            for (var seg : segmentsWithPossibleOverlaps) {
                if (seg.getLocalDateInterval().overlaps(per.getLocalDateInterval())) {
                    per.getValue().add(seg.getValue());
                }
            }
        }
        return uniqueIntervalTimeline;
    }

    private static <V> LocalDateSegment<List<V>> createNewListOnSplit(LocalDateInterval di, LocalDateSegment<List<V>> seg) {
        if (di.equals(seg.getLocalDateInterval())) {
            return seg;
        }
        return new LocalDateSegment<>(di, new ArrayList<>(seg.getValue()));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
                (isEmpty() ? "0" //$NON-NLS-1$
                        : getMinLocalDate() + ", " + getMaxLocalDate()) //$NON-NLS-1$
                + " [" + size() + "]" //$NON-NLS-1$ // $NON-NLS-2$
                + "> = [" + getLocalDateIntervals().stream().map(String::valueOf).collect(Collectors.joining(",")) + "]" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                ;
    }

    /**
     * Add segment to timeline. Validate overlapping. If overlap found use given combinator to handle, or abort if not specified.
     */
    private boolean add(LocalDateSegment<V> datoSegment, LocalDateSegmentCombinator<V, V, V> overlapCombinator) {
        if (segments.isEmpty()) {
            // tomt sett
            segments.add(datoSegment);
            return true;
        } else {
            LocalDateInterval datoInterval = datoSegment.getLocalDateInterval();
            if (this.isTimelineOutsideInterval(datoInterval)) {
                segments.add(datoSegment);
                return true;
            } else {
                if (!isTimelineOverlappingWithSegment(datoSegment)) {
                    segments.add(datoSegment);
                } else if (overlapCombinator == null) {
                    throw new IllegalArgumentException(String.format("Overlapp %s: %s", datoSegment, this));
                } else {
                    addWhenOverlap(datoSegment, overlapCombinator, datoInterval);
                }
                return true;
            }
        }

    }

    /**
     * Quick check to see if interval is completely outside timeline. (avoids testing each segment)
     */
    public boolean isTimelineOutsideInterval(LocalDateInterval datoInterval) {
        return datoInterval.getFomDato().isAfter(this.getMaxLocalDate()) || datoInterval.getTomDato().isBefore(this.getMinLocalDate());
    }

    private void addWhenOverlap(LocalDateSegment<V> datoSegment, LocalDateSegmentCombinator<V, V, V> overlapCombinator, LocalDateInterval datoInterval) {
        List<LocalDateSegment<V>> newSegments = new ArrayList<>();

        for (LocalDateSegment<V> segEntry : segments) {
            if (segEntry.overlapper(datoSegment)) {
                LocalDateInterval segInterval = segEntry.getLocalDateInterval();

                // handle intervals which exist but do not overlap with new
                segInterval.except(datoInterval)
                        .forEach(di -> newSegments.add(segmentSplitter.apply(di, segEntry)));

                // handle gap in existing series when new has a value
                handleGapInExistingTimeline(datoSegment, datoInterval, newSegments, segEntry);

                // handle overlap between existing and new value
                Optional<LocalDateInterval> segOverlap = segInterval.overlap(datoInterval);
                LocalDateInterval segOverlappInterval = segOverlap.orElseThrow(() -> new IllegalArgumentException(
                        String.format("Utvikler-feil: intervall overlapper ikke : %s - %s", segInterval, datoInterval)));

                newSegments.add(overlapCombinator.combine(segOverlappInterval, segEntry, datoSegment));
            } else {
                newSegments.add(segEntry);
            }
        }
        segments.clear();
        segments.addAll(newSegments);
    }

    private void handleGapInExistingTimeline(LocalDateSegment<V> datoSegment, LocalDateInterval datoInterval, List<LocalDateSegment<V>> newSegments,
                                             LocalDateSegment<V> segEntry) {
        LocalDateSegment<V> nesteSegment = segments.higher(segEntry);
        LocalDateSegment<V> forrigeSegment = segments.lower(segEntry);

        LocalDateInterval gapBak = null;
        LocalDateInterval gapForan = null;
        if (nesteSegment == null && datoInterval.getTomDato().isAfter(segEntry.getTom())) {
            gapBak = new LocalDateInterval(segEntry.getTom().plusDays(1), datoInterval.getTomDato());
        } else if (nesteSegment != null && !nesteSegment.getFom().isEqual(segEntry.getTom().plusDays(1))) {
            gapBak = new LocalDateInterval(segEntry.getTom().plusDays(1), nesteSegment.getFom().minusDays(1));
        }

        if (forrigeSegment == null && datoInterval.getFomDato().isBefore(segEntry.getFom())) {
            gapForan = new LocalDateInterval(datoInterval.getFomDato(), segEntry.getFom().minusDays(1));
        } else if (forrigeSegment != null && !forrigeSegment.getTom().isEqual(segEntry.getFom().minusDays(1))) {
            gapForan = new LocalDateInterval(forrigeSegment.getTom().plusDays(1), segEntry.getFom().minusDays(1));
        }

        if (gapBak != null) {
            Optional<LocalDateInterval> overlap = datoInterval.overlap(gapBak);
            overlap.ifPresent(di -> newSegments.add(new LocalDateSegment<V>(di, datoSegment.getValue())));
        }
        if (gapForan != null) {
            Optional<LocalDateInterval> overlap = datoInterval.overlap(gapForan);
            overlap.ifPresent(di -> newSegments.add(new LocalDateSegment<V>(di, datoSegment.getValue())));
        }
    }

    /**
     * Get list of unique intervals, handling overlaps if necessary.
     */
    private NavigableSet<LocalDateInterval> combineAllUniqueIntervals(NavigableSet<LocalDateInterval> lhsIntervaller,
                                                                      NavigableSet<LocalDateInterval> rhsIntervaller) {

        if (lhsIntervaller.isEmpty()) {
            return rhsIntervaller;
        } else if (rhsIntervaller.isEmpty()) {
            return lhsIntervaller;
        }

        List<LocalDateInterval> unmergedIntervals = new ArrayList<>(lhsIntervaller);
        unmergedIntervals.addAll(rhsIntervaller);

        NavigableSet<LocalDateInterval> result = new TreeSet<>();

        List<LocalDateInterval> intervals = new ArrayList<>(unmergedIntervals);
        Collections.sort(intervals, LocalDateInterval.ORDER_INTERVALS);

        for (LocalDateInterval interval : intervals) {
            if (result.isEmpty()) {
                result.add(interval);
            } else {

                List<LocalDateInterval> overlapped = new ArrayList<>();
                List<LocalDateInterval> split = result.stream()
                        .filter(i -> {
                            boolean ret = i.overlaps(interval);
                            if (ret) {
                                overlapped.add(i);
                            }
                            return ret;
                        })
                        .flatMap(i -> i.splitAll(interval).stream())
                        .collect(Collectors.toList());

                if (split.isEmpty()) {
                    result.add(interval);
                } else {
                    result.removeAll(overlapped);
                    result.addAll(split);
                }

            }

        }

        return result;
    }

    private Optional<LocalDateSegment<V>> findOverlappingValue(LocalDateInterval datoInterval) {
        if (isEmpty()) {
            return Optional.empty();
        }
        return segments.stream().filter(s -> s.getLocalDateInterval().overlaps(datoInterval)).findFirst();
    }

    private boolean isTimelineOverlappingWithSegment(LocalDateSegment<V> datoSegment) {
        if (isEmpty()) {
            return false;
        }
        // sjekk for overlapp
        Optional<LocalDateSegment<V>> overlappingSegment = findOverlappingValue(datoSegment.getLocalDateInterval());

        return (overlappingSegment.isPresent());
    }

    private NavigableMap<LocalDateInterval, Integer> joinLocalDateIntervals(NavigableSet<LocalDateInterval> lhsIntervaller,
                                                                            NavigableSet<LocalDateInterval> rhsIntervaller) {

        NavigableMap<LocalDateInterval, Integer> joined = new TreeMap<>();

        NavigableSet<LocalDateInterval> alleIntervaller = combineAllUniqueIntervals(lhsIntervaller, rhsIntervaller);

        for (LocalDateInterval datoInterval : alleIntervaller) {
            boolean lhsMatch = lhsIntervaller.stream().anyMatch(lhs -> lhs.contains(datoInterval));
            boolean rhsMatch = rhsIntervaller.stream().anyMatch(rhs -> rhs.contains(datoInterval));

            int combinedFlags = (lhsMatch ? LHS : 0) | (rhsMatch ? RHS : 0);

            if (combinedFlags != 0) {
                joined.put(datoInterval, combinedFlags);
            }
        }

        return joined;
    }

    /**
     * Finner alle knekkpunkter fra to tidslinjer, i sekvens.
     * <p>
     * Knekkpunkter er 'start av et intervall' og 'dagen etter slutt av et intervall'. Sistnevnte fordi det da kan være starten på et nytt intervall
     * <p>
     * Hvis slutt av intervall er LocalDate.MAX, er dagen etter ikke representerbar i LocalDate, derfor bruker denne klassen epochDay istedet
     */
    private static class KnekkpunktIterator<V, T> {
        private final Iterator<LocalDateSegment<V>> lhsIterator;
        private final Iterator<LocalDateSegment<T>> rhsIterator;
        private LocalDateSegment<V> lhsSegment;
        private LocalDateSegment<T> rhsSegment;
        private Long next;

        public KnekkpunktIterator(NavigableSet<LocalDateSegment<V>> lhsIntervaller, NavigableSet<LocalDateSegment<T>> rhsIntervaller) {
            lhsIterator = lhsIntervaller.iterator();
            rhsIterator = rhsIntervaller.iterator();
            lhsSegment = lhsIterator.hasNext() ? lhsIterator.next() : null;
            rhsSegment = rhsIterator.hasNext() ? rhsIterator.next() : null;

            next = lhsSegment != null ? lhsSegment.getFom().toEpochDay() : null;
            if (rhsSegment != null && (next == null || rhsSegment.getFom().toEpochDay() < next)) {
                next = rhsSegment.getFom().toEpochDay();
            }
        }

        public long nextEpochDay() {
            if (next == null) {
                throw new NoSuchElementException("Ikke flere verdier igjen");
            }
            long denne = next;
            oppdaterNeste();
            return denne;
        }

        public boolean hasNext() {
            return next != null;
        }

        private void oppdaterNeste() {
            spolUnderliggendeIteratorer();

            Long forrige = next;
            next = null;
            //neste knekkpunkt kan komme fra hvilken som helst av de to tidsseriene, må sjekke begge
            oppdaterNeste(forrige, lhsSegment);
            oppdaterNeste(forrige, rhsSegment);
        }

        private void spolUnderliggendeIteratorer() {
            while (lhsSegment != null && lhsSegment.getTom().toEpochDay() < next) {
                lhsSegment = lhsIterator.hasNext() ? lhsIterator.next() : null;
            }
            while (rhsSegment != null && rhsSegment.getTom().toEpochDay() < next) {
                rhsSegment = rhsIterator.hasNext() ? rhsIterator.next() : null;
            }
        }

        private <X> void oppdaterNeste(Long forrige, LocalDateSegment<X> segment) {
            if (segment == null) {
                return; //underliggende iterator er tømt
            }
            long fomKandidat = segment.getFom().toEpochDay();
            if (fomKandidat > forrige && (next == null || fomKandidat < next)) {
                next = fomKandidat;
            } else {
                long tomKandidat = segment.getTom().toEpochDay() + 1;
                if (tomKandidat > forrige && (next == null || tomKandidat < next)) {
                    next = tomKandidat;
                }
            }
        }
    }

    private static <V> LocalDateSegment<V> spolTil(LocalDateSegment<V> intervall, Iterator<LocalDateSegment<V>> iterator, LocalDate fom) {
        while (intervall != null && intervall.getTom().isBefore(fom)) {
            intervall = iterator.hasNext() ? iterator.next() : null;
        }
        return intervall;
    }

    private void validateNonOverlapping() {
        LocalDateSegment<V> last = null;
        for (LocalDateSegment<V> seg : segments) {
            if (last != null) {
                if (seg.overlapper(last)) {
                    throw new IllegalArgumentException(String.format("Overlapp %s - %s", last, seg));
                }
            }
            last = seg;
        }
    }

    /**
     * Standard join typer for å kombinere to tidsserier.
     */
    public enum JoinStyle {
        /**
         * Ene eller andre har verdi.
         */
        CROSS_JOIN {
            @Override
            public boolean accept(boolean harLhs, boolean harRhs) {
                return harLhs || harRhs;
            }
        },
        /**
         * kun venstre tidsserie.
         */
        DISJOINT {
            @Override
            public boolean accept(boolean harLhs, boolean harRhs) {
                return harLhs && !harRhs;
            }
        },
        /**
         * kun dersom begge tidsserier har verdi.
         */
        INNER_JOIN {
            @Override
            public boolean accept(boolean harLhs, boolean harRhs) {
                return harLhs && harRhs;
            }

        },
        /**
         * alltid venstre tidsserie (LHS), høyre (RHS) kun med verdi dersom matcher. Combinator funksjon må hensyn ta
         * nulls for RHS.
         */
        LEFT_JOIN {
            @Override
            public boolean accept(boolean harLhs, boolean harRhs) {
                return harLhs;
            }

        },
        /**
         * alltid høyre side (RHS), venstre kun med verdi dersom matcher. Combinator funksjon må hensyn ta nulls for
         * LHS.
         */
        RIGHT_JOIN {
            @Override
            public boolean accept(boolean harLhs, boolean harRhs) {
                return harRhs;
            }
        };

        public abstract boolean accept(boolean harLhs, boolean harRhs);

    }

    /**
     * Brukes sammen collector for å vurdere hvilke segmenter som skal returneres.
     */
    public interface CollectSegmentPredicate<V> {

        boolean test(NavigableSet<LocalDateSegment<V>> akseptertTilNå, LocalDateSegment<V> segmentUnderVurdering,
                     NavigableSet<LocalDateSegment<V>> alleForegående, NavigableSet<LocalDateSegment<V>> allePåfølgende);

    }

    /**
     * Reduce a timeline to single output.
     */
    public interface Reducer<V, R> {
        R reduce(R aggregateValue, LocalDateSegment<V> nextSegment);
    }

    /**
     * Interface for å custom funksjon for å splitte segmenter.
     */
    public interface SegmentSplitter<V> extends BiFunction<LocalDateInterval, LocalDateSegment<V>, LocalDateSegment<V>> {
    }

    private static final class EqualValueSegmentSplitter<V> implements SegmentSplitter<V>, Serializable {
        @Override
        public LocalDateSegment<V> apply(LocalDateInterval di, LocalDateSegment<V> seg) {
            if (di.equals(seg.getLocalDateInterval())) {
                return seg;
            } else {
                return new LocalDateSegment<V>(di, seg.getValue());
            }
        }
    }

    /**
     * Kombinerer løpende sammenhengende intervaller med samme verdi. Forutsetter en timeline der ingen segmenter er
     * overlappende (hvilket aldri skal kunne skje).
     */
    static class TimelineCompressor<V> implements Consumer<LocalDateSegment<V>> {

        private final NavigableSet<LocalDateSegment<V>> segmenter = new TreeSet<>();
        private final BiPredicate<LocalDateInterval, LocalDateInterval> abuts;
        private final BiPredicate<V, V> equals;
        private final LocalDateSegmentCombinator<V, V, V> combinator;

        TimelineCompressor(BiPredicate<LocalDateInterval, LocalDateInterval> a, BiPredicate<V, V> e, LocalDateSegmentCombinator<V, V, V> c) {
            this.abuts = a;
            this.equals = e;
            this.combinator = c;
        }

        @Override
        public void accept(LocalDateSegment<V> t) {
            if (segmenter.isEmpty()) {
                segmenter.add(t);
            } else {
                LocalDateSegment<V> last = segmenter.last();
                if (abuts.test(last.getLocalDateInterval(), t.getLocalDateInterval())
                        && equals.test(last.getValue(), t.getValue())) {
                    // bytt ut og ekspander intervall for siste
                    segmenter.remove(last);
                    LocalDateInterval expandedInterval = last.getLocalDateInterval().expand(t.getLocalDateInterval(), abuts);
                    segmenter.add(new LocalDateSegment<>(expandedInterval, combinator.combine(expandedInterval, last, t).getValue()));
                } else {
                    segmenter.add(t);
                }
            }
        }

        public void combine(@SuppressWarnings("unused") TimelineCompressor<V> other) {
            throw new UnsupportedOperationException("Ikke implementert, men påkrevd av Stream#collect for parallell collect"); //$NON-NLS-1$
        }

    }

    static class CompressorFactory<V> {
        private final BiPredicate<LocalDateInterval, LocalDateInterval> abuts;
        private final BiPredicate<V, V> equals;
        private final LocalDateSegmentCombinator<V, V, V> combinator;

        CompressorFactory(BiPredicate<LocalDateInterval, LocalDateInterval> a, BiPredicate<V, V> e, LocalDateSegmentCombinator<V, V, V> c) {
            this.abuts = a;
            this.equals = e;
            this.combinator = c;
        }

        TimelineCompressor<V> get() {
            return new TimelineCompressor<>(abuts, equals, combinator);
        }
    }

}
