# Tidsserie håndtering for Fpsak
Bibliotek for å forenkle håndtering av periodiserte data.  
Dette tar vekk kompleksitet knyttet til periodisering av data og forenkler det ned til et sett med operasjoner på en tidsserie av periodiserte data.

# Implementasjon
LocalDateTimeline er en tidsserie.  Den består av et sett med LocalDateSegment, som har fom/tom datoer (i form av et LocalDateInterval) og en verdi <V>. 
Fom/tom (fra-og-med, til-og-med) er valgt ettersom det er datoer som manipuleres og det er dermed mindre sjanse for forvirring om sluttdatoer er med eller ikke. 
Åpne intervaller er modellert med min/max dato (som tilfeldigvis matcher min/max date i Oracle db) slik at null-håndtering unngås.  
Dette er ikke synlig for bruker av API'et, men kan gi problemer med må modellere intervaller etter år 9999, eller før -4999.

Implementasjonen er uavhengig av hvilken type verdi <V> har, brukeren av biblioteket kan selv bestemme dette.

For eksempel for to tidsserier med verdier av type A og B (kan f.eks. være en BigDecimal og et eget-definert objekt), vil det ved noen typer operasjoner være behov for å definere en funksjon for å kombinere to verdier.
Hva funksjonen gjør er opp til brukeren, men det kan være matematiske operasjoner som addisjon, multiplikasjon, subtraksjon, divisjon, eller noe helt egen-definert ("pærer % epler = appelsiner").
Noen vanlige funksjoner er ferdig-definert.

### Immutabilitet
Alle objekter (LocalDateTimeline, LocalDateSegment, LocalDateInterval) er immutable.  Ved operasjoner på en tidsserie, vil en ny tidsserie eller verdi produseres.

# Operasjoner på tidslinjer
Operasjoner på tidslinjer vanligvis en av følgende typer:
### reduce
aggregerer innhold i en tidsserie ned til en enkel output, f.eks. en sum, eller count
### mapValue
mapper en tidsserie fra en verdi-type til en annen
### filterValue
returnerer en ny tidsserie der alle intervaller/verdier som matcher en angitt funksjon er med.
### compress
Returnerer ny tidsserie der tilliggende intervaller med samme verdi er slått sammen.
### combine
Kombinerer to tidsserier T(a) og T(b) til en ny tidsserie T(c), der a, b, og c kan være forskjellig typer java objekter. 
Intervaller i en tidsserie kombineres ihenhold til angitt "JoinStyle" som angir om det er intersection(inner_join) eller noe annen form for kombinering av intervaller. 
Det krever at bruker oppgir en funksjon f(a,b)=c for hvordan verdier skal håndteres (evt. bruker en av de vedlagte standard funksjonene).

Det finnes også convenience metoder å kombinere tidsserier, med færre parametere:
* intersection:  returnerer ny tidserie med intervall kun der begge overlapper
* disjoint: Returnerer ny tidsserie som kun inneholder intervaller som ikke finnes i angitt tidsserie parameter

Hvorvidt tomme intervaller, eller andre måter å kombinere tidsseriene på kan angis ved parametere.


