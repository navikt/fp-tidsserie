package no.nav.fpsak.tidsserie;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;

/** Standard (vanlige brukte) funksjoner for kombinere verdier fra to segmenter over et gitt interval. */
public class StandardCombinators {

    private static final Map<String, Number> ZEROS;

    static {
        ZEROS = new HashMap<>();
        ZEROS.put("Long", 0L);
        ZEROS.put("Integer", 0);
        ZEROS.put("Double", 0d);
        ZEROS.put("Float", 0f);
        ZEROS.put("Short", (short) 0);
        ZEROS.put("Byte", (byte) 0);
        ZEROS.put("BigDecimal", BigDecimal.ZERO);
        ZEROS.put("BigInteger", BigInteger.ZERO);
    }

    private StandardCombinators() {
        // private
    }

    /**
     * Returner liste alle verdier fra begge tidsserier angitt. Det anbefales først og fremst benyttet for tidsserier
     * med verdier av samme type, men det er ikke et krav.
     */
    public static <V> LocalDateSegment<List<V>> allValues(LocalDateInterval dateInterval, LocalDateSegment<List<V>> lhs, LocalDateSegment<V> rhs) {
        if (lhs != null && rhs != null) {
            List<V> list = new ArrayList<>(lhs.getValue());
            list.add(rhs.getValue());
            return new LocalDateSegment<>(dateInterval, list);
        } else if (lhs == null && rhs == null) {
            return null;
        } else {
            if (lhs != null) {
                return new LocalDateSegment<>(dateInterval, lhs.getValue());
            } else {
                return new LocalDateSegment<>(dateInterval, List.of(rhs.getValue()));
            }
        }
    }

    /**
     * Basic combinator som alltid returnerer Boolean.TRUE for angitt interval. Greit å bruke når verdi ikke betyr
     * noe, kun intervaller. Merk hvilket intervall som benyttes avhenger av {@link JoinStyle} og "includeGaps". Alle som passer får True.
     */
    public static LocalDateSegment<Boolean> alwaysTrueForMatch(LocalDateInterval dateInterval,
                                                               @SuppressWarnings("unused") LocalDateSegment<?> lhs, // NOSONAR
                                                               @SuppressWarnings("unused") LocalDateSegment<?> rhs // NOSONAR
    ) {
        return new LocalDateSegment<>(dateInterval, Boolean.TRUE);
    }

    /** Returner begge verdier. */
    @SuppressWarnings({"rawtypes"})
    public static <T, V> LocalDateSegment<List> bothValues(LocalDateInterval dateInterval,
                                                           LocalDateSegment<T> lhs, LocalDateSegment<V> rhs) {
        if (lhs != null && rhs != null) {
            return new LocalDateSegment<>(dateInterval, Arrays.asList(lhs.getValue(), rhs.getValue()));
        } else if (lhs == null && rhs == null) {
            return null;
        } else {
            return new LocalDateSegment<>(dateInterval, Collections.singletonList(Objects.requireNonNullElse(lhs, rhs).getValue()));
        }
    }

    /** Basic combinator som alltid returnerer verdi fra første (Left-Hand Side) timeline hvis finnes, ellers andre. */
    public static <V> LocalDateSegment<V> coalesceLeftHandSide(LocalDateInterval dateInterval,
                                                               LocalDateSegment<V> lhs, LocalDateSegment<V> rhs) {
        return lhs == null ? new LocalDateSegment<>(dateInterval, rhs.getValue()) : new LocalDateSegment<>(dateInterval, lhs.getValue());
    }

    /** Basic combinator som alltid returnerer verdi fra andre (Right-Hand Side) timeline hvis finnes, ellers første. */
    public static <V> LocalDateSegment<V> coalesceRightHandSide(LocalDateInterval dateInterval,
                                                                LocalDateSegment<V> lhs, LocalDateSegment<V> rhs) {
        return rhs == null ? new LocalDateSegment<>(dateInterval, lhs.getValue()) : new LocalDateSegment<>(dateInterval, rhs.getValue());
    }

    /** Tar kun første (Left-Hand Side) verdi alltid, selv om null */
    public static <T, V> LocalDateSegment<T> leftOnly(LocalDateInterval dateInterval,
                                                      LocalDateSegment<T> lhs,
                                                      @SuppressWarnings("unused") LocalDateSegment<V> rhs // NOSONAR
    ) {
        return new LocalDateSegment<>(dateInterval, lhs.getValue());
    }

    /** Tar kun andre (Right-Hand Side) verdi alltid, selv om null */
    public static <T, V> LocalDateSegment<V> rightOnly(LocalDateInterval dateInterval,
                                                       @SuppressWarnings("unused") LocalDateSegment<T> lhs, // NOSONAR
                                                       LocalDateSegment<V> rhs) {
        return new LocalDateSegment<>(dateInterval, rhs.getValue());
    }

