package no.nav.fpsak.tidsserie;

import static java.time.temporal.TemporalAdjusters.next;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import no.nav.fpsak.tidsserie.json.LocalDateIntervalFormatters;

/**
 * Denne modellerer et interval av to LocalDate. Intern representasjon benytter
 * fom/tom istdf. fom/til da dette er innarbeidet i de fleste modeller i NAV, og
 * mindre forvirrende ved lesing av dato intervaller.
 * <p>
 * API'et er modellert etter metoder fra java.time og threeten-extra Interval.
 *
 * <p>
 * (Det er relativt lett å utvide til ikke-inklusive intervaller, dersom det er
 * behov for det.)
 * <p>
 * Representasjonen bruker faste tidspuynkt til å representere åpne intervaller,
 * slik at null håndtering ikke er nødvendig i {@link LocalDateTimeline}.
 */
@JsonSerialize(using = LocalDateIntervalFormatters.Serializer.class)
@JsonDeserialize(using = LocalDateIntervalFormatters.Deserializer.class)
public class LocalDateInterval implements Comparable<LocalDateInterval>, Serializable {

    private static final String OPEN_END_FORMAT = "-";

    public static final Comparator<LocalDateInterval> ORDER_INTERVALS = Comparator.comparing(LocalDateInterval::getFomDato)
            .thenComparing(LocalDateInterval::getTomDato);

    /**
     * bruker en verdi til å representere åpen start, forenkle en del algoritmer.
     */
    public static final LocalDate TIDENES_BEGYNNELSE = LocalDate.of(-4712, Month.JANUARY, 1);

    /**
     * bruker en verdi til å representere åpent intervall, forenkler en del
     * algoritmer. Verdien er tilsvarende max sysdate en Oracle db.
     */
    public static final LocalDate TIDENES_ENDE = LocalDate.of(9999, Month.DECEMBER, 31);

    private LocalDate fomDato;

    private LocalDate tomDato;

    public LocalDateInterval(LocalDate fomDato, LocalDate tomDato) {
        if (fomDato != null && tomDato != null && tomDato.isBefore(fomDato)) {
            throw new IllegalArgumentException("Til og med dato før fra og med dato: " + fomDato + ">" + tomDato); //$NON-NLS-1$ //$NON-NLS-2$
        }
        this.fomDato = fomDato == null ? TIDENES_BEGYNNELSE : fomDato;
        this.tomDato = tomDato == null ? TIDENES_ENDE : tomDato;
    }

    public static String formatDate(LocalDate date, String openEndFormat) {
        if (date == null || TIDENES_BEGYNNELSE.equals(date) || TIDENES_ENDE.equals(date)) {
            return openEndFormat;
        } else {
            return date.toString();
        }
    }

    public static LocalDate max(LocalDate en, LocalDate to) {
        return en.isAfter(to) ? en : to;
    }

    public static LocalDate min(LocalDate en, LocalDate to) {
        return en.isBefore(to) ? en : to;
    }

    public static LocalDateInterval withPeriodAfterDate(LocalDate startDate, Period period) {
        return new LocalDateInterval(startDate, startDate.plus(period));
    }

    public static LocalDateInterval withPeriodBeforeDate(Period period, LocalDate endDate) {
        return new LocalDateInterval(endDate.minus(period), endDate);
    }

    /**
     * Hvorvidt to intervaller ligger rett ved siden av hverandre (at this.tomDato == other.fomDato - 1 eller vice versa).
     */
    public boolean abuts(LocalDateInterval other) {
        return getTomDato().equals(other.getFomDato().minusDays(1)) || other.getTomDato().equals(getFomDato().minusDays(1));
    }

    /**
     * Hvorvidt to intervaller ligger rett ved siden av hverandre (at this.tomDato == other.fomDato - 1 eller vice versa).
     */
    public boolean abutsWorkdays(LocalDateInterval other) {
        return nextWorkday(getTomDato()).equals(adjustWeekendToMonday(other.getFomDato())) || nextWorkday(other.getTomDato()).equals(adjustWeekendToMonday(getFomDato()));
    }

    @Override
    public int compareTo(LocalDateInterval periode) {
        return ORDER_INTERVALS.compare(this, periode);
    }

    public boolean contains(LocalDate dato) {
        return !getFomDato().isAfter(dato) && !getTomDato().isBefore(dato);
    }

