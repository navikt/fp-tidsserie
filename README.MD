![](https://github.com/navikt/fp-tidsserie/workflows/Bygg%20og%20deploy/badge.svg) 
[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-tidsserie&metric=alert_status)](https://sonarcloud.io/dashboard?id=navikt_fp-tidsserie) 
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-tidsserie&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=navikt_fp-tidsserie)
[![SonarCloud Bugs](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-tidsserie&metric=bugs)](https://sonarcloud.io/component_measures/metric/reliability_rating/list?id=navikt_fp-tidsserie)
[![SonarCloud Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-tidsserie&metric=vulnerabilities)](https://sonarcloud.io/component_measures/metric/security_rating/list?id=navikt_fp-tidsserie)
![GitHub](https://img.shields.io/github/license/navikt/fp-tidsserie)

![GitHub release (latest by date)](https://img.shields.io/github/v/release/navikt/fp-tidsserie.svg?style=for-the-badge&logo=apache-maven&label=)

# Bibliotek for Tidsserie håndtering
Bibliotek for å forenkle håndtering av periodiserte data.  
Dette tar vekk kompleksitet knyttet til periodisering av data og forenkler det ned til et sett med operasjoner på en tidsserie av periodiserte data.  

F.eks. vil det automatisk holde rede på knekkpunkter, kunne kombinere tidsserier med ulike typer data, slå sammen tidsserier, eller forenkle tidsserier med påløpende perioder med like verdier.

(NB: det er ikke egnet for 'timeseries analysis' eller lignende statistisk analyse av store datamengder).

# Bruk

```        
		<dependency>
			<groupId>no.nav.fpsak.tidsserie</groupId>
			<artifactId>fpsak-tidsserie</artifactId>
			<version>2.3-20200427084950-396F346</version>
		</dependency>
```

# Brukstilfeller

## Knekke opp et sett med perioder (fom - fra-og-med, tom - til-og-med).
Tar inn perioder (`LocalDateSegment`) og bygger en serie ut av de.  Dersom perioder overlapper vil den automatiskk knekke i flere perioder.

## Kombinere tidsserier
Flere tidsserier (`LocalDateTimeline`) kan kombineres på ulike måter:
* Kombinere verdier
* Aritmetiske operasjoner  (summere, negere, multiplisere, dividere) verdier  periodene
* Boolske operassjoner (eks. finne boolsk resultat per periode)
* Velge alle perioder fra ene tidsserien med matchende data i begge, ene eller andre tidsserie

Alle operasjoner som utføres på tidsserier er abstrahert bort ved et funksjonsuttrykk som kan spesifiseres. Vanlige operasjoner er lagt til i `StandardCombinators`, deriblant:
* `sum`
* `product`
* `leftOnly`, `rightOnly` - velg kun verdier fra 'venstre' eller 'høyre' tidsserie i et funksonsutrykk når perioden matcher
* `coalesceLeftHandSide` - velg verdi fra venstre hvis fins når periode matcher, ellers velg fra høyre når ikke finnes.
* `allValues`- returner alle verdier fra begge tidsserier fra hver periode (typisk som en liste av verdier per periode).
* `concat` - konkatener per periode sett med strenger fra to tidsserier.

Ved å implementer `LocalDateSegmentCombinator` (evt. som lambda) kan egen-definert håndtering av verdier per periode (segment) benyttes.


[Eksempler på kombinering av tidsserier](https://github.com/navikt/fp-tidsserie/blob/master/src/test/java/no/nav/fpsak/tidsserie/LocalDateTimelineExamplesTest.java)

# Implementasjon
`LocalDateTimeline` er en tidsserie.  Den består av et sett med `LocalDateSegment`, som har fom/tom datoer (i form av et `LocalDateInterval`) og en verdi <V>. 
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
