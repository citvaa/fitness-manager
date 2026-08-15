# Kompletan scenario za demonstraciju na odbrani

Kompletan obilazak traje približno 15–20 minuta. Koraci označeni sa **CORE** čine preporučenu demonstraciju od 8–10 minuta. Stavke u odeljku **AKO IMA VREMENA / PITANJA** pokazuju širinu sistema, ali nisu neophodne za osnovnu priču.

## Priprema dan ranije

1. Pokrenuti Postgres i Redis: `docker compose -f Docker/docker-compose.yaml up -d`.
2. Uraditi čist backend build iz `Backend/demo/`: `mvnw.cmd clean test`, pa pokrenuti `mvnw.cmd spring-boot:run`. Za AI deo je potreban važeći `ANTHROPIC_API_KEY`, a za aktivaciju važeći Gmail App Password u `.env`.
3. Pokrenuti frontend iz `Frontend/`: `npm install`, zatim `npm run dev`.
4. Proveriti frontend na `http://localhost:5173` i Swagger na `http://localhost:8088/swagger-ui/index.html`.
5. Po potrebi vratiti kompletan relativni demo skup preko manager-only `POST /api/dev/reseed`. Ovo briše postojeće operativne podatke, pa se radi samo na namenskoj demo bazi.
6. Proveriti glavne naloge:
   - menadžer: `admin` / `admin`
   - trener: `marko.trener@momentum.demo` / `Demo123!`
   - klijent: `jelena.klijent@momentum.demo` / `Demo123!`
7. U tri odvojena browser profila ili privatna prozora prijaviti managera, Marka i Jelenu. Tako WebSocket notifikacije ostaju vidljive bez stalnog odjavljivanja.
8. Proveriti da današnji ili naredni dan sadrži termine sa sobom i rosterom. Za check-in demonstraciju izabrati termin koji još nije počeo; manager vidi sve preostale današnje termine, a trener najviše svoja naredna dva.
9. Na **AI uvidima** unapred pritisnuti **Regeneriši**. Ne prazniti Redis pred odbranu: keširani strukturirani rezultat je plan B ako model ili internet nisu dostupni.
10. Za aktivacioni tok koristiti jedinstvenu plus-adresu sandučeta čiji inbox demonstrator može da otvori, na primer `nalog+odbrana-1530@example.com`. Aktivacioni ključ više nije deo API odgovora niti se prikazuje u aplikaciji.
11. Proveriti da jedan klijent ima dug i da Jelena ima budući dostupan termin udaljen bar 24 sata. Sačuvati nekoliko aktuelnih screenshotova kao rezervu.

Seeder pravi pet trenera, pedeset klijenata, relativne rasporede i termine, istoriju uplata/check-in-a i podatke napretka. Nije potrebna ručna izmena baze.

## Preporučeni CORE tok (8–10 minuta)

### 0:00–1:00 — Poziv, aktivacija i bezbedna rola

1. Kao `admin` otvoriti **Administracija** → **Klijenti** i kreirati klijenta sa jedinstvenom email adresom.
2. Pokazati poruku „Nalog kreiran, aktivacioni email poslat na …”. U API odgovoru nema registracionog ključa.
3. Otvoriti pristigli email, pratiti jednokratni link, postaviti lozinku i prijaviti novi nalog.
4. Reći: „Poziv se dostavlja samo emailom. Svaki nalog ima tačno jednu operativnu rolu — manager, trener ili klijent — dok je ADMIN nepromenljiva dodatna privilegija seedovanog managera.”

Plan B: ako SMTP kasni, pokazati potvrdu slanja i nastaviti sa seeded Jeleninim nalogom. Ne čitati ključ iz baze niti iz API-ja. Ako je adresa zauzeta, dodati trenutni minut u plus-sufiks.

### 1:00–2:20 — Kalendar i životni ciklus termina

1. U Jeleninoj sesiji otvoriti **Zakaži trening**, izabrati budući datum u mesečnom kalendaru i rezervisati stavku iz **Dostupni termini**.
2. Otvoriti odvojenu stranicu **Moji termini**. Pokazati da ona sadrži samo Jelenine rezervacije: prošli dan prikazuje održane, budući predstojeće, a današnji obe grupe. Marketplace postoji samo na **Zakaži trening**.
3. Prebaciti se na Markovu sesiju, otvoriti **Moji termini** i kalendarom pokazati isti prošli/danas/budući obrazac. Neradni i praznični datumi su prigušeni, a datumi sa terminima i dalje imaju svoj indikator.
4. Ako postoji slobodan termin, Marko ga preuzima. Reći: „Preuzimanje otvorenog termina jedini je slučaj u kome sistem može automatski napraviti tačnu radnu smenu; običan manager termin i dalje prolazi punu proveru dostupnosti.”

