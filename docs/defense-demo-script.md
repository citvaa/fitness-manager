# Vodič za demonstraciju na odbrani

Ovaj dokument je operativni scenario za kratku (5-10 min) demonstraciju
nadogradnje `fitness-manager` sistema (grana `upgrade/claude-code`) na
odbrani diplomskog rada. Pokriva sva tri stuba nadogradnje: **live plan
teretane**, **AI uvide za menadžera**, i **praćenje napretka klijenata**.

Pre odbrane, pročitaj i "Priprema pre odbrane" ispod - radi se jednom, dan
ili sat vremena unapred, ne pred komisijom.

## Nalozi koji se koriste

Sva tri naloga postoje na svakoj svežoj bazi (Flyway migracija
`V1.0017__set_known_dev_test_passwords.sql`, samo na `dev` profilu):

| Email   | Lozinka       | Uloga   | Koristi se za                          |
|---------|---------------|---------|-----------------------------------------|
| `admin` | `password123` | MANAGER | Editor sala, live plan, AI uvidi        |
| `ogi`   | `password123` | TRAINER | Unos mere/rekorda, "Moji klijenti"      |
| `citva` | `password123` | CLIENT  | Read-only prikaz sopstvenog napretka    |

Dev-seed podaci (`V1.0016`-`V1.0018`) već povezuju `ogi` (trener) i `citva`
(klijent) kroz jedan zajednički termin, tako da je "Moji klijenti" popunjeno
odmah bez ručne pripreme - i već postoji gym `FitPro Gym` sa 5 sala ("Sala za
tegove", "Kardio zona", "Joga studio", "Svlačionica", "Recepcija").

## Priprema pre odbrane (uradi unapred, ne uživo)

1. Pokreni infrastrukturu i backend:
   `docker compose -f Docker/docker-compose.yaml up -d`, zatim
   `cd Backend/demo && ./mvnw.cmd spring-boot:run` (Windows) sa env
   varijablama iz `.env` učitanim (uključujući `ANTHROPIC_API_KEY` - bez
   njega AI ekrani vraćaju grešku, vidi "Plan B" ispod).
2. Pokreni frontend: `cd Frontend && npm run dev` (`http://localhost:5173`).
3. Otvori **dva browser taba/prozora** unapred:
   - **Tab A**: prijavljen kao `admin`, na `/manager/floor-plan` (live plan).
   - **Tab B**: Swagger UI (`http://localhost:8088/swagger-ui/index.html`) ILI
     terminal sa `curl` spreman za check-in komandu (vidi korak 3 ispod) - ovo
     je "daljinski upravljač" za uživo promenu zauzetosti dok komisija
     gleda Tab A.
4. Po potrebi pribavi ID sale i ID klijenta unapred (da ne kucaš uživo):
   `GET /api/gym/room` (kao `admin`) → zapamti ID sale "Sala za tegove";
   `GET /api/trainer/me/clients` (kao `ogi`) → zapamti ID klijenta `citva`.
   Na svežoj dev bazi ovo su tipično `roomId=1`, `clientId=1`, ali provjeri
   uvek iznova - ID-jevi zavise od redosleda kreiranja podataka.
5. Uveri se da AI uvidi već imaju keširan, svež odgovor (pozovi
   `GET /api/insights/manager` i `GET /api/progress/insight/client/{id}`
   jednom pre nego što komisija uđe) - prvi (necache-ovan) pozivi Claude API-ju
   traju par sekundi, drugi poziv unutar TTL-a je trenutan. Ne mora da bude
   "sveže" tokom demonstracije - keširan odgovor je i brži i pouzdaniji.

## Scenario (redosled)

### 1. Uvod i prijava (~1 min)

Prijaviti se kao `admin` (ako Tab A već nije prijavljen, uradi to uživo -
inače samo reci "već sam prijavljen kao menadžer"). Reci: *"Sistem
razlikuje tri uloge - menadžer, trener i klijent - i svaka uloga vidi svoj
deo aplikacije. Krenimo od menadžera."*

### 2. Editor sala (~1 min)

Otvori `/manager/room-editor`. Pokaži postojeće sale na 2D planu, prevuci
jednu salu (drag), pokaži da se pozicija menja uživo na canvas-u. Reci:
*"Menadžer ovde crta stvarni raspored teretane - pravougaonici sa rotacijom,
mapirano 1:1 na `react-konva` canvas biblioteku. Ovo se čuva u bazi čim
prevlačenje završi."* Ne moraš da praviš novu salu - samo pokaži da postojeća
reaguje na drag. Refresh stranice (F5) da pokažeš da je pozicija zaista
sačuvana, ne samo lokalna.

### 3. Live plan teretane - GLAVNI "wow" trenutak (~2-3 min)

Ovo je najefektniji deo - **osmišljen da se odigra pred komisijom u realnom
vremenu, ne kao snimljen video**:

1. Pređi na `/manager/floor-plan` u Tab A. Pokaži legendu boja (sivo/zeleno/
   žuto/crveno) i objasni: *"Ovo je uživo prikaz zauzetosti - kombinuje dva
   signala: ručne prijave na recepciji i klijente koji su trenutno na
   terminu zakazanom u toj sali, bez dupliranja provere - ako je isti
   klijent i prijavljen i na terminu, računa se dvaput po dizajnu, jer je
   ovo indikator zauzetosti prostora, ne tačan broj glava."*
2. U Tab B (Swagger ili terminal), pošalji check-in poziv:
   ```
   curl -X POST "http://localhost:8088/api/gym/room/{roomId}/check-in?clientId={clientId}" \
     -H "Authorization: Bearer {admin_access_token}"
   ```
   (Ako koristiš Swagger UI: uloguj se tamo preko `/api/user/login`, kopiraj
   `accessToken`, klikni "Authorize", pa pozovi
   `POST /api/gym/room/{roomId}/check-in`.)
3. **Bez ikakvog refresh-a**, pločica sale u Tab A treba da promeni boju iz
   sivo/zeleno u sledeći nivo, u realnom vremenu - to je STOMP/WebSocket push
   na `/topic/gym/occupancy`. Reci: *"Nijedan reload - server je gurnuo ovu
   promenu kroz WebSocket čim se desio check-in."*
4. Pošalji check-out (`POST /api/gym/check-in/{checkInId}/check-out` -
   `checkInId` je vraćen u odgovoru na check-in poziv) i pokaži da se pločica
   vrati na prethodnu boju - takođe uživo.
5. (Opciono, ako ima vremena) Pokušaj da prijaviš istog klijenta u DRUGU
   salu bez check-out-a prvo - pokazaće `409 Conflict` sa porukom da klijent
   već ima aktivnu prijavu. Reci: *"Ovo pravilo je sprovedeno i na nivou
   baze, ne samo u kodu - jedinstveni indeks u Postgresu odbija drugi upis
   čak i pri konkurentnom pokušaju."*

### 4. AI uvidi za menadžera (~1-2 min)

Pređi na `/manager/insights`. Pokaži narativ generisan modelom
(`claude-haiku-4-5`) na osnovu stvarnih podataka o prijavama i uplatama.
Reci: *"Ovo nije šablon - model dobija agregirane brojeve (broj prijava po
sali, plaćeni termini po tipu sesije kao proksi za prihod, jer šema nema
cenu po terminu) i piše sažetak na srpskom."* Klikni "Regeneriši" da pokažeš
da forsira novi Claude API poziv (nekoliko sekundi čekanja je normalno i
očekivano - to je live poziv, ne trik).

