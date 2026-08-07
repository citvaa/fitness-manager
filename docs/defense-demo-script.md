# Scenario za demonstraciju na odbrani (5–10 minuta)

## Priprema dan ranije

1. Pokrenuti Postgres i Redis: `docker compose -f Docker/docker-compose.yaml up -d`.
2. Pokrenuti backend iz `Backend/demo/` sa ispravnim `JWT_SECRET` i, za AI deo, `ANTHROPIC_API_KEY`: `mvnw.cmd spring-boot:run`.
3. Pokrenuti frontend iz `Frontend/`: `npm run dev`.
4. Proveriti `http://localhost:5173` i Swagger na `http://localhost:8088/swagger-ui/index.html`.
5. Prijaviti se jednom sa sva tri demo naloga i proveriti lozinke:
   - menadžer: `admin` / `admin`
   - trener: `ogi` / `ogi`
   - klijent: `citva` / `citva`
6. U Swaggeru, kao menadžer, napraviti trenerski raspored i termin koji povezuje trenera ID `1` i klijenta ID `1`. Termin mora biti unutar radnog vremena trenera. Na svežoj bazi prvo napraviti raspored preko `POST /api/schedule/trainer`, a zatim termin preko `POST /api/appointment`.
7. Kao trener uneti bar dva merenja sa različitim datumima i jedan lični rekord, da grafikon i AI rezime imaju jasan sadržaj.
8. Regenerisati menadžerski i klijentski AI rezime dok internet radi. Ne brisati Redis pre odbrane: keširani odgovori su koristan rezervni prikaz.
9. Otvoriti dva taba: frontend na stranici **Plan uživo** i Swagger autorizovan manager tokenom na `POST /api/gym/occupancy/check-ins`.

## Demonstracija uživo

### 0:00–1:00 — Ulaz i kontekst

1. Ulogovati se kao `admin` / `admin`.
2. Reći: „Aplikacija je jedinstven sistem za operativno upravljanje teretanom. Prikazaću tri povezana stuba: prostorni plan uživo, AI uvid za menadžera i praćenje napretka klijenta.“
3. Skrenuti pažnju na naziv taba **Fitness Manager · GymOS**, brendirani interfejs i manager navigaciju.

### 1:00–3:30 — Plan i promena zauzetosti uživo

1. Otvoriti **Editor sala**. Izabrati jednu salu, pomeriti je nekoliko piksela i pustiti miš kako bi se pozicija sačuvala. Reći: „Plan se čuva kao pravougaona geometrija u logičkom koordinatnom sistemu, pa isti raspored radi na različitim ekranima.“
2. Vratiti se na **Plan uživo** i pokazati status „Uživo povezano“, ukupni kapacitet i izvorne brojeve `check-in` / `zakazano`.
3. Preći u već otvoren Swagger tab i izvršiti `POST /api/gym/occupancy/check-ins`:

   ```json
   { "roomId": 1, "clientId": 1 }
   ```

   Ako je sala ID 1 promenjena/obrisana, uzeti važeći ID iz `GET /api/gym/rooms`.
4. Odmah se vratiti na frontend tab. Broj u sali i gornje metrike treba da se promene bez refresh-a. Reći: „REST daje determinističko početno stanje, a STOMP/WebSocket šalje kompletan snapshot nakon promene i na svakih 60 sekundi.“
5. Po želji u Swaggeru pokušati drugi check-in istog klijenta u drugu salu. Očekivan je HTTP 409. Reći: „Baza i servis zajedno garantuju da klijent može imati samo jedan aktivan check-in u celoj teretani.“

### 3:30–5:00 — AI uvidi menadžera

1. Otvoriti **AI uvidi**.
2. Pokazati vreme generisanja i pinned model. Reći: „Servis agregira poslednjih 30 dana, a Claude dobija samo sažet operativni kontekst. Odgovor se kešira šest sati da se smanje latencija i trošak.“
3. Naglasiti da „prihod“ trenutno znači broj uplata i prodatih termina, jer postojeći model nema cenu ni valutu; sistem eksplicitno zabranjuje modelu da izmisli novčani iznos.
4. Dugme **Regeneriši** koristiti samo ako je internet stabilan.

### 5:00–7:30 — Napredak klijenta i kontrola pristupa

1. Odjaviti managera i prijaviti se kao `ogi` / `ogi`.
2. Otvoriti **Praćenje napretka**, izabrati `citva`, pokazati grafikon, rekord i AI rezime.
3. Uneti novo merenje i sačuvati ga. Reći: „Upis automatski poništava jednosatni AI keš, pa sledeći rezime koristi nove podatke.“
4. Odjaviti se i prijaviti kao `citva` / `citva`. Pokazati isti grafikon i rekord bez formi za izmenu. Reći: „Klijent ima samo read-only self rute; trener vidi samo klijente sa kojima ima postojeći termin.“

### 7:30–8:00 — Zaključak

Reći: „Tri funkcionalnosti dele isti auditable PostgreSQL model: operativno stanje prostora, istoriju napretka i AI sloj koji sumira proverljive podatke. Autorizacija, keš i real-time transport ostaju na backendu, dok frontend samo prikazuje dozvoljene tokove.“

## Plan B na dan odbrane

- **Nema interneta ili Claude API ne odgovara:** ne pritiskati **Regeneriši**. Pokazati prethodno keširan odgovor i screenshotove u `docs/browser-qa/`. Objasniti da nekonfigurisani AI endpoint namerno vraća 503, a testovi koriste fake klijent i nikada ne troše pravi API budžet.
- **WebSocket se prekine:** UI prikazuje poruku o prekidu i automatskom povezivanju. REST snapshot i dalje ostaje vidljiv; osvežiti stranicu za novo početno stanje.
- **Check-in već postoji:** prvo izvršiti `POST /api/gym/occupancy/check-outs/1`, zatim ponoviti check-in.
- **Trener ne vidi klijenta:** proveriti da postoji termin sa `trainerId: 1` i `clientIds: [1]`, kao i trenerski raspored koji obuhvata termin. Ne menjati bazu ručno.
- **Frontend nije dostupan:** pokazati iste tokove kroz Swagger i već pripremljene screenshotove. Backend, autorizacija, 409 pravilo i AI 503/cached ponašanje i dalje se mogu demonstrirati nezavisno.
- **Potpuni lokalni kvar:** imati otvorene finalne screenshotove i kratki snimak live promene kao rezervu; zatim objasniti test/fresh-volume rezultate iz završnog zapisa u `AGENTS.md`.