    public boolean contains(LocalDateInterval other) {
        boolean inneholder = (getFomDato().isBefore(other.getFomDato()) || getFomDato().isEqual(other.getFomDato()))
                && (getTomDato().isAfter(other.getTomDato()) || getTomDato().isEqual(other.getTomDato()));
        return inneholder;
    }

    public long days() {
        if (!isClosedInterval()) {
            throw new UnsupportedOperationException("Intervallet er åpent, kan ikke beregne antall dager på en trygg måte:" + this); //$NON-NLS-1$
        }
        return ChronoUnit.DAYS.between(getFomDato(), getTomDato().plusDays(1));
    }

    /**
     * Returnerer true hvis intervalet er lukket (dvs. hverken åpen fom eller tom
     * dato.
     */
    public boolean isClosedInterval() {
        return !(TIDENES_BEGYNNELSE.isEqual(getFomDato()) && TIDENES_ENDE.isEqual(getTomDato()));
    }

    public boolean encloses(ChronoLocalDate dato) {
        return isDateAfterOrEqualStartOfInterval(dato) && isDateBeforeOrEqualEndOfInterval(dato);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof LocalDateInterval)) {
            return false;
        }
        var periode = (LocalDateInterval) obj;
        return isEqual(periode);
    }

    /** Returnerer deler av this som ikke overlapper i #annen. */
    public NavigableSet<LocalDateInterval> except(LocalDateInterval annen) {
        if (!this.overlaps(annen)) {
            return new TreeSet<>(Collections.singletonList(this));
        }

        NavigableSet<LocalDateInterval> resultat = new TreeSet<>();
        if (getFomDato().isBefore(annen.getFomDato())) {
            resultat.add(new LocalDateInterval(getFomDato(), min(getTomDato(), annen.getFomDato().minusDays(1))));
        }
        if (getTomDato().isAfter(annen.getTomDato())) {
            resultat.add(new LocalDateInterval(max(getFomDato(), annen.getTomDato().plusDays(1)), getTomDato()));
        }
        return resultat;
    }

    public LocalDateInterval expand(LocalDateInterval other) {
        return expand(other, LocalDateInterval::abuts);
    }

    public LocalDateInterval expand(LocalDateInterval other, BiPredicate<LocalDateInterval, LocalDateInterval> abuts) {
        if (!(abuts.test(this, other) || this.overlaps(other))) {
            throw new IllegalArgumentException(String.format("Intervals do not abut/overlap: %s <-> %s", this, other)); //$NON-NLS-1$
        } else {
            return new LocalDateInterval(min(this.getFomDato(), other.getFomDato()), max(getTomDato(), other.getTomDato()));
        }
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    /** Hvorvidt intervallet har åpen start. */
    public boolean isOpenStart() {
        return TIDENES_BEGYNNELSE.isEqual(fomDato);
    }

    /** Hvorvidt intervallet har åpen slutt. */
    public boolean isOpenEnd() {
        return TIDENES_ENDE.isEqual(tomDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFomDato(), getTomDato());
    }

    /**
     * Hvorvidt to intervaller enten overlapper eller grenser til ('abuts')
     * hverandre.
     */
    public boolean isConnected(LocalDateInterval other) {
        return overlaps(other) || abuts(other);
    }

    public boolean isConnected(LocalDateInterval other, BiPredicate<LocalDateInterval, LocalDateInterval> abuts) {
        return overlaps(other) || abuts.test(this, other);
    }

    public boolean isDateAfterOrEqualStartOfInterval(ChronoLocalDate dato) {
        return getFomDato().isBefore(dato) || getFomDato().isEqual(dato);
    }

    public boolean isDateBeforeOrEqualEndOfInterval(ChronoLocalDate dato) {
        return getTomDato() == null || getTomDato().isAfter(dato) || getTomDato().isEqual(dato);
    }

    public boolean isEqual(LocalDateInterval other) {
        return Objects.equals(getFomDato(), other.getFomDato())
                && Objects.equals(getTomDato(), other.getTomDato());
    }

    public Optional<LocalDateInterval> overlap(LocalDateInterval annen) {
        if (!this.overlaps(annen)) {
            return Optional.empty();
        } else if (this.isEqual(annen)) {
            return Optional.of(this);
        } else {
            return Optional.of(new LocalDateInterval(max(getFomDato(), annen.getFomDato()), min(getTomDato(), annen.getTomDato())));
        }
    }

    public boolean overlaps(LocalDateInterval other) {
        boolean fomBeforeOrEqual = getFomDato().isBefore(other.getTomDato()) || getFomDato().isEqual(other.getTomDato());
        boolean tomAfterOrEqual = getTomDato().isAfter(other.getFomDato()) || getTomDato().isEqual(other.getFomDato());
        boolean overlapper = fomBeforeOrEqual && tomAfterOrEqual;
        return overlapper;
    }

    /**
     * Splitter to intervaller mot hverandre. Hvis de er like, returnerer this
     * <p>
     * Ellers returneres alle unike intervaller, der overlap også splittes i eget
     * interval.
     * <p>
     */
    public NavigableSet<LocalDateInterval> splitAll(LocalDateInterval annen) {
        if (this.isEqual(annen)) {
            return new TreeSet<>(Collections.singletonList(this));
        }

        NavigableSet<LocalDateInterval> resultat = new TreeSet<>();
        resultat.addAll(this.except(annen));
        resultat.addAll(annen.except(this));
        this.overlap(annen).ifPresent(resultat::add);
        return resultat;
    }

    /**
     * Splitter dette intervallet mot et annet. Hvis de er like, returnerer this
     * <p>
     * Ellers returneres alle unike intervaller fra this, der overlap også splittes
     * i eget interval. Intervaller som kun er i annen returneres ikke
     * <p>
     */
    public NavigableSet<LocalDateInterval> splitThisBy(LocalDateInterval annen) {
        if (this.isEqual(annen)) {
            return new TreeSet<>(Collections.singletonList(this));
        }

        NavigableSet<LocalDateInterval> resultat = new TreeSet<>();
        resultat.addAll(this.except(annen));
        this.overlap(annen).ifPresent(resultat::add);
        return resultat;
    }

    public Period toPeriod() {
        return Period.between(fomDato, tomDato);
    }

    @Override
    public String toString() {
        LocalDate fom = getFomDato();
        LocalDate tom = getTomDato();
        return String.format("[%s, %s]", formatDate(fom, OPEN_END_FORMAT), formatDate(tom, OPEN_END_FORMAT)); //$NON-NLS-1$ //$NON-NLS-2$
                                                                                                              // //$NON-NLS-3$
    }

    public long totalDays() {
        return ChronoUnit.DAYS.between(fomDato, tomDato.plusDays(1));
    }

    public static LocalDateInterval parseFrom(String fom, String tom) {
        LocalDate fomDato = fom == null || fom.isEmpty() || OPEN_END_FORMAT.equals(fom) ? null : LocalDate.parse(fom); // $NON-NLS-1$
        LocalDate tomDato = tom == null || tom.isEmpty() || OPEN_END_FORMAT.equals(tom) ? null : LocalDate.parse(tom); // $NON-NLS-1$
        return new LocalDateInterval(fomDato, tomDato);
    }

    /**
     * Litt hjelp for perioder og arbeidsdager vs weekend. Funder på å inkludere helligdagsalgoritmen
     */
    public static final Set<DayOfWeek> WEEKEND = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    public static LocalDate nextWorkday(LocalDate date) {
        return adjustWeekendToMonday(date.plusDays(1));
    }

    public static LocalDate adjustWeekendToMonday(LocalDate date) {
        return adjustWeekendTo(date, next(DayOfWeek.MONDAY));
    }

    public static LocalDate adjustWeekendToFriday(LocalDate date) {
        return adjustWeekendTo(date, DayOfWeek.FRIDAY);
    }

    public static LocalDate adjustWeekendTo(LocalDate date, TemporalAdjuster weekendAdjuster) {
        return WEEKEND.contains(DayOfWeek.from(date)) ? date.with(weekendAdjuster) : date;
    }

    public static LocalDate adjustThorughWeekend(LocalDate date) {
        return WEEKEND.contains(DayOfWeek.from(date.plusDays(1))) ? date.with(DayOfWeek.SUNDAY) : date;
    }

    public LocalDateInterval adjustIntoWorkweek() {
        return new LocalDateInterval(adjustWeekendToMonday(getFomDato()), adjustWeekendToFriday(getTomDato()));
    }

    public LocalDateInterval extendThroughWeekend() {
        return new LocalDateInterval(getFomDato(), adjustThorughWeekend(getTomDato()));
    }
}
