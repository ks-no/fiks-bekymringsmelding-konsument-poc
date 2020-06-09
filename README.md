# Proof-of-Concept for nedlastning av bekymringsmelding via Fiks-IO

## Disclaimers
Koden er ikke klargjort for produksjon og er ment som et konseptbevis på hvordan man kan koble seg opp mot Fiks-IO for å hente ned bekymringsmeldinger.

**Les over koden før dere konfigurerer opp for å kjøre PoC-testing i testmiljøet vårt.**

Når produksjonskode skal utvikles er det anbefalt å lage gode rutiner for logging, prosessflyt og unntakshåndtering. 

## Komme i gang
1. Dersom dere ikke har opprettet maskinporten-klient hos Difi må det gjøres, [klikk her for å lese mer](https://ks-no.github.io/fiks-plattform/difiidportenklient/)
1. Lag et selvsignert sertifikat, f.eks: 
    * `openssl req -x509 -newkey rsa:4096 -keyout private-key.pem -out public-key.pem -subj '/CN=Fiks IO Test' -days 365`
    * `openssl pkcs8 -topk8 -inform PEM -outform DER -in private-key.pem -out private_key.der -nocrypt`
1. Ta i bruk "Nasjonal portal for bekymringsmelding" og bruk public key som du laget i steget over og fullfør konfigurasjonsveiviseren
1. Lag en integrasjon og deretter tildel den rettigheter til å Bruke Fiks-IO-kontoen som ble opprettet i steget over
1. Dere trenger også virksomhetssertfikat for test-miljøet

Oppdater konfigurasjon.properties og start applikasjonen med oppstartsparametre `-k sti/konfigurasjon.properties`