    /**
     * Basic combinator som tar produkt av verdi fra begge segmenter (multipliserer)
     */
    public static <L extends Number, R extends Number> LocalDateSegment<L> product(LocalDateInterval dateInterval,
                                                                                   LocalDateSegment<L> lhs,
                                                                                   LocalDateSegment<R> rhs) {

        return new LocalDateSegment<>(dateInterval, product(lhs == null ? null : lhs.getValue(), rhs == null ? null : rhs.getValue()));
    }

    /**
     * Basic combinator som sum av verdi fra begge segmenter
     */
    public static <L extends Number, R extends Number> LocalDateSegment<L> sum(LocalDateInterval dateInterval,
                                                                               LocalDateSegment<L> lhs,
                                                                               LocalDateSegment<R> rhs) {

        return new LocalDateSegment<>(dateInterval, sum(lhs == null ? null : lhs.getValue(), rhs == null ? null : rhs.getValue()));
    }

    /**
     * Basic combinator som sum av verdi fra begge segmenter
     */
    public static LocalDateSegment<String> concat(LocalDateInterval dateInterval,
                                                  LocalDateSegment<String> lhs,
                                                  LocalDateSegment<String> rhs) {

        String lv = lhs == null ? null : lhs.getValue();
        String rv = rhs == null ? null : rhs.getValue();
        return new LocalDateSegment<>(dateInterval, (lv == null ? "" : lv) + (rv == null ? "" : rv));
    }

    /**
     * Basic combinator som tar minste verdi, evt lhs dersom like
     */
    public static <V extends Comparable<? super V>> LocalDateSegment<V> min(LocalDateInterval dateInterval,
                                                                            LocalDateSegment<V> lhs,
                                                                            LocalDateSegment<V> rhs) {
        if (lhs != null && rhs != null) {
            var least = lhs.getValue().compareTo(rhs.getValue()) <= 0 ? lhs.getValue() : rhs.getValue();
            return new LocalDateSegment<>(dateInterval, least);
        }
        return lhs == null ? new LocalDateSegment<>(dateInterval, rhs.getValue()) : new LocalDateSegment<>(dateInterval, lhs.getValue());
    }

    /**
     * Basic combinator som tar største verdi, evt lhs dersom like
     */
    public static <V extends Comparable<? super V>> LocalDateSegment<V> max(LocalDateInterval dateInterval,
                                                                            LocalDateSegment<V> lhs,
                                                                            LocalDateSegment<V> rhs) {
        if (lhs != null && rhs != null) {
            var greatest = lhs.getValue().compareTo(rhs.getValue()) >= 0 ? lhs.getValue() : rhs.getValue();
            return new LocalDateSegment<>(dateInterval, greatest);
        }
        return lhs == null ? new LocalDateSegment<>(dateInterval, rhs.getValue()) : new LocalDateSegment<>(dateInterval, lhs.getValue());
    }

    /**
     * Basic combinator som slår sammen Liste-verdier til en liste
     */
    public static <V> LocalDateSegment<List<V>> concatLists(LocalDateInterval dateInterval,
                                                            LocalDateSegment<List<V>> lhs,
                                                            LocalDateSegment<List<V>> rhs) {
        if (lhs != null && rhs != null) {
            return new LocalDateSegment<>(dateInterval, Stream.concat(lhs.getValue().stream(), rhs.getValue().stream()).toList());
        } else if (lhs == null && rhs == null) {
            return null;
        }
        return lhs == null ? new LocalDateSegment<>(dateInterval, rhs.getValue()) : new LocalDateSegment<>(dateInterval, lhs.getValue());
    }

    /**
     * Basic combinator som slår sammen to Sets vha Union
     */
    public static <V> LocalDateSegment<Set<V>> union(LocalDateInterval dateInterval,
                                                     LocalDateSegment<Set<V>> lhs,
                                                     LocalDateSegment<Set<V>> rhs) {
        if (lhs != null && rhs != null) {
            var union = new HashSet<>(lhs.getValue());
            union.addAll(rhs.getValue());
            return new LocalDateSegment<>(dateInterval, union);
        } else if (lhs == null && rhs == null) {
            return null;
        }
        return lhs == null ? new LocalDateSegment<>(dateInterval, rhs.getValue()) : new LocalDateSegment<>(dateInterval, lhs.getValue());
    }

