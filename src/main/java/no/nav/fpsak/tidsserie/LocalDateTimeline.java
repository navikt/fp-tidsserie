package no.nav.fpsak.tidsserie;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
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
    private static final SegmentSplitter<Object> DEFAULT_SEGMENT_SPLITTER = new DefaultSegmentSplitter<>();

    // Invariant: Er sortert etter fom-dato. Ingen overlappende segment.
    private final ArrayList<LocalDateSegment<V>> segments;
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
        this.segmentSplitter = customSegmentSplitter != null ? customSegmentSplitter : (SegmentSplitter<V>) DEFAULT_SEGMENT_SPLITTER;
        if (erCollectionSortertOgIkkeOverlappende(datoSegmenter)) {
            //veldig vanlig tilfelle. skjer bl.a når resultatet fra combine-metoden dannes
            segments = new ArrayList<>(datoSegmenter);
        } else {
            segments = splittOgHåndterOverlapp(datoSegmenter, segmentSplitter, overlapCombinator);
            validateNonOverlapping();
        }
    }

    private static <V> ArrayList<LocalDateSegment<V>> splittOgHåndterOverlapp(Collection<LocalDateSegment<V>> inputSegmenter, SegmentSplitter<V> segmentSplitter, LocalDateSegmentCombinator<V, V, V> overlapCombinator) {
        ArrayList<LocalDateSegment<V>> resultat = new ArrayList<>();
        ArrayList<LocalDateSegment<V>> inputListe = inputSegmenter instanceof ArrayList<LocalDateSegment<V>> inputSegmentListe ? inputSegmentListe : new ArrayList<>(inputSegmenter);
        NavigableMap<LocalDate, List<Integer>> segmenterPrStartdato = grupperSegmenter(inputListe, LocalDateSegment::getFom);
        NavigableMap<LocalDate, List<Integer>> segmenterPrSluttdato = grupperSegmenter(inputListe, LocalDateSegment::getTom);

        NavigableSet<Integer> aktiveSegmenter = new TreeSet<>(); //må være sortert for å bevare rekkfølge av input-segmenter ved kombinering
        KnekkpunktIterator knekkpunktIterator = new KnekkpunktIterator(segmenterPrStartdato.navigableKeySet(), segmenterPrSluttdato.navigableKeySet());
        LocalDate fom = knekkpunktIterator.next();
        while (fom != null && knekkpunktIterator.hasNext()) {
            boolean pastEndOfTime = knekkpunktIterator.nextIsPastEndOfTime();
            LocalDate nesteFom = pastEndOfTime ? null : knekkpunktIterator.next();
            LocalDate tom = pastEndOfTime ? LocalDate.MAX : nesteFom.minusDays(1);

            if (!aktiveSegmenter.isEmpty()) {
                segmenterPrSluttdato.getOrDefault(fom.minusDays(1), List.of()).forEach(aktiveSegmenter::remove);
            }
            aktiveSegmenter.addAll(segmenterPrStartdato.getOrDefault(fom, List.of()));
            if (!aktiveSegmenter.isEmpty()) {
                resultat.add(lagSegment(inputListe, aktiveSegmenter, fom, tom, segmentSplitter, overlapCombinator));
            }
            fom = nesteFom;
        }
        return resultat;
    }

    private static <V> NavigableMap<LocalDate, List<Integer>> grupperSegmenter(ArrayList<LocalDateSegment<V>> segmenter, Function<LocalDateSegment<V>, LocalDate> grupperPå) {
        NavigableMap<LocalDate, List<Integer>> grupperte = new TreeMap<>();

        for (int i = 0; i < segmenter.size(); i++) {
            LocalDateSegment<V> segment = segmenter.get(i);
            LocalDate nøkkel = grupperPå.apply(segment);
            List<Integer> startListe = grupperte.get(nøkkel);
            if (startListe != null) {
                startListe.add(i);
            } else {
                List<Integer> lista = new ArrayList<>();
                lista.add(i);
                grupperte.put(nøkkel, lista);
            }
        }
        return grupperte;
    }

    static final class KnekkpunktIterator {
        private Iterator<LocalDate> startIterator;
        private Iterator<LocalDate> sluttIterator;

        private LocalDate nesteStart;
        private LocalDate nesteSlutt;

        public KnekkpunktIterator(Iterator<LocalDate> startIterator, Iterator<LocalDate> sluttIterator) {
            this.startIterator = startIterator;
            this.sluttIterator = sluttIterator;

            nesteStart = startIterator.hasNext() ? startIterator.next() : null;
            nesteSlutt = sluttIterator.hasNext() ? sluttIterator.next() : null;
        }

        public KnekkpunktIterator(NavigableSet<LocalDate> startdatoer, NavigableSet<LocalDate> sluttdatoer) {
            this(startdatoer.iterator(), sluttdatoer.iterator());
        }

        public boolean nextIsPastEndOfTime() {
            boolean harFlereStarttidspunkt = startIterator.hasNext();
            boolean harFlereSluttidspunktFørTidenesEnde = sluttIterator.hasNext() || (!nesteSlutt.equals(LocalDate.MAX));
            return !harFlereStarttidspunkt && !harFlereSluttidspunktFørTidenesEnde;
        }

        public LocalDate next() {
            if (nesteSlutt == null && nesteStart == null) {
                throw new NoSuchElementException("Ikke flere verdier igjen");
            }
            boolean velgStartVerdi = nesteStart != null && (nesteSlutt == null || !nesteStart.isAfter(nesteSlutt));
            LocalDate valgtDato = velgStartVerdi ? nesteStart : nesteSlutt.plusDays(1);
            if (nesteStart != null && !nesteStart.isAfter(valgtDato)) {
                nesteStart = startIterator.hasNext() ? startIterator.next() : null;
            }
            if (nesteSlutt != null && nesteSlutt.isBefore(valgtDato)) {
                nesteSlutt = sluttIterator.hasNext() ? sluttIterator.next() : null;
            }
            return valgtDato;
        }

        public boolean hasNext() {
            return nesteStart != null || nesteSlutt != null;
        }

    }

    private static <V> LocalDateSegment<V> lagSegment(ArrayList<LocalDateSegment<V>> inputListe, NavigableSet<Integer> aktiveSegmenter, LocalDate fom, LocalDate tom, SegmentSplitter<V> segmentSplitter, LocalDateSegmentCombinator<V, V, V> overlapCombinator) {
        if (aktiveSegmenter.size() > 1 && overlapCombinator == null) {
            throw new IllegalArgumentException("Overlapp ikke tillat uten overlapCombinator");
        }
        LocalDateSegment<V> resultat = null;
        LocalDateInterval intervall = new LocalDateInterval(fom, tom);
        for (Integer index : aktiveSegmenter) {
            LocalDateSegment<V> segmentet = inputListe.get(index);
            LocalDateSegment<V> tilpasset = intervall.equals(segmentet.getLocalDateInterval()) ? segmentet : segmentSplitter.apply(intervall, segmentet);
            if (resultat == null) {
                resultat = tilpasset;
            } else {
                resultat = overlapCombinator.combine(intervall, resultat, tilpasset);
            }
        }
        return resultat;
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
        LocalDateSegment<V> prev = segments.isEmpty() ? null : segments.getFirst();

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
     */
    public <T, R> LocalDateTimeline<R> combine(final LocalDateTimeline<T> other, final LocalDateSegmentCombinator<V, T, R> combinator, final JoinStyle combinationStyle) {
        List<LocalDateSegment<R>> combinedSegmenter = new ArrayList<>();
        Iterator<LocalDateSegment<V>> lhsIterator = this.segments.iterator();
        Iterator<LocalDateSegment<T>> rhsIterator = other.segments.iterator();
        LocalDateSegment<V> lhs = lhsIterator.hasNext() ? lhsIterator.next() : null;
        LocalDateSegment<T> rhs = rhsIterator.hasNext() ? rhsIterator.next() : null;
        StartdatoIterator<V, T> startdatoIterator = new StartdatoIterator<>(this.segments, other.segments);
        SluttdatoIterator<V, T> sluttdatoIterator = new SluttdatoIterator<>(this.segments, other.segments);

        KnekkpunktIterator knekkpunktIterator = new KnekkpunktIterator(startdatoIterator, sluttdatoIterator);
        if (!knekkpunktIterator.hasNext()) {
            return empty(); //begge input-tidslinjer var tomme
        }

        LocalDate fom = knekkpunktIterator.next();
        while (fom != null && knekkpunktIterator.hasNext()) {
            lhs = spolTil(lhs, lhsIterator, fom);
            rhs = spolTil(rhs, rhsIterator, fom);

            boolean harLhs = lhs != null && lhs.getLocalDateInterval().contains(fom);
            boolean harRhs = rhs != null && rhs.getLocalDateInterval().contains(fom);

            boolean maxtime = knekkpunktIterator.nextIsPastEndOfTime();
            LocalDate neste;
            LocalDate tom;
            if (maxtime) {
                neste = null;
                tom = LocalDate.MAX;
            } else {
                neste = knekkpunktIterator.next();
                tom = neste.minusDays(1);
            }
            if (combinationStyle.accept(harLhs, harRhs)) {
                LocalDateInterval periode = new LocalDateInterval(fom, tom);
                LocalDateSegment<V> tilpassetLhsSegment = tilpassSegment(harLhs, lhs, periode, this.segmentSplitter);
                LocalDateSegment<T> tilpassetRhsSegment = tilpassSegment(harRhs, rhs, periode, other.segmentSplitter);
                LocalDateSegment<R> nyVerdi = combinator.combine(periode, tilpassetLhsSegment, tilpassetRhsSegment);
                if (nyVerdi != null) {
                    combinedSegmenter.add(nyVerdi);
                }
            }
            fom = neste;
        }
        return new LocalDateTimeline<>(combinedSegmenter);
    }

    static class StartdatoIterator<T, V> implements Iterator<LocalDate> {
        private final Iterator<LocalDateSegment<T>> lhsIterator;
        private final Iterator<LocalDateSegment<V>> rhsIterator;
        private LocalDate lhsDato;
        private LocalDate rhsDato;

        public StartdatoIterator(Collection<LocalDateSegment<T>> lhs, Collection<LocalDateSegment<V>> rhs) {
            lhsIterator = lhs.iterator();
            rhsIterator = rhs.iterator();
            lhsDato = lhsIterator.hasNext() ? lhsIterator.next().getFom() : null;
            rhsDato = rhsIterator.hasNext() ? rhsIterator.next().getFom() : null;
        }

        @Override
        public boolean hasNext() {
            return lhsDato != null || rhsDato != null;
        }

        @Override
        public LocalDate next() {
            LocalDate neste = (lhsDato != null && (rhsDato == null || lhsDato.isBefore(rhsDato))) ? lhsDato : rhsDato;
            while (lhsDato != null && !lhsDato.isAfter(neste)) {
                lhsDato = lhsIterator.hasNext() ? lhsIterator.next().getFom() : null;
            }
            while (rhsDato != null && !rhsDato.isAfter(neste)) {
                rhsDato = rhsIterator.hasNext() ? rhsIterator.next().getFom() : null;
            }
            return neste;
        }
    }

    static class SluttdatoIterator<T, V> implements Iterator<LocalDate> {
        private final Iterator<LocalDateSegment<T>> lhsIterator;
        private final Iterator<LocalDateSegment<V>> rhsIterator;
        private LocalDate lhsDato;
        private LocalDate rhsDato;

        public SluttdatoIterator(Collection<LocalDateSegment<T>> lhs, Collection<LocalDateSegment<V>> rhs) {
            lhsIterator = lhs.iterator();
            rhsIterator = rhs.iterator();
            lhsDato = lhsIterator.hasNext() ? lhsIterator.next().getTom() : null;
            rhsDato = rhsIterator.hasNext() ? rhsIterator.next().getTom() : null;
        }

        @Override
        public boolean hasNext() {
            return lhsDato != null || rhsDato != null;
        }

        @Override
        public LocalDate next() {
            LocalDate neste = (lhsDato != null && (rhsDato == null || lhsDato.isBefore(rhsDato))) ? lhsDato : rhsDato;
            while (lhsDato != null && !lhsDato.isAfter(neste)) {
                lhsDato = lhsIterator.hasNext() ? lhsIterator.next().getTom() : null;
            }
            while (rhsDato != null && !rhsDato.isAfter(neste)) {
                rhsDato = rhsIterator.hasNext() ? rhsIterator.next().getTom() : null;
            }
            return neste;
        }
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
        if (size() != other.size()) {
            return false;
        }

        Iterator<LocalDateSegment<V>> iteratorA = segments.iterator();
        @SuppressWarnings("rawtypes")
        Iterator<LocalDateSegment> iteratorB = other.segments.iterator();
        while (iteratorA.hasNext()) {
            if (!Objects.equals(iteratorA.next(), iteratorB.next())) {
                return false;
            }
        }
        return true;
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
        return segments.getLast().getTom();
    }

    public LocalDate getMinLocalDate() {
        if (isEmpty()) {
            throw new IllegalStateException("Timeline is empty"); //$NON-NLS-1$
        }
        return segments.getFirst().getFom();
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
        int hash = 1;
        for (LocalDateSegment<V> segment : segments) {
            hash = hash * 31 + segment.hashCode();
        }
        return hash;
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
     *
     * @deprecated Bytt til segmenter() istedet
     */
    @Deprecated(forRemoval = true) //bruk segmenter() istedet
    public NavigableSet<LocalDateSegment<V>> toSegments() {
        return Collections.unmodifiableNavigableSet(new TreeSet<>(segments));
    }

    public SequencedCollection<LocalDateSegment<V>> segmenter() {
        return Collections.unmodifiableSequencedCollection(segments);
    }


    @Override
    public Iterator<LocalDateSegment<V>> iterator() {
        return segmenter().iterator();
    }


    public Stream<LocalDateSegment<V>> stream() {
        return segments.stream();
    }

    /**
     * Find timeline of unique segments in collection of segments that may overlap.
     */
    public static <V> no.nav.fpsak.tidsserie.LocalDateTimeline<List<V>> buildGroupOverlappingSegments(Collection<LocalDateSegment<V>> segmentsWithPossibleOverlaps) {
        List<LocalDateSegment<List<V>>> mappedSegments = segmentsWithPossibleOverlaps.stream()
                .map(s -> new LocalDateSegment<>(s.getLocalDateInterval(), List.of(s.getValue())))
                .toList();
        return new LocalDateTimeline<>(mappedSegments, StandardCombinators::concatLists);
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
     * Quick check to see if interval is completely outside timeline. (avoids testing each segment)
     */
    public boolean isTimelineOutsideInterval(LocalDateInterval datoInterval) {
        return datoInterval.getFomDato().isAfter(this.getMaxLocalDate()) || datoInterval.getTomDato().isBefore(this.getMinLocalDate());
    }

    private Optional<LocalDateSegment<V>> findOverlappingValue(LocalDateInterval datoInterval) {
        if (isEmpty()) {
            return Optional.empty();
        }
        return segments.stream().filter(s -> s.getLocalDateInterval().overlaps(datoInterval)).findFirst();
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

    private static <X> boolean erCollectionSortertOgIkkeOverlappende(Collection<LocalDateSegment<X>> segmenter) {
        LocalDateSegment<X> last = null;
        for (LocalDateSegment<X> segment : segmenter) {
            if (last != null && !segment.getFom().isAfter(last.getTom())) {
                return false;
            }
            last = segment;
        }
        return true;
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

    /**
     * Segment splitter som oppretter ny collection for støttede typer dersom verdien er en collection. Ellers som EqualValueSegmentSplitter.
     */
    static final class DefaultSegmentSplitter<V> implements SegmentSplitter<V>, Serializable {
        @Override
        public LocalDateSegment<V> apply(LocalDateInterval di, LocalDateSegment<V> seg) {
            if (di.equals(seg.getLocalDateInterval())) {
                return seg;
            }
            V gammelVerdi = seg.getValue();
            if (gammelVerdi == null){
                return new LocalDateSegment<>(di, null);
            }
            Object nyVerdi = switch (gammelVerdi) {
                case List<?> l -> {
                    verifyNotFromJavaConcurrent(l);
                    yield new ArrayList<>(l);
                }
                case Set<?> s -> {
                    verifyNotFromJavaConcurrent(s);
                    yield switch (s) {
                        case TreeSet<?> ts -> new TreeSet<>(ts);
                        case EnumSet<?> es -> EnumSet.copyOf(es);
                        case SequencedSet<?> seq -> new LinkedHashSet<>(seq);
                        default -> new HashSet<>(s);
                    };
                }
                case Map<?, ?> m -> {
                    verifyNotFromJavaConcurrent(m);
                    yield switch (m) {
                        case TreeMap<?, ?> tm -> new TreeMap<>(tm);
                        case EnumMap<?, ?> em -> new EnumMap<>(em);
                        case SequencedMap<?, ?> seq -> new LinkedHashMap<>(seq);
                        default -> new HashMap<>(m);
                    };
                }
                case Collection<?> c ->
                        throw new IllegalArgumentException(String.format("Collection type %s is not supported for default segment splitter", c.getClass().getName()));
                default -> gammelVerdi;
            };
            return new LocalDateSegment<>(di, (V) nyVerdi);
        }


        static void verifyNotFromJavaConcurrent(Object obj) {
            if (obj.getClass().getPackageName().equals("java.util.concurrent")) {
                throw new IllegalArgumentException(String.format("Collection/Map type %s is not supported for default segment splitter", obj.getClass().getName()));
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