### 5. Praćenje napretka klijenata (~2 min)

Odjavi se, prijavi se kao `ogi` (TRAINER). Otvori `/trainer` - pokaži "Moji
klijenti" (samo `citva`, jer je jedini klijent sa kojim `ogi` deli termin -
reci da je ovo stvarna autorizaciona provera, ne samo UI filter: *"Trener ne
može ni preko API-ja da pročita podatke klijenta kog nikad nije trenirao -
vraća 403."*). Klikni na `citva`, unesi novu meru (npr. težina) kroz formu,
pokaži da se grafik ažurira odmah bez reload-a. Unesi i lični rekord.

Odjavi se, prijavi se kao `citva` (CLIENT). Otvori `/client` i pokaži da
klijent vidi iste podatke koje je trener upravo uneo - read-only, bez formi.
Reci: *"Isti podaci, ali klijent nema mogućnost izmene - samo uvid."*

### 6. Zaključak (~30s)

*"Sve tri funkcionalnosti - live plan, AI uvidi, praćenje napretka - su
potpuno integrisane sa postojećim sistemom zakazivanja termina i uplata, bez
narušavanja postojeće funkcionalnosti."*

## Plan B - ako nešto ne radi na dan odbrane

- **Nema interneta / Anthropic API ne radi**: AI uvidi (koraci 4 i deo
  koraka 5) su jedini delovi koji zahtevaju spoljnu mrežu. Ako
  `GET /api/insights/manager` ili `GET /api/progress/insight/...` vrate
  grešku, prijaviti to otvoreno ("ovo zove eksterni Claude API, koji zavisi
  od internet konekcije") i pokazati unapred snimljene screenshotove iz
  `docs/browser-qa/phase4-*.jpg` kao dokaz da funkcionalnost radi kada je
  mreža dostupna. **Zato uradi korak 5 iz "Priprema" unapred** - ako je
  odgovor već keširan (`MANAGER_INSIGHTS_CACHE` 30 min TTL,
  `CLIENT_PROGRESS_INSIGHT_CACHE` 10 min TTL), stranica će prikazati
  keširan sadržaj čak i ako je mreža u tom trenutku nedostupna za NOVI
  poziv - samo nemoj klikati "Regeneriši" ako nisi siguran da mreža radi.
- **WebSocket se ne poveže / veza padne uživo (korak 3)**: frontend sada
  prikazuje vidljiv baner ("Veza sa uživo prikazom je prekinuta, ponovno
  povezivanje...") umesto da tiho zamrzne prikaz - ako se to desi, reci
  otvoreno *"evo, ovo je tačno ponašanje koje želimo kad veza padne - sistem
  javlja korisniku, ne ćuti"* i uradi F5 refresh da ponovo učitaš početni
  HTTP snapshot, zatim ponovi check-in poziv.
- **Backend/frontend ne startuje uopšte**: imaj already-running instancu
  pokrenutu unapred (vidi "Priprema") - ne pokušavaj da pokreneš bilo šta od
  nule pred komisijom. Ako i to otkaže, koristi screenshotove iz
  `docs/browser-qa/` i objasni šta bi se videlo uživo.
- **Zaboravljena lozinka/nalog ne radi**: sve tri lozinke su `password123`
  za `admin`/`ogi`/`citva` - ako login ne radi, provjeri da li je baza
  stvarno na `dev` Flyway profilu (`spring.profiles.active=dev`) - samo taj
  profil učitava `V1.0017`.
- **Check-in vrati grešku umesto 409/201**: provjeri da li si stvarno prosledio
  ispravan `roomId`/`clientId` (pogledaj "Priprema", korak 4) - ID-jevi se
  menjaju ako je baza ranije korišćena za drugu demonstraciju.

## Tehnički podsetnik (za pitanja komisije)

Detaljno obrazloženje svake netrivijalne odluke (šema, servisni sloj,
frontend) je u `AGENTS.md`, sekcije "Upgrade: schema decisions", "Upgrade:
service layer decisions", "Upgrade: frontend decisions", i finalni pregled
u "Upgrade: final summary" - koristi ih kao referencu ako komisija pita
"zašto baš tako", npr. zašto je soba pravougaonik a ne poligon, zašto
occupancy ne dedupira dva signala, ili zašto je izabran `claude-haiku-4-5`.