    /**
     * Basic combinator som slår sammen to Sets vha Intersection
     */
    public static <V> LocalDateSegment<Set<V>> intersection(LocalDateInterval dateInterval,
                                                            LocalDateSegment<Set<V>> lhs,
                                                            LocalDateSegment<Set<V>> rhs) {
        if (lhs != null && rhs != null) {
            var intersection = lhs.getValue().stream().filter(v -> rhs.getValue().contains(v)).collect(Collectors.toCollection(HashSet::new));
            return new LocalDateSegment<>(dateInterval, intersection);
        } else if (lhs == null && rhs == null) {
            return null;
        }
        return lhs == null ? new LocalDateSegment<>(dateInterval, rhs.getValue()) : new LocalDateSegment<>(dateInterval, lhs.getValue());
    }

    /**
     * Basic combinator som slår sammen to Sets vha Difference (A-B)
     */
    public static <V> LocalDateSegment<Set<V>> difference(LocalDateInterval dateInterval,
                                                          LocalDateSegment<Set<V>> lhs,
                                                          LocalDateSegment<Set<V>> rhs) {
        if (lhs != null && rhs != null) {
            var intersection = lhs.getValue().stream().filter(v -> !rhs.getValue().contains(v)).collect(Collectors.toCollection(HashSet::new));
            return new LocalDateSegment<>(dateInterval, intersection);
        } else if (lhs == null && rhs == null) {
            return null;
        }
        return lhs == null ? new LocalDateSegment<>(dateInterval, rhs.getValue()) : new LocalDateSegment<>(dateInterval, lhs.getValue());
    }



    @SuppressWarnings("unchecked")
    private static <L extends Number, R extends Number> L sum(L lhs, R rhs) {
        if (lhs == null && rhs == null) {
            return null; // kan ikke bestemme return type?
        } else {
            // fungerer kun hvis begge har samme type
            if (lhs == null) {
                return sumNonNull((L) ZEROS.get(rhs.getClass().getSimpleName()), rhs);
            } else if (rhs == null) {
                return sumNonNull((L) ZEROS.get(lhs.getClass().getSimpleName()), lhs);
            }
        }

        return sumNonNull(lhs, rhs);
    }

    @SuppressWarnings("unchecked")
    private static <L extends Number, R extends Number> L sumNonNull(L lhs, R rhs) {
        String type = lhs.getClass().getSimpleName();
        return switch (type) {
            case "Long" -> (L) Long.valueOf(lhs.longValue() + rhs.longValue());
            case "Integer" -> (L) Integer.valueOf(lhs.intValue() + rhs.intValue());
            case "Double" -> (L) Double.valueOf(lhs.doubleValue() + rhs.doubleValue());
            case "Float" -> (L) Float.valueOf(lhs.floatValue() + rhs.floatValue());
            case "Short" -> (L) Short.valueOf((short) (lhs.shortValue() + rhs.shortValue()));
            case "Byte" -> (L) Byte.valueOf((byte) (lhs.byteValue() + rhs.byteValue()));
            case "BigDecimal" -> (L) ((BigDecimal) lhs).add(new BigDecimal(rhs.toString()));
            case "BigInteger" -> (L) ((BigInteger) lhs).add(new BigInteger(rhs.toString()));
            default -> (L) Double.valueOf(lhs.doubleValue() + rhs.doubleValue());
        };
    }

    @SuppressWarnings("unchecked")
    private static <L extends Number, R extends Number> L product(L lhs, R rhs) {
        if (lhs == null && rhs == null) {
            return null; // kan ikke bestemme return type?
        } else {
            // fungerer kun hvis begge har samme type
            if (lhs == null) {
                return productNonNull((L) ZEROS.get(rhs.getClass().getSimpleName()), rhs);
            } else if (rhs == null) {
                return productNonNull((L) ZEROS.get(lhs.getClass().getSimpleName()), lhs);
            }
        }

        return productNonNull(lhs, rhs);
    }

    @SuppressWarnings("unchecked")
    private static <L extends Number, R extends Number> L productNonNull(L lhs, R rhs) {
        String type = lhs.getClass().getSimpleName();
        return switch (type) {
            case "Long" -> (L) Long.valueOf(lhs.longValue() * rhs.longValue());
            case "Integer" -> (L) Integer.valueOf(lhs.intValue() * rhs.intValue());
            case "Double" -> (L) Double.valueOf(lhs.doubleValue() * rhs.doubleValue());
            case "Float" -> (L) Float.valueOf(lhs.floatValue() * rhs.floatValue());
            case "Short" -> (L) Short.valueOf((short) (lhs.shortValue() * rhs.shortValue()));
            case "Byte" -> (L) Byte.valueOf((byte) (lhs.byteValue() * rhs.byteValue()));
            case "BigDecimal" -> (L) ((BigDecimal) lhs).multiply(new BigDecimal(rhs.toString()));
            case "BigInteger" -> (L) ((BigInteger) lhs).multiply(new BigInteger(rhs.toString()));
            default -> (L) Double.valueOf(lhs.doubleValue() * rhs.doubleValue());
        };
    }
}
