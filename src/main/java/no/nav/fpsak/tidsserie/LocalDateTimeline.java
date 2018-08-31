package no.nav.fpsak.tidsserie;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
 * De fleste algoritmene for å kombinere to tidsserier er O(n^2) og ikke optimale for sammenstilling av 1000+ verdier.
 * <p>
 * API'et er modellert etter metoder fra java.time og threeten-extra Interval.
 *
 * @param <V> Java type for verdier i tidsserien.
 */
@JsonSerialize(using = LocalDateTimelineFormatters.Serializer.class)
@JsonDeserialize(using = LocalDateTimelineFormatters.Deserializer.class)
public class LocalDateTimeline<V> implements Serializable {

    @SuppressWarnings("rawtypes")
    public static final LocalDateTimeline EMPTY_TIMELINE = new LocalDateTimeline<>(Collections.emptyList());
    /**
     * Left-hand side, konstant for håndtere joins mellom tidsserier.
     */
    private static int LHS = 1;
    /**
     * Right-hand side.
     */
    private static int RHS = LHS << 1;
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
        for (LocalDateSegment<V> ds : datoSegmenter) {
            add(ds, overlapCombinator);
        }
        this.segmentSplitter = customSegmentSplitter != null ? customSegmentSplitter : new SegmentSplitter<>();
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
     * Kombinerer to tidslinjer, med angitt combinator funksjon og {@link JoinStyle}.
     * <p>
     * NB: Nåværende implementasjon er kun egnet for mindre datasett (feks. &lt; x1000 segmenter).
     * Spesielt join har høyt minneforbruk og O(n^2) ytelse. (potensiale for å forbedre algoritme til O(nlogn)) men øker
     * kompleksitet. Ytelsen er nær uavhengig av type {@link JoinStyle}.
     */
    public <T, R> LocalDateTimeline<R> combine(final LocalDateTimeline<T> other, final LocalDateSegmentCombinator<V, T, R> combinator,
                                               final JoinStyle combinationStyle) {

        if (skipNonMatchingInnerJoin(other, combinationStyle)) {
            // rask exit dersom intersetion og ingen sjanse for overlap
            return new LocalDateTimeline<>(Collections.emptyList());
        }

        // Join alle intervaller
        final NavigableMap<LocalDateInterval, Integer> joinDatoInterval = joinLocalDateIntervals(getDatoIntervaller(),
                other.getDatoIntervaller());

        // filtrer ut i henhold til combinationStyle
        final List<LocalDateSegment<R>> combinedSegmenter = new ArrayList<>();
        final LocalDateTimeline<V> myTidslinje = this;
        joinDatoInterval.entrySet().stream()
                .filter(e -> combinationStyle.accept(e.getValue()))
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
     * Fikser opp tidslinjen slik at tilgrensende intervaller med equal verdi får et sammenhengende intervall. Nyttig
     * for å 'redusere' tidslinjen ned til enkleste form før lagring etc.
     */
    public LocalDateTimeline<V> compress() {
        TimelineCompressor<V> compressor = segments.stream()
                .collect(TimelineCompressor::new, TimelineCompressor::accept, TimelineCompressor::combine);

        return new LocalDateTimeline<>(compressor.segmenter);
    }

    /**
     * Returnerer timeline der enten denne eller andre har verdier.
     */
    public <T, R> LocalDateTimeline<R> crossJoin(LocalDateTimeline<T> other, LocalDateSegmentCombinator<V, T, R> combinator) {
        return combine(other, combinator, JoinStyle.CROSS_JOIN);
    }

    /**
     * Disjoint this timeline with given interval. Returns all parts of this timeline not overlapping with given
     * interval.
     */
    public LocalDateTimeline<V> disjoint(LocalDateInterval datoInterval) {
        LocalDateTimeline<V> intervalTimeline = new LocalDateTimeline<>(Arrays.asList(new LocalDateSegment<V>(datoInterval, null)));
        return this.disjoint(intervalTimeline, StandardCombinators::leftOnly);
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

    public NavigableSet<LocalDateInterval> getDatoIntervaller() {
        if (isEmpty()) {
            return Collections.emptyNavigableSet();
        }
        return segments.stream().map(s -> s.getLocalDateInterval()).collect(Collectors.toCollection(TreeSet::new));
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
     * @return true if continuous and not empty for whole matchInterval
     */
    public boolean isContinuous() {
        if (segments.size() == 1) {
            return true;
        }

        LocalDateInterval lastInterval = null;
        for (LocalDateSegment<V> entry : segments) {
            if (lastInterval != null) {
                if (!lastInterval.abuts(entry.getLocalDateInterval())) {
                    return false;
                }
            }
            lastInterval = entry.getLocalDateInterval();
        }
        return true;
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
        segments.forEach(d -> {
            result.set(reducer.reduce(result.get(), d));
        });
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
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
                (isEmpty() ? "0" //$NON-NLS-1$
                        : getMinLocalDate() + ", " + getMaxLocalDate()) //$NON-NLS-1$
                + " [" + size() + "]" //$NON-NLS-1$ // $NON-NLS-2$
                + "> = [" + getDatoIntervaller().stream().map(d -> String.valueOf(d)).collect(Collectors.joining(",")) + "]" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
        return !(datoInterval.getFomDato().isBefore(this.getMaxLocalDate().plusDays(1))
                && datoInterval.getTomDato().isAfter(this.getMinLocalDate().minusDays(1)));
    }

    private void addWhenOverlap(LocalDateSegment<V> datoSegment, LocalDateSegmentCombinator<V, V, V> overlapCombinator,
                                LocalDateInterval datoInterval) {
        List<LocalDateSegment<V>> newSegments = new ArrayList<>();

        for (LocalDateSegment<V> segEntry : segments) {
            if (segEntry.overlapper(datoSegment)) {
                LocalDateInterval segInterval = segEntry.getLocalDateInterval();

                // håndter intervaller som eksisterer fra før, men ikke overlapper nytt
                segInterval.except(datoInterval)
                        .forEach(di -> newSegments.add(new LocalDateSegment<>(di, segEntry.getValue())));

                // håndter gap i eksisterende serie der ny har verdi
                håndterGapIEksisterende(datoSegment, datoInterval, newSegments, segEntry);

                // håndter overlapp mellom eksisterende serie og ny verdi
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

    private void håndterGapIEksisterende(LocalDateSegment<V> datoSegment, LocalDateInterval datoInterval, List<LocalDateSegment<V>> newSegments, LocalDateSegment<V> segEntry) {
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

    private <T> boolean skipNonMatchingInnerJoin(final LocalDateTimeline<T> other, final JoinStyle combinationStyle) {
        return JoinStyle.INNER_JOIN == combinationStyle
                && ((this.isEmpty() || other.isEmpty()) || !new LocalDateInterval(this.getMinLocalDate(), this.getMaxLocalDate())
                .overlaps(new LocalDateInterval(other.getMinLocalDate(), other.getMaxLocalDate())));
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
            public boolean accept(int option) {
                return option > 0;
            }
        },
        /**
         * kun venstre tidsserie.
         */
        DISJOINT {
            @Override
            public boolean accept(int option) {
                return option == LHS;
            }
        },
        /**
         * kun dersom begge tidsserier har verdi.
         */
        INNER_JOIN {
            @Override
            public boolean accept(int option) {
                return (option & LHS) == LHS && (option & RHS) == RHS;
            }
        },
        /**
         * alltid venstre tidsserie (LHS), høyre (RHS) kun med verdi dersom matcher. Combinator funksjon må hensyn ta
         * nulls for RHS.
         */
        LEFT_JOIN {
            @Override
            public boolean accept(int option) {
                return (option & LHS) == LHS;
            }
        },
        /**
         * alltid høyre side (RHS), venstre kun med verdi dersom matcher. Combinator funksjon må hensyn ta nulls for
         * LHS.
         */
        RIGHT_JOIN {
            @Override
            public boolean accept(int option) {
                return (option & RHS) == RHS;
            }
        };

        public abstract boolean accept(int option);
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

    private static final class SegmentSplitter<V>
            implements BiFunction<LocalDateInterval, LocalDateSegment<V>, LocalDateSegment<V>>, Serializable {
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

        @Override
        public void accept(LocalDateSegment<V> t) {
            if (segmenter.isEmpty()) {
                segmenter.add(t);
            } else {
                LocalDateSegment<V> last = segmenter.last();
                if (last.getLocalDateInterval().abuts(t.getLocalDateInterval())
                        && Objects.equals(last.getValue(), t.getValue())) {
                    // bytt ut og ekspander intervall for siste
                    segmenter.remove(last);
                    LocalDateInterval expandedInterval = last.getLocalDateInterval().expand(t.getLocalDateInterval());
                    segmenter.add(new LocalDateSegment<V>(expandedInterval, last.getValue()));
                } else {
                    segmenter.add(t);
                }
            }
        }

        public void combine(@SuppressWarnings("unused") TimelineCompressor<V> other) {
            throw new UnsupportedOperationException("Ikke implementert, men påkrevd av Stream#collect for parallell collect"); //$NON-NLS-1$
        }

    }

}
