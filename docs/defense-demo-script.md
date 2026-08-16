# Vodič za demonstraciju na odbrani

Ovaj dokument je operativni scenario za demonstraciju cele nadogradnje
`fitness-manager` sistema (grana `upgrade/claude-code`) na odbrani diplomskog
rada - od samostalne registracije naloga do sve tri "wow" funkcionalnosti,
preko administracije, trenerskog samouslužnog rasporeda i toka zakazivanja
termina.

**Realno ne staje sve u jednu kratku demonstraciju.** Zato je scenario
podeljen na:

- **CORE** - mora da se pokaže, ~10-12 minuta. Ovo je minimalni prikaz koji
  dokazuje da je sistem end-to-end funkcionalan, ne samo da tri izolovane
  "wow" funkcionalnosti radi.
- **AKO IMA VREMENA / PITANJA** - dodatnih ~8-10 minuta materijala,
  spreman za izvođenje ako komisija ima vremena ili konkretno pita o toku
  zakazivanja, trenerskom rasporedu, ili detaljima administracije.

Svaki korak ispod je obeležen sa **[CORE]** ili **[EKSTRA]** u naslovu.

Pre odbrane, pročitaj i "Priprema pre odbrane" ispod - radi se jednom, dan
ili sat vremena unapred, ne pred komisijom.

## Nalozi koji se koriste

Sva tri postojeća naloga postoje na svakoj svežoj bazi (Flyway migracija
`V1.0017__set_known_dev_test_passwords.sql`, samo na `dev` profilu):

| Email   | Lozinka       | Uloga   | Koristi se za                                          |
|---------|---------------|---------|----------------------------------------------------------|
| `admin` | `password123` | MANAGER | Administracija, editor sala, live plan, AI uvidi, uplate |
| `ogi`   | `password123` | TRAINER | Moj raspored, unos mere/rekorda, "Moji klijenti", termini |
| `citva` | `password123` | CLIENT  | Read-only napredak, zakazivanje, moji termini, moje uplate |

