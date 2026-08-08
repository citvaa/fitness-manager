# Kompletan scenario za demonstraciju na odbrani

Kompletan obilazak traje približno 15–20 minuta. Koraci označeni sa **CORE** čine preporučenu demonstraciju od 8–10 minuta i moraju da se pokažu. Koraci označeni sa **AKO IMA VREMENA / PITANJA** pokazuju širinu aplikacije, ali se preskaču kada je vreme ograničeno.

## Priprema dan ranije

1. Pokrenuti Postgres i Redis: `docker compose -f Docker/docker-compose.yaml up -d`.
2. Pokrenuti backend iz `Backend/demo/` sa `JWT_SECRET` od najmanje 32 znaka i, za AI deo, važećim `ANTHROPIC_API_KEY`: `mvnw.cmd spring-boot:run`.
3. Pokrenuti frontend iz `Frontend/`: `npm run dev`.
4. Proveriti frontend na `http://localhost:5173` i Swagger na `http://localhost:8088/swagger-ui/index.html`.
5. Proveriti glavne naloge iz relativnog dev seedera:
   - menadžer: `admin` / `admin`
   - trener: `marko.trener@momentum.demo` / `Demo123!`
   - klijent: `jelena.klijent@momentum.demo` / `Demo123!`
6. Ulogovati se sa sva tri naloga. Proveriti da klijent ima dostupne buduće termine, a trener bar jedan termin bez trenera u marketplace-u.
7. Regenerisati menadžerski i jedan klijentski AI rezime dok internet radi. Ne prazniti Redis pre odbrane: keširani odgovor je koristan plan B.
8. Izabrati jednu buduću smenu/termin dovoljno daleko od dana odbrane. Klijentsko otkazivanje mora biti najmanje 24 sata pre početka.
9. Otvoriti rezervni Swagger tab sa manager tokenom i sačuvati screenshotove iz `docs/browser-qa/` lokalno.

Nije potrebna ručna intervencija u bazi. Seeder na praznom dev okruženju sam pravi relativne rasporede, termine, uplate, istoriju check-in-a i podatke napretka.

## Preporučeni CORE tok (8–10 minuta)

### 0:00–1:15 — Registracija i aktivacija naloga

1. Prijaviti se kao `admin` / `admin` i otvoriti **Administracija** → **Klijenti**.
2. Kreirati klijenta sa jedinstvenim emailom, na primer `odbrana.klijent@example.com`.
3. U modalu **Dev / demo način** naglas pokazati aktivacioni link i otvoriti ga u novom tabu.
4. Uneti lozinku `Demo123!`, završiti aktivaciju i prijaviti se novim nalogom.
5. Reći: „Produkcioni tok je email-first. Demo samo prikazuje isti jednokratni aktivacioni URL zato što odbrana ne zavisi od SMTP kredencijala; nalog se ne kreira na javnoj stranici.”

Plan B: ako je email već iskorišćen, dodati trenutni minut u adresu. Ako aktivacioni tab nije otvoren, kopirati link iz modala. Ne vaditi ključ direktno iz baze.

### 1:15–2:15 — MANAGER administracija

1. Vratiti se na manager sesiju i otvoriti **Rasporedi**.
2. Promeniti radno vreme jednog dana za 15 minuta i sačuvati, ili dodati budući praznik „Odbrana sistema”.
3. Pokazati da se promena odmah nalazi na listi.
4. Reći: „Menadžer upravlja nalozima odvojeno od domenskih profila, radnim vremenom, praznicima, trenerskim rasporedima, dnevnim kalendarom i paketima termina. Ne postoji ručno održavanje baze.”

Plan B: ako izabrani praznik već postoji, uzeti drugi budući datum. Ako promena radnog vremena preseca demo smenu, samo vratiti početnu vrednost nakon prikaza.

### 2:15–4:15 — CORE wow stub 1: plan i zauzetost uživo

1. Otvoriti **Editor sala**, pomeriti jednu salu nekoliko piksela i sačuvati. Reći: „Rotirani pravougaonici se čuvaju u fiksnom logičkom koordinatnom sistemu, pa plan nije vezan za rezoluciju ekrana.”
2. Otvoriti **Plan uživo** i pokazati „Uživo povezano”, kapacitet i odvojene brojeve `check-in` / `zakazano`.
3. U Swagger tabu izvršiti `POST /api/gym/occupancy/check-ins` sa važećim `roomId` i `clientId`; identifikatore po potrebi uzeti iz `GET /api/gym/rooms` i `GET /api/client`.
4. Vratiti se na frontend bez refresh-a i pokazati promenjen broj. Reći: „REST daje početni snapshot, a STOMP šalje isto kompletno stanje posle događaja i periodično na 60 sekundi.”
5. Ako vreme dopušta, pokušati check-in istog klijenta u drugu salu i pokazati HTTP 409.

Plan B: ako klijent već ima otvoren check-in, prvo ga odjaviti odgovarajućim check-out endpointom. Ako WebSocket nije povezan, osvežiti stranicu i pokazati REST snapshot uz objašnjenje automatskog reconnect-a.

### 4:15–5:15 — CORE wow stub 2: AI uvidi menadžera

1. Otvoriti **AI uvidi** i pokazati vreme generisanja i pinned Claude Haiku model.
2. Reći: „Backend agregira proverljive podatke za poslednjih 30 dana i kešira rezultat šest sati. Uplate nemaju cenu ni valutu, pa je prihod eksplicitno označen kao proxy broja kupljenih termina i model ne sme da izmisli novčani iznos.”
3. **Regeneriši** koristiti samo kada su internet i API ključ provereni.