Plan B: ako na izabranom datumu nema ponude, izabrati drugi označeni budući dan. Rezervaciju za povrat kredita otkazivati samo kada je udaljena najmanje 24 sata.

### 2:20–3:35 — Appointment-scoped check-in uživo

1. Kao manager otvoriti **Plan uživo**. Pokazati sve preostale današnje termine, sortirane po vremenu; slobodan izbor bilo kog klijenta ili sobe više ne postoji.
2. Otvoriti termin sa sobom i rosterom, pritisnuti **Započni trening**, pa prijaviti jednog klijenta. Soba je unapred određena terminom, a akcije postoje samo za članove rostera.
3. Pokazati promenu zauzetosti bez osvežavanja stranice, zatim uraditi check-out.
4. U Markovoj sesiji pokazati isti obrazac za njegova najviše dva naredna termina.
5. Reći: „REST daje početni snapshot, a STOMP prenosi promenu zauzetosti. Manager nadgleda celu teretanu, dok trener vidi samo svoj neposredni operativni kontekst.”

Plan B: ako je demonstracija posle poslednjeg današnjeg termina, pokažite prazno stanje i pređite na **Dnevni raspored** za seeded datum. Pre odbrane je bolje reseedovati dok još postoji predstojeći termin nego ručno menjati bazu.

### 3:35–4:35 — Strukturirani AI uvidi

1. Kao manager otvoriti **AI uvidi**. Na vrhu pokazati kratak rezime i 2–4 preporuke.
2. Zatim pokazati pojedinačne kartice: popunjenost svake sale, odnos individualnih i grupnih termina, check-in/pohodu i kupljene termine. Svaka kartica ima broj izračunat u Javi, obojeni `EXCELLENT/GOOD/AVERAGE/POOR` bedž i kratak komentar za baš tu metriku.
3. Reći: „Model ocenjuje i komentariše proverljive agregate, ali ih ne izračunava. Ako izostavi ili pokvari jedan element, taj element dobija bezbedan AVERAGE fallback i ostatak stranice ostaje upotrebljiv.”
4. **Regeneriši** koristiti samo ako su mreža i API ključ provereni; inače pokazati keširani rezultat.

### 4:35–5:35 — Uplate i stvarni dug

1. Kao manager otvoriti **Plaćanja** i izabrati klijenta u filteru. Pokazati status kartice za svaki tip sesije.
2. Objasniti: `održano` se računa iz termina čiji su datum i kraj prošli, `plaćeno` iz evidentiranih uplata, a dug je `max(0, održano - plaćeno)`.
3. Evidentirati malu uplatu. U Jeleninoj sesiji otvoriti centar notifikacija i pokazati potvrdu uplate, zatim **Moje uplate** sa istim deljenim prikazom statusa.
4. Reći: „Plaćanje unapred ne pravi negativan dug, a tipovi bez aktivnosti se ipak prikazuju. Aplikacija prati broj termina, ne novčani iznos.”

Plan B: ako izabrani klijent nema dug, odabrati seeded dužnika. Ne praviti veliku probnu uplatu koju je teško objasniti ili vratiti.

### 5:35–6:25 — Rasporedi i manager granica

1. Kao Marko otvoriti **Moj raspored**. Kalendar prigušuje lične neradne dane i praznike; radne smene se menjaju kroz self-service formu.
2. Sačuvati bezopasnu buduću smenu i pokazati „Sačuvano ✓”/osveženo serversko stanje.
3. Kao manager otvoriti **Rasporedi i neradni dani**. Pokazati radno vreme i praznike, ali naglasiti da je raspored izabranog trenera samo za čitanje — manager više ne može da mu proizvoljno upisuje smenu.
4. Reći: „Backend izvodi trenera iz JWT-a i legacy write rute takođe odbijaju managera sa 403; uklanjanje forme nije jedina zaštita.”

### 6:25–7:30 — Manager kalendari i fiksni termin

1. Otvoriti **Dnevni raspored** i mesečnim kalendarom promeniti datum; pokazati objedinjene termine, sale, trenere i klijente.
2. Otvoriti **Upravljanje terminima**. Kreirati običan termin sa obaveznom sobom ili uključiti fiksni termin.
3. Kod fiksnog termina objasniti osam nedeljnih pokušaja: svaka nedelja prolazi iste provere trenera, sobe, radnog vremena, praznika i preklapanja; neuspešna nedelja se preskače, a odgovor prikazuje broj kreiranih i razloge.
4. Po želji pokazati da **Dodaj klijenta** dropdown radi nad stvarnim rosterom i kandidati nestaju nakon dodavanja.