Realistični dev-seeder (`DevDataSeeder`, Faza 7) na svakom svežem pokretanju
popuni bazu sa ~110 termina (8 nedelja istorije + 3 nedelje unapred), 4
trenera, 6 klijenata (uključujući `ogi`/`citva`), uplatama, prijavama u sale
i mesecima podataka o napretku - tako da svaki ekran ima realan sadržaj bez
ručne pripreme podataka. Postoji i gym `FitPro Gym` sa 5 sala ("Sala za
tegove", "Kardio zona", "Joga studio", "Boks studio", "TRX sala").

Za korak "Registracija novog naloga" **ne koristi se** postojeći nalog -
kreira se potpuno novi nalog uživo, uz `manager+demo@fitpro.dev` (ili slično)
kao email da se ne pomeša sa seed podacima.

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
     terminal sa `curl` spreman za check-in komandu (vidi korak "Live plan"
     ispod) - ovo je "daljinski upravljač" za uživo promenu zauzetosti dok
     komisija gleda Tab A. Isti tab/terminal se koristi i za registraciju
     (otvaranje aktivacionog linka) i za booking-flow korake.
4. Po potrebi pribavi ID-jeve unapred (da ne kucaš uživo):
   - `GET /api/gym/room` (kao `admin`) → ID sale "Sala za tegove".
   - `GET /api/trainer/me/clients` (kao `ogi`) → ID klijenta `citva`.
   - `GET /api/appointment/without-trainer` (kao `ogi`) → ID bar jednog
     budućeg termina bez trenera, za booking-flow deo.
   Na svežoj dev bazi ovo su tipično `roomId=1`, `clientId=6` (poslednji
   seed-ovani klijent je `citva`), ali provjeri uvek iznova - ID-jevi zavise
   od redosleda kreiranja podataka i od toga koliko puta je seeder već
   pokretan.
5. Uveri se da AI uvidi već imaju keširan, svež odgovor (pozovi
   `GET /api/insights/manager` i `GET /api/progress/insight/client/{id}`
   jednom pre nego što komisija uđe) - prvi (necache-ovan) pozivi Claude API-ju
   traju par sekundi, drugi poziv unutar TTL-a je trenutan. Ne mora da bude
   "sveže" tokom demonstracije - keširan odgovor je i brži i pouzdaniji.
6. Ako planiraš da uživo pokažeš i registraciju, unapred smisli i zapamti
   koji email ćeš koristiti (npr. `demo.klijent@fitpro.dev`) - ako ga
   pokušaš dvaput na istoj bazi, drugi pokušaj će vratiti grešku "korisnik
   već postoji" (očekivano, ne bug).

## Scenario (redosled)

### 0. Uvod i prijava [CORE] (~30s)

Prijaviti se kao `admin` (ako Tab A već nije prijavljen, uradi to uživo -
inače samo reci "već sam prijavljen kao menadžer"). Reci: *"Sistem
razlikuje tri uloge - menadžer, trener i klijent - i svaka uloga vidi svoj
deo aplikacije. Krenimo od toga kako nalog uopšte nastaje."*

### 1. Registracija i aktivacija novog naloga [CORE] (~1-1.5 min)

Ovo demonstrira samostalni onboarding flow dodat u Fazi 6 - pre toga je
jedini način da nalog postoji bio ručni SQL insert.

1. U Tab A (`admin`), otvori `/manager/administracija` → tab "Klijenti" (ili
   "Treneri"). Popuni formu sa novim email-om - **stvarnom adresom do koje
   uživo imaš pristup** (npr. svojim Gmail nalogom), pošto je pravi Gmail
   SMTP ožičen (`MAIL_USERNAME`/`MAIL_PASSWORD` u `.env`) i aktivacioni link
   stiže isključivo na email, ne više preko UI banera - taj dev/demo baner je
   uklonjen u ovoj sesiji. Klikni "Kreiraj".
2. Otvori inbox (na telefonu ili drugom uređaju da ne gubiš Tab A/B) i
   pronađi mejl "Aktivacija naloga" - objasni: *"Ovo više nije placeholder -
   link u mejlu vodi na pravu `FRONTEND_URL` adresu, ne na stari hardkodovani
   `nesto.com` domen."*
3. Otvori link iz mejla u **trećem tabu** (ili incognito prozoru - ostavi Tab
   A i Tab B netaknute) - vodi na `/register/complete?registration_key=...`.
   Postavi lozinku (npr. `Demo1234!`), potvrdi.
4. U tom trećem tabu, uloguj se sa novim nalogom da pokažeš da radi
   kraj-do-kraja, pa ga zatvori - Tab A je i dalje prijavljen kao `admin`,
   nastavi tamo na sledeći korak.

**Ako nema pristupa mejlu uživo pred komisijom**: pripremi unapred (dan pre)
jedan test-nalog kreiran na stvarnu adresu kojoj imaš pristup na telefonu, pa
tokom demonstracije samo otvori taj već primljeni mejl - ne moraš kreirati
nalog uživo da bi pokazao aktivacioni mejl koji izgleda ispravno.

**Ako komisija pita o zaboravljenoj lozinci**: `/forgot-password` i
`/reset-password` postoje i radi identično (isti razlog za "vidljivo u
dev-u" ne postoji ovde - reset link stvarno ide samo na email, pa bi
uživo demo zahtevao ručan upit u bazu za `reset_key` kolonu). Bolje ostaviti
ovo kao usmeno objašnjenje nego uživo klikanje, osim ako je terminal sa
`psql` pripravan.

### 2. MANAGER administracija - radno vreme teretane [CORE] (~1 min)

Na istoj `/manager/administracija` stranici, pređi na tab "Radno vreme i
praznici". Reci: *"Ovo je upsert - ako menadžer greškom unese pogrešno radno
vreme za neki dan, drugi unos za isti dan ga prepisuje umesto da vrati
grešku, jer ranije nije postojao endpoint za izmenu."* Promeni radno vreme za
jedan dan (npr. nedelja) i sačuvaj - pokaži da se odmah ažuriralo u listi.

*(Opciono, ako ima vremena: dodaj i jedan praznik da pokažeš i tu formu.)*

### 3. Editor sala [CORE] (~1 min)

Otvori `/manager/room-editor`. Pokaži postojeće sale na 2D planu, prevuci
jednu salu (drag), pokaži da se pozicija menja uživo na canvas-u. Reci:
*"Menadžer ovde crta stvarni raspored teretane - pravougaonici sa rotacijom,
mapirano 1:1 na `react-konva` canvas biblioteku. Ovo se čuva u bazi čim
prevlačenje završi."* Ne moraš da praviš novu salu - samo pokaži da postojeća
reaguje na drag. Refresh stranice (F5) da pokažeš da je pozicija zaista
sačuvana, ne samo lokalna.

### 4. Live plan teretane - GLAVNI "wow" trenutak [CORE] (~2-3 min)

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

### 5. AI uvidi za menadžera [CORE] (~1-2 min)

Pređi na `/manager/insights`. Pokaži narativ generisan modelom
(`claude-haiku-4-5`) na osnovu stvarnih podataka o prijavama i uplatama.
Reci: *"Ovo nije šablon - model dobija agregirane brojeve (broj prijava po
sali, plaćeni termini po tipu sesije kao proksi za prihod, jer šema nema
cenu po terminu) i piše sažetak na srpskom."* Klikni "Regeneriši" da pokažeš
da forsira novi Claude API poziv (nekoliko sekundi čekanja je normalno i
očekivano - to je live poziv, ne trik).