Plan B: pokazati keširani odgovor ili `docs/browser-qa/manager-ai-insights-plain-text.png`. Nekonfigurisan AI namerno vraća 503; testovi koriste fake Claude granicu i ne troše API budžet.

### 5:15–6:30 — CORE wow stub 3: napredak klijenta

1. Odjaviti managera i prijaviti se kao `marko.trener@momentum.demo` / `Demo123!`.
2. Otvoriti **Praćenje napretka**, izabrati `jelena.klijent@momentum.demo` i pokazati grafikon, lične rekorde i AI narativ.
3. Dodati jedno novo merenje. Reći: „Trener vidi samo klijente sa kojima ima istoriju termina; upis odmah poništava jednosatni AI keš.”
4. Ako vreme dopušta, prijaviti se kao Jelena i pokazati isti sadržaj bez formi za izmenu: klijentske rute su self-scoped i read-only.

Plan B: ako izabrani trener ne vidi klijenta, izabrati drugi par koji se pojavljuje u seeder podacima; ne dodavati vezu ručno u bazu.

### 6:30–7:30 — TRENER self-service raspored

1. Kao Marko otvoriti **Moj raspored**.
2. Dodati buduću smenu unutar radnog vremena teretane ili kratko odsustvo, zatim izmeniti sopstvenu smenu.
3. Reći: „Frontend ne šalje autoritativni trainer ID. Backend ga izvodi iz JWT-a i pri izmeni/brisanja ponovo proverava vlasništvo; pokušaj izmene tuđeg reda vraća 403.”

Plan B: ako se smena preklapa sa seeded smenom, izabrati nedelju posle poslednjih demo termina ili dodati odsustvo. Vreme van radnog vremena treba da vrati čitljiv 400, ne 500.

### 7:30–9:15 — Booking marketplace: klijent, pa trener, pa otkazivanje

1. Prijaviti se kao `jelena.klijent@momentum.demo` / `Demo123!`, otvoriti **Zakaži trening** i rezervisati budući termin iz **Dostupni termini**.
2. Pokazati da se termin premestio u **Predstojeći termini** i da postoji istorija treninga.
3. Prijaviti se kao Marko, otvoriti **Moji termini** i u **Termini bez trenera** preuzeti slobodan budući termin. Ako je klijentov termin već imao trenera, naglasiti da su rezervacija mesta i dodela trenera nezavisni koraci marketplace modela.
4. Kao Marko pokazati **Otkaži dodelu**, ili ostaviti dodelu i ponovo se prijaviti kao Jelena.
5. Kao Jelena otkazati rezervaciju termina koji je udaljen više od 24 sata. Pokazati da se kredit vraća i da termin nestaje iz njenih predstojećih termina.
6. Reći: „Sve `/me` rute izvode profil iz JWT-a. Marketplace ne prikazuje prošle/pune termine, trener ne može preuzeti već dodeljen termin, a klijent ne može duplirati rezervaciju niti otkazati unutar 24 sata.”

Plan B: ako nema otvorenog termina, koristiti drugi seeded trener/klijent nalog ili managerom napraviti budući termin kroz postojeći UI/API. Ako je otkazivanje onemogućeno, izabrati termin udaljen više od 24 sata — ne menjati datum u bazi.

### 9:15–10:00 — Zaključak

Reći: „Nadogradnja sada pokriva ceo životni ciklus: poziv i aktivaciju naloga, manager administraciju, operativni plan uživo, AI analitiku, napredak klijenta, trenerski self-service i booking. Autorizacija, poslovna pravila, audit, keš i real-time transport ostaju na backendu; frontend prikazuje samo role-scoped tokove.”

## AKO IMA VREMENA / PITANJA

- **Administracija naloga:** pretraga/paginacija korisnika, role, izmena Client/Trainer profila i brisanje profila koje uklanja samo odgovarajuću rolu, ne ceo User.
- **Plaćanja:** manager filtrira istoriju po klijentu i evidentira paket termina; klijent na **Moje uplate** vidi samo sopstvene redove. Terminologija je „kupljeni termini”, ne valuta.
- **Dnevni raspored:** manager-only vremenska linija objedinjuje termine, trenere i klijente za datum; treneri i klijenti nemaju pristup tom globalnom preseku.
- **Bezbednosne granice:** kroz Swagger pokazati 403 za tuđi self-service raspored ili neovlašćen appointment pristup. `AccessDeniedException` se eksplicitno prevodi u 403, dok validacione greške ostaju 400.
- **Otpornost booking toka:** pokušati duplu rezervaciju, preuzimanje već dodeljenog termina ili kasno otkazivanje i objasniti zašto backend odbija zahtev.

## Plan B za potpuni kvar

- Frontend screenshotovi su u `docs/browser-qa/`; prikazati ih uz Swagger/API odgovore.
- Ako backend radi, sve ključne akcije mogu da se pokažu kroz Swagger bez ručne SQL intervencije.
- Ako AI ili internet ne rade, koristiti keš/screenshot i pokazati očekivani 503 za nekonfigurisan servis.
- Ako ni lokalni sistem ne radi, pokazati finalne screenshotove i rezultate `mvn test`, frontend build-a i fresh-volume provere zapisane u `AGENTS.md`.

## Brza provera redosleda pre izlaska

Scenario namerno ide redom kojim bi ga pratio novi korisnik: manager poziva nalog → korisnik ga aktivira → manager podešava operacije → pokazuju se tri glavna vizuelna/AI stuba → trener uređuje svoj raspored → klijent i trener završavaju booking životni ciklus. Svaki sledeći korak koristi stanje koje je prethodni korak već objasnio, pa demonstrator ne mora da poznaje internu strukturu koda.