Plan B: koristite datum/sat za koji trener već ima WORKING smenu. Soba je obavezna; „Bez sobe” više nije validna opcija.

### 7:30–8:30 — Notifikacije za sve tri role

1. Držati otvoren centar notifikacija u tri sesije.
2. Pokazati manager alert nastao kada je Jelena samostalno rezervisala termin.
3. Pokazati trenerovu notifikaciju nakon manager dodele termina ili postojeći dnevni/assignment događaj.
4. Pokazati Jeleninu potvrdu evidentirane uplate. U sidebar-u se vidi istorija i izbor preference `EMAIL` / `PUSH` / `BOTH` za korisničke notifikacije; manager operativni broadcast ne zavisi od preference.
5. Reći: „Manager sluša `/topic/manager`, trener i klijent svoje korisničke teme, a zauzetost ima zaseban gym broadcast.”

Plan B: WebSocket kartica mora pisati da je veza uspostavljena. Ako reconnect traje, osvežiti jednu sesiju i izazvati novi, bezopasan događaj.

### 8:30–10:00 — Napredak i zaključak

1. Kao Marko otvoriti **Praćenje napretka**, izabrati Jelenu i pokazati sva telesna merenja, grafikon izabrane vežbe i AI sažetak/preporuku.
2. Ako je vreme bezbedno, dodati i obrisati jedno probno merenje koristeći in-app potvrdu umesto browser `confirm()` dijaloga.
3. Zaključiti: „Sistem pokriva email aktivaciju, role-scoped administraciju, plan i check-in uživo, kalendarsko planiranje, booking, dug i uplate, strukturiranu AI analitiku, napredak i notifikacije. Autoritativna pravila ostaju na backendu; frontend prikazuje samo dozvoljeni tok.”

## AKO IMA VREMENA / PITANJA

- **Administracija naloga:** pretraga i paginacija, srpske labele statusa trenera, modal za promenu emaila i generički modal za potvrdu brisanja. Kreiranje bira tačno jednu operativnu rolu; ADMIN se ne nudi kao promenljiva rola.
- **Editor sala i teretane:** pomeriti/rotirati salu, sačuvati geometriju i pokazati dropdown IANA vremenskih zona u podešavanjima teretane.
- **Preklapanje rasporeda:** radna smena preko odsustva (ili obrnuto) prvo vraća eksplicitnu overlap potvrdu; rezervisani termin sprečava overwrite.
- **Otpornost booking toka:** pokušati duplu rezervaciju, preuzimanje već dodeljenog termina ili kasno otkazivanje i pokazati čitljivu backend grešku.
- **Bezbednosne granice:** kroz Swagger pokazati 403 za manager write tuđeg trenerskog rasporeda, CLIENT pristup manager endpointu i pokušaj promene ADMIN role.
- **Manager roster:** na **Upravljanje terminima** dodati pa ukloniti klijenta; dropdown je uživo potvrđen nad stvarnim API oblikom i nije zahtevao spekulativnu izmenu.
- **Klijentski read-only napredak:** kao Jelena pokazati iste grafikone bez trenerskih formi za izmenu.
- **Mobilni izgled:** suziti prozor i pokazati da header filteri i forme ne probijaju kartice; `.client-picker` zadržava desktop širinu van payment forme.

## Plan B za potpuni kvar

- Ako frontend ne radi, kroz Swagger pokazati odgovarajuće `/me`, manager date, payment status, recurring appointment i AI endpoint-e. Ne zaobilaziti autorizaciju ručnim SQL-om.
- Ako AI ili internet ne rade, koristiti keširani rezultat/screenshot. Stranica i dalje prikazuje Java metrike i fallback ocene.
- Ako SMTP ne radi, pokazati UI potvrdu i server log greške, pa nastaviti seeded nalogom; ne vraćati aktivacioni ključ u DTO.
- Ako WebSocket ne radi, pokazati REST snapshot, istoriju centra notifikacija iz prethodno otvorene sesije i objasniti reconnect.
- Ako lokalni sistem potpuno otkaže, koristiti aktuelne screenshotove i rezultate backend testova/frontend build-a. Ne oslanjati se na stare slike ekrana sa uklonjenim formama.

## Brza provera redosleda pre izlaska

CORE priča prati jedan proverljiv životni ciklus: manager poziva klijenta → klijent rezerviše → manager/trener rade appointment-scoped operaciju → AI objašnjava agregate → uplata menja status i stiže kao notifikacija → kalendari i rasporedi pokazuju role granice. Pre odbrane obavezno proveriti da demo podaci odgovaraju trenutnom datumu i satu, da su tri WebSocket sesije povezane i da keširani AI odgovor postoji.