### 6. Praćenje napretka klijenata [CORE] (~2 min)

Odjavi se, prijavi se kao `ogi` (TRAINER). Otvori `/trainer` - pokaži "Moji
klijenti" (uključuje `citva` i ostale klijente sa kojima `ogi` ima
zajednički termin iz seed podataka - reci da je ovo stvarna autorizaciona
provera, ne samo UI filter: *"Trener ne može ni preko API-ja da pročita
podatke klijenta kog nikad nije trenirao - vraća 403."*). Klikni na
`citva`, unesi novu meru (npr. težina) kroz formu, pokaži da se grafik
ažurira odmah bez reload-a. Unesi i lični rekord.

Odjavi se, prijavi se kao `citva` (CLIENT). Otvori `/client` i pokaži da
klijent vidi iste podatke koje je trener upravo uneo - read-only, bez formi.
Reci: *"Isti podaci, ali klijent nema mogućnost izmene - samo uvid."*

### 7. Trenerski samouslužni raspored [EKSTRA] (~1-1.5 min)

Odjavi se, prijavi se ponovo kao `ogi` (TRAINER) - u prethodnom koraku smo
završili prijavljeni kao `citva`. Otvori `/trainer/raspored` ("Moj raspored").
Reci: *"Ovo je Faza 6 - trener sam unosi svoje radno vreme, bez da menadžer
mora da uradi to za njega."* Unesi novu smenu za budući datum unutar radnog
vremena teretane. Pokaži da se odmah pojavljuje u listi.

Ako komisija pita "šta ako trener pokuša da izmeni raspored drugog trenera":
objasni da je to sprovedeno na nivou servisa (`403` ako pokuša da obriše
tuđi unos), i da self-service DTO-ovi (`CreateOwnTrainerScheduleRequest`) ne
sadrže `trainerId` polje uopšte - trener fizički ne može da pošalje tuđi ID,
jer ga tip zahteva ne dozvoljava.

*(Opciono: pokušaj unos van radnog vremena teretane ili van radnog vremena
koji se poklapa sa nedeljom bez definisanog radnog vremena - pokazaće jasnu
`400` grešku sa porukom, ne prazan `500`, zahvaljujući `GlobalExceptionHandler`
dodatom u Fazi 6.)*

### 8. Tok zakazivanja termina (booking flow) [EKSTRA] (~2-2.5 min)

Ovo demonstrira "marketplace" model dodat u Fazi 7 - menadžer kreira slotove,
klijenti ih rezervišu, treneri se sami dodeljuju.

1. Odjavi se, prijavi se kao `citva` (CLIENT). Otvori `/client/zakazivanje` ("Zakaži
   trening") - pokaži listu dostupnih termina sa slobodnim mestom. Rezerviši
   jedan budući termin.
2. Otvori `/client/moji-termini` ("Moji termini") - pokaži da se novi termin
   odmah pojavio u "budući" tabeli.
3. Odjavi se, prijavi se kao `ogi` (TRAINER). Otvori `/trainer/termini`
   ("Moji termini") - pokaži sekciju "termini bez trenera" i samododeli se
   na jedan od njih klikom na dugme. Reci: *"Trener se ovde sam prijavljuje
   na slobodan termin - ne čeka da mu menadžer dodeli."*
4. Vrati se na `citva` i otkaži termin rezervisan u koraku 1 (dugme
   "Otkaži" je dostupno samo za buduće termine). Reci: *"Otkazivanje je
   moguće samo do 24h pre termina - pokušaj otkazivanja termina koji počinje
   za manje od 24h vraća jasnu grešku, ne prazan 500."*

*(Ako ima još vremena: pokaži i da menadžer može ručno kreirati termin sa
već dodeljenim trenerom/klijentima preko Swagger-a, kao kontrast prema
samostalnom booking-u iznad.)*

### 9. Notifikacioni centar [EKSTRA] (~1 min)

Demonstrira push-obaveštenja koja stižu uživo kroz WebSocket, ne samo email -
najlakše se uklapa odmah posle koraka 8 (booking flow), pošto ta rezervacija
sama po sebi okida jedno obaveštenje.

1. Vrati se u Tab A (`admin`) - ne odjavljuj se. U zaglavlju bočne trake
   primeti zvonce ikonicu pored naziva aplikacije.
2. Ako je Tab A ostao otvoren tokom koraka 8 (klijent `citva` rezerviše
   termin), zvonce bi već trebalo da pokazuje broj nepročitanih - to je
   `POST /{id}/reserve` koji šalje "manager alert" na `/topic/manager`,
   primljen uživo, bez ikakvog refresh-a. Ako nije ostao otvoren, ponovi
   jednu rezervaciju kao `citva` u pozadini dok gledaš Tab A.
3. Klikni zvonce - otvara se padajuća lista sa vremenom prijema svakog
   obaveštenja (`HH:mm`). Reci: *"Padajuća lista se renderuje kroz portal u
   `document.body`, ne unutar bočne trake - ranija verzija je bila
   podsečena jer je uža bočna traka imala `overflow` koji je sekao širi
   panel."*
4. Otvori podnožje bočne trake i pokaži padajući meni za preferencu
   obaveštenja (EMAIL/PUSH/OBOJE) - svaka uloga bira sama za sebe, nezavisno
   od toga koju aktivnu ulogu trenutno koristi.

### 10. Praćenje uplata i duga [EKSTRA] (~1-1.5 min)

1. U Tab A (`admin`), otvori `/manager/placanja`. Izaberi klijenta iz
   pretrage (npr. `citva`) - pojavljuje se sažetak "Plaćeno X/Y
   individualnih, Z/W grupnih", i, ako klijent duguje, crveni okvir "Duguje N
   termina" po tipu sesije. Reci: *"Ovo poredi stvarno održane termine sa
   plaćenim terminima po tipu sesije - ne čita direktno iz internog brojača
   rezervacija, jer bi taj brojao i buduće, još neodržane termine kao da su
   'iskorišćeni'."*
2. Odjavi se, prijavi se kao `citva` (CLIENT), otvori `/client/uplate` ("Moje
   uplate") - pokaži da klijent vidi identičan sažetak za sebe, read-only.

### 11. Kalendarski UI kroz čitavu aplikaciju [EKSTRA] (~30s)

Ne mora biti poseban korak ako je vreme ograničeno - iskoristi ga kao
napomenu dok si već na `/client/zakazivanje` ili `/trainer/raspored` (koraci
7-8): *"Svaki ekran koji bira dan - zakazivanje, moji termini, raspored
trenera, dnevni raspored menadžera - koristi isti kalendarski
komponent-za-biranje-dana. Dani koji nisu dostupni (praznik teretane, dan bez
definisanog radnog vremena, ili - na trenerskim ekranima - trenerov slobodan
dan) su precrtani, ali i dalje klikabilni; klik prikazuje tačan razlog ispod
kalendara, umesto da korisnik sazna tek posle pokušaja zakazivanja."* Klikni
na jedan precrtan dan (ako postoji u vidljivom mesecu) da pokažeš baner sa
razlogom uživo.

### 12. Zaključak [CORE] (~30s)

*"Sistem sada pokriva ceo tok - od samostalne registracije, kroz
administraciju od strane menadžera, do sve tri 'wow' funkcionalnosti i
svakodnevnog korišćenja (zakazivanje, raspored, uplate) - sve integrisano sa
postojećim sistemom, bez narušavanja postojeće funkcionalnosti."*

## Plan B - ako nešto ne radi na dan odbrane

- **Nema interneta / Anthropic API ne radi**: AI uvidi (koraci 5 i deo
  koraka 6) su jedini delovi koji zahtevaju spoljnu mrežu. Ako
  `GET /api/insights/manager` ili `GET /api/progress/insight/...` vrate
  grešku, prijaviti to otvoreno ("ovo zove eksterni Claude API, koji zavisi
  od internet konekcije") i pokazati unapred snimljene screenshotove iz
  `docs/browser-qa/phase4-*.jpg` kao dokaz da funkcionalnost radi kada je
  mreža dostupna. **Zato uradi korak 5 iz "Priprema" unapred** - ako je
  odgovor već keširan (`MANAGER_INSIGHTS_CACHE` 30 min TTL,
  `CLIENT_PROGRESS_INSIGHT_CACHE` 10 min TTL), stranica će prikazati
  keširan sadržaj čak i ako je mreža u tom trenutku nedostupna za NOVI
  poziv - samo nemoj klikati "Regeneriši" ako nisi siguran da mreža radi.
- **WebSocket se ne poveže / veza padne uživo (korak 4)**: frontend sada
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
  profil učitava `V1.0017` i pokreće `DevDataSeeder`.
- **Registracija (korak 1) - mejl ne stiže ili nalog "već postoji"**: ako je
  email već iskorišćen na ovoj bazi (npr. ponovljena proba istog demo-a),
  koristi drugi email umesto da pokušavaš isti ponovo - `findOrCreateUser` će
  vratiti postojećeg korisnika bez novog `registrationKey`-a, pa novi mejl
  neće stići. Ako Gmail SMTP otkaže uživo (retko, ali moguće - vidi i "Nema
  interneta" ispod), prijavi to otvoreno i pređi na unapred pripremljeni
  primljeni mejl (vidi napomenu u koraku 1) umesto uživo slanja.
- **Booking flow (korak 8) - nema dostupnih termina bez trenera/sa slobodnim
  mestom**: seeder namerno pravi samo deo budućih termina bez trenera
  (~45%) i sa punim kapacitetom - ako baš svi vidljivi termini u trenutku
  demonstracije ispadnu popunjeni/dodeljeni (retko, ali moguće nakon više
  ranijih proba na istoj bazi), koristi Swagger da kreiraš novi termin
  (`POST /api/appointment`) bez trenera i klijenata, pa nastavi odatle.
- **Check-in vrati grešku umesto 409/201**: provjeri da li si stvarno prosledio
  ispravan `roomId`/`clientId` (pogledaj "Priprema", korak 4) - ID-jevi se
  menjaju ako je baza ranije korišćena za drugu demonstraciju.

## Tehnički podsetnik (za pitanja komisije)

Detaljno obrazloženje svake netrivijalne odluke (šema, servisni sloj,
frontend, i Faza 6/7 dopune) je u `AGENTS.md`, sekcije "Upgrade: schema
decisions", "Upgrade: service layer decisions", "Upgrade: frontend
decisions", "Upgrade: Faza 6 decisions" (uključujući nastavke), "Upgrade:
Faza 7 decisions", i finalni pregled u "Upgrade: final summary" - koristi ih
kao referencu ako komisija pita "zašto baš tako", npr.:

- zašto je soba pravougaonik a ne poligon,
- zašto occupancy ne dedupira dva signala,
- zašto je izabran `claude-haiku-4-5`,
- zašto self-service raspored DTO nema `trainerId` polje,
- zašto `GymScheduleServiceImpl.create` radi upsert a ne insert-only,
- zašto je dev-seed podataka Java `CommandLineRunner` a ne Flyway migracija,
- zašto rezervacija termina može da ode u negativan saldo preostalih termina
  (postojeća, nedirnuta ponašanja backend-a - vidi "Upgrade: Faza 7
  decisions"),
- koja poznata ograničenja postoje i zašto nisu popravljena u ovoj sesiji
  (sekcija "Known issues" u `AGENTS.md`).

Za noviju funkcionalnost dodatu posle Faze 7 (notifikacije, uplate/dug,
kalendarski UI, potvrda brisanja) pogledaj i `docs/decision-log.md` sekcije
"Upgrade: notification decisions", "Upgrade: notification-bell clipping
fix", "Upgrade: payment debt tracking decisions", "Upgrade: MonthCalendar
unavailability decisions", i "Upgrade: custom confirm-dialog decisions".
