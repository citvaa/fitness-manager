# Fitness Manager - plan nadogradnje za diplomski rad

Ovaj dokument je radni plan za nadogradnju postojećeg backend-a (Spring Boot) frontend-om i skupom funkcionalnosti koje treba da budu vizuelno i tehnički upečatljive za odbranu diplomskog rada. Čuva se u repou da bi i Claude Code i budući ti imali stalnu referencu.

## 1. Trenutno stanje (rezultat istraživanja, jul 2026)

Backend: Spring Boot 3.4.5, PostgreSQL + Flyway migracije, Redis keširanje, JWT autentifikacija sa refresh tokenima, role MANAGER/TRAINER/CLIENT, Hibernate Envers audit log, WebSocket/STOMP za real-time notifikacije (bez SockJS, čist WebSocket), email notifikacije (aktivacija, reset lozinke, podsetnici pre treninga preko scheduled job-ova), Swagger/OpenAPI dokumentacija.

Domen: klijenti, treneri, sesije (individualne/grupne), termini, raspored teretane i trenera, praznici, plaćanja, praćenje sesija klijenata.

Frontend: ne postoji (prazan folder).

### Otkriveni problemi

- `application.yaml` sadrži pravu Gmail app lozinku i JWT secret u plain textu, oboje su u git istoriji - treba prebaciti u env varijable i rotirati kredencijale.
- `SecurityConfig` ima `.requestMatchers("/**").permitAll()` pre `.anyRequest().authenticated()`, što znači da Spring Security sloj praktično ništa ne blokira - sva autorizacija se radi ručno kroz `JwtInterceptor`/`RoleInterceptor`. Radi, ali nema safety net ako se neki path preskoči u interceptor konfiguraciji. Treba dokumentovati namerno ili preći na standardni Spring Security pristup.
- Nema CORS konfiguracije nigde - blokiraće pozive sa frontend origin-a čim krene development.
- Skoro da nema testova (jedan prazan test fajl), nema CI-ja, nema README-a.
- `docker-compose.yaml` ima zakomentarisan volume za Postgres podatke, a `Docker/postgres_data` folder postoji i sadrži podatke - nekonzistentno, treba razjasniti da li se koristi.
- **Radni folder trenutno nije na `main` grani, nego na `feature/notification`, koja je 21 commit ispred `main` i sadrži neumergovan rad** koji main uopšte nema: refresh token autentifikaciju, Hibernate Envers auditing, strukturisano logovanje, dev profil sa test podacima, calendar API, notifikacije (email + WebSocket), reorganizaciju paketa po strukturi. `main` je zastareo - poslednji commit mu je merge `feature/mail` grane. Ovo mora da se smerguje pre bilo kakvog novog rada, inače se gradi na grani, ne na main-u, i pravi se dodatna zbrka. (Radna kopija ima i razlike u svakom fajlu u `git diff`, ali su to isključivo razlike u kraju linije/CRLF-LF, ne stvarne izmene - verovatno artefakt okruženja u kom je folder otvoren, ne pravi rad koji treba sačuvati.)

## 2. Vizija nadogradnje

Aplikacija se ne gradi kao jedna deljena SaaS platforma za sve teretane - svaka teretana dobija svoju instalaciju/instancu koju koristi samo za sebe (ali proizvod se prodaje/postavlja više puta, jednom po teretani). Zbog toga model ne treba tenant izolaciju kroz celu bazu, samo čist `Gym` entitet (jedan red konfiguracije po instalaciji: naziv, adresa, brend/logo) i `Room` entitet vezan za njega - ovo ostavlja i čist put ka pravom multi-tenant modelu kasnije, uz minimalnu cenu sada.

### Tri stuba nadogradnje

**1. Live plan teretane (glavni "wow" element)**
Vlasnik teretane u ručnom 2D drag & drop editoru iscrta sobe (pravougaonici, naziv, kapacitet, tip - npr. sala za jogu, teretana sa spravama, sala za grupne treninge). Na frontu se ceo plan prikazuje iz ptičije perspektive i sobe "žive" u realnom vremenu: boja/animacija sobe se menja u zavisnosti od trenutne zauzetosti. Zauzetost se računa iz termina koji su trenutno u toku (Appointment vezan za Room), plus opciono ručni check-in koji trener/klijent može da označi da plan deluje potpuno živ. WebSocket infrastruktura za ovo već postoji, samo treba novi topic (npr. `/topic/gym/occupancy`).

**2. AI insights za menadžera**
Servis koji periodično ili na zahtev agregira istorijske i uživo podatke (zauzetost soba, posećenost, prihodi, otkazivanja) i poziva Claude API da generiše kratke tekstualne uvide i preporuke na dashboardu menadžera (npr. "Sala za jogu je iskorišćena 20% u poslednjih 7 dana - razmisli o promeni termina."). Pošto se poziva pravi LLM, treba keširati rezultate (npr. dnevno) da se ne pozivaju API pri svakom učitavanju stranice.

**3. Vizuelno praćenje napretka klijenata**
Trener dobija ekran gde vizuelno (grafici kroz vreme, radar dijagrami, poređenje pre/posle) prati napredak klijenta - težina, telesne mere, lični rekordi u vežbama, posećenost. LLM ovde generiše kratak narativni rezime napretka i preporuku za sledeći period, na osnovu unetih podataka - koristan alat za trenera, ne gimmick.

### Predloženi tech stack (frontend)

React + TypeScript (Vite), `react-konva` ili sličan canvas/SVG pristup za drag & drop editor sala, Recharts za grafike napretka, TanStack Query za fetch/cache, `@stomp/stompjs` za real-time konekciju (čist WebSocket, bez SockJS-a - poklapa se sa trenutnim backend podešavanjem).

## 3. Metodologija poređenja alata (Claude Code vs Codex)

Bitna ispravka u odnosu na raniju verziju ovog dokumenta: poređenje nije "model A vs model B" unutar istog alata, nego **dva različita coding agent alata - Claude Code i Codex CLI**. Ovo je važnije nego što zvuči, jer ta dva alata imaju različite konvencije za trajnu memoriju/instrukcije: Claude Code čita `CLAUDE.md`, a Codex čita `AGENTS.md` - ne postoji automatska međusobna kompatibilnost. Ako bi baseline imao samo `CLAUDE.md`, Codex bi krenuo bez konteksta koji Claude Code ima, i poređenje ne bi bilo pošteno. Zato:

- Kanonski fajl sa instrukcijama je `AGENTS.md` (to je i otvoreni standard koji čita više alata), a `CLAUDE.md` se pravi kao symlink na njega (`ln -s AGENTS.md CLAUDE.md`) ili sa `@AGENTS.md` import linijom na vrhu - u svakom slučaju, sadržaj mora biti identičan za oba alata.
- Baseline NE treba da sadrži Claude Code-specifične mehanizme (skillove, subagente, `.claude/` konfiguraciju) kao deo trajnog seta instrukcija za nadogradnju, jer Codex nema ekvivalent za njih - to bi neopravdano favorizovalo Claude Code u poređenju. Sve konvencije/odluke idu kao običan tekst u `AGENTS.md`.

Van toga, princip fiksnog baseline-a ostaje isti:

1. **Baseline se pravi jednom.** Merge zaostalih grana, higijena (bezbednost, CORS, README...) i `AGENTS.md`/`CLAUDE.md` se rade jednom - to je zajednička, alat-nezavisna priprema, ne deo onoga što se poredi. Kad je repo u stanju da sve što trenutno postoji radi ispravno, taj commit se tagira, npr. `git tag baseline-v1`.
2. **Svaki alat kreće od tog istog tag-a, na svojoj grani.** Npr. `git checkout -b upgrade/claude-code baseline-v1` i `git checkout -b upgrade/codex baseline-v1`. Obe grane kreću od identičnog koda i identičnog `AGENTS.md`.
3. **Svaki alat radi sopstveni arhitektonski plan i implementaciju.** Oba dobijaju isti brief (vizija iz sekcije 2 ovog dokumenta - tri stuba, tech constraints, model "jedna instanca po teretani"), ali svaki sam smišlja šemu baze, API, i piše frontend. Ovo je namerno - poredi se i kvalitet planiranja, ne samo pisanje koda, što je bogatije za diplomski.
4. **Obe grane se čuvaju nakon završetka**, bez mergovanja u main dok se ne odluči koja verzija (ili delovi obe) ide dalje - grane same po sebi postaju materijal za poređenje (git diff, broj commita, LOC, subjektivni kvalitet UX-a, da li je build/testovi prošao, koliko je bilo potrebno tvojih intervencija/ispravki).

Vredi unapred zapisati (za metodologiju u samom radu) kriterijume po kojima ćeš ocenjivati: da li plan/kod radi bez grešaka, poklapanje sa specifikacijom, kvalitet i doslednost koda, kvalitet UX-a wow-feature-a, broj iteracija/ispravki koje si morao da tražiš, vreme/broj poruka do završetka.

Prompt za sledeću fazu (identičan brief koji će ići i Claude Code-u i Codex-u) pravimo kad baseline bude tagovan - osloniće se na `AGENTS.md` napravljen u ovoj sesiji.

## 4. Fazni plan rada

- **Faza 0 - Baseline (ovaj prompt, spreman za pokretanje sada).** Merge zaostale grane, higijena (bezbednost, CORS, konfiguracija), CLAUDE.md, memorija/konvencije za buduće sesije, provera da app ispravno radi, tag `baseline-v1`. Bez arhitektonskog plana za nadogradnju - to ide u fazu 1, po modelu.
- **Grananje po alatu.** Od `baseline-v1` napraviš dve grane - npr. `upgrade/claude-code` (radi se u Claude Code-u) i `upgrade/codex` (radi se u Codex CLI-ju).
- **Faza 1 - Arhitektura + šema baze (po alatu).** I Claude Code i Codex, svaki na svojoj grani, sam dizajnira Gym, Room, Occupancy/CheckIn, ClientProgress, ClientPersonalRecord i piše migracije/entitete.
- **Faza 2 - Backend servisi i API (po alatu).** Endpoints za plan teretane, real-time occupancy preko WebSocket-a, AI insights servis (Claude API + keširanje), progress tracking API.
- **Faza 3 - Frontend scaffold + live plan teretane (po alatu).** React projekat, autentifikacija, role-based routing, editor sala, live vizuelizacija.
- **Faza 4 - Praćenje napretka + AI insights UI (po alatu).** Ekrani za trenera i klijenta, grafici, AI rezimei i preporuke.
- **Faza 5 - Polish, testovi, priprema za odbranu (po alatu, ili samo za granu koja se bira za finalnu odbranu).**

## 5. Prompt za Claude Code - Faza 0 (Baseline)

Kopiraj sledeći prompt i pokreni ga u Claude Code-u, iz root foldera projekta.

```
Radiš na "fitness-manager" projektu - Spring Boot backend za upravljanje teretanom (klijenti, treneri, termini, plaćanja, notifikacije), koji je osnova za moj diplomski rad. Frontend trenutno ne postoji. Cilj ove sesije NIJE da praviš nove funkcionalnosti niti arhitektonski plan za njih - cilj je da repo dođe u čisto, provereno funkcionalno stanje koje postaje fiksni baseline. Taj baseline će posle biti tagovan i od njega će kretati (na odvojenim granama) dve odvojene AI-upgrade sesije - jedna u Claude Code-u, jedna u Codex CLI-ju - koje se međusobno poređuju za diplomski rad. Zato je bitno da baseline bude potpuno stabilan, da ne sadrži ništa vezano za samu nadogradnju, i da bude jednako čitljiv i tebi (ovoj sesiji) i Codex-u kasnije - ne pravi ništa što bi radilo samo u Claude Code-u.

Radi u sledećem redosledu:

0. GIT GRANA - PRVO OVO, PRE SVEGA OSTALOG
Trenutno se najverovatnije nalaziš na grani `feature/notification`, koja je oko 21 commit ispred `main` i sadrži rad koji `main` uopšte nema (refresh token autentifikacija, Hibernate Envers auditing, strukturisano logovanje, dev profil sa test podacima, calendar API, notifikacije email+WebSocket, reorganizacija paketa). `main` je zastareo - poslednji commit mu je merge `feature/mail` grane.
Proveri `git branch -a -vv`, `git status` i `git log main..HEAD --oneline` da potvrdiš tačno stanje (moglo se promeniti od kad sam ovo pisao). Ako zaista postoji ovakva razlika: prvo proveri da li ima STVARNIH nesačuvanih izmena u radnoj kopiji (`git diff --stat` - ako su brojevi insertions/deletions skoro identični po fajlu, to je verovatno samo CRLF/LF razlika u kraju linije, ne pravi rad, i ne treba je čuvati). Zatim smerguj `feature/notification` u `main` (main je čist podskup, grane se ne razilaze - standardni merge ili fast-forward, po tvojoj proceni), i tek nakon toga napravi novu granu OD ažuriranog `main`-a za sav dalji rad iz ove sesije (higijena iz koraka 4). Ne nastavljaj rad na `feature/notification` kao da je main.
Ako situacija ne odgovara ovom opisu (npr. već je smergovano, ili je stanje drugačije), samo mi javi šta si zatekao i nastavi logično.

1. ISTRAŽIVANJE
Pročitaj ceo Backend/demo/src, sve Flyway migracije, application.yaml/application-dev.yaml, docker-compose.yaml, pom.xml. Napravi sebi kompletnu mentalnu mapu: domenski model (User/Trainer/Client/UserRole, Session/Appointment, GymSchedule/TrainerSchedule/Holiday, Payment/ClientSessionTracking), auth flow (JWT + refresh token, role MANAGER/TRAINER/CLIENT, JwtInterceptor + RoleInterceptor, SecurityConfig), notifikacije (email + WebSocket/STOMP + NotificationScheduler), audit (Hibernate Envers), keširanje (Redis).

2. AGENTS.md (+ CLAUDE.md kao symlink)
Napravi AGENTS.md u rootu repoa - ovo je kanonski fajl sa instrukcijama, jer njega čita Codex CLI (i drugi alati), a Claude Code ga ne čita nativno. Sadržaj: kratak opis projekta i svrhe (diplomski rad, baza za nadogradnju - i eksplicitno napomeni da će ovaj repo koristiti i Claude Code i Codex CLI, u odvojenim sesijama, i da AGENTS.md mora da bude jednako koristan za oba), tech stack, kako se pokreće lokalno (docker-compose za Postgres/Redis, mvnw, portovi), rezime domenskog modela, auth flow, konvencije koje primetiš u kodu (paketna struktura, DTO/Mapper/Service/Repository pattern, MapStruct, Lombok), poznati problemi (vidi korak 4 ispod), i eksplicitno pravilo na kraju fajla: "Ovaj fajl se ažurira u svakoj sesiji kada se otkrije nešto novo o arhitekturi, donese važna odluka, ili promeni konvencija - ne čekaj da te neko pita." Piši AGENTS.md na engleskom (standard za codebase), ali kad razgovaraš sa mnom u ovoj sesiji koristi srpski.
Zatim napravi CLAUDE.md kao symlink na AGENTS.md (`ln -s AGENTS.md CLAUDE.md`) da Claude Code u budućim sesijama automatski učita isti sadržaj. Ne pravi dva fajla sa duplim/različitim tekstom - jedan izvor istine.

3. KONVENCIJE ZA BUDUĆE SESIJE - ČUVAJ ALAT-NEZAVISNO
Sve arhitektonske odluke, stvari koje NE treba raditi (npr. "ne diraj postojeće Flyway migracije, samo dodaj nove"), i slične napomene idi direktno u AGENTS.md kao običan tekst, ne kao Claude Code-specifične skillove/subagente/`.claude/` konfiguraciju - Codex nema ekvivalent za njih, i takva podešavanja bi neopravdano pomogla samo Claude Code-u u kasnijem poređenju. Cilj je da Codex, kad kasnije krene sa AGENTS.md, dobije potpuno isti kontekst kao Claude Code.

4. HIGIJENA - POPRAVI OVO
- Izbaci pravu Gmail app lozinku i JWT secret iz application.yaml u environment varijable (sa .env.example fajlom kao dokumentacijom, i .gitignore pravilom za pravi .env). Napomeni mi da rotiram Gmail app lozinku jer je bila u git istoriji.
- Dodaj CORS konfiguraciju koja dozvoljava localhost frontend origin (npr. http://localhost:5173) za development, sa jasnim mestom gde se u produkciji menja na pravi domen.
- Pogledaj SecurityConfig (.requestMatchers("/**").permitAll() pre anyRequest().authenticated()) - ili preformuliši da Spring Security stvarno štiti rute (uz JwtInterceptor/RoleInterceptor kao dodatnu proveru), ili ako odlučiš da ostaviš kako jeste, jasno dokumentuj u AGENTS.md zašto i gde je stvarna zaštita implementirana.
- Razjasni docker-compose.yaml - Postgres volume je zakomentarisan a Docker/postgres_data folder postoji sa podacima. Odluči da li treba da se uključi persistencija i uskladi.
- Dodaj osnovni README.md (kako se pokreće projekat lokalno).
Radi ove izmene na novoj grani (npr. chore/repo-hygiene), sa jasnim commit porukama. Ne diraj postojeće migracije - ako nešto treba u bazi, dodaj novu migraciju.

5. PROVERI DA APLIKACIJA STVARNO RADI
Ovo je uslov da baseline bude validan. Podigni Postgres i Redis preko docker-compose (Docker/docker-compose.yaml), pokreni migracije, podigni Spring Boot aplikaciju (dev profil) i potvrdi da:
- aplikacija startuje bez grešaka i migracije prođu čisto na svežoj bazi,
- Swagger UI je dostupan,
- bar jedan kompletan flow radi end-to-end (npr. register/login -> poziv nekog zaštićenog endpointa sa JWT-om).
Ako nešto od ovoga ne radi, popravi pre nego što nastaviš - baseline mora da bude funkcionalan, ne samo da se kompajlira. Zapiši u README kako si to proverio (koje komande) da mogu i sâm da ponovim proveru.

6. TAG BASELINE-A
Kada su higijena i provera gotovi i smergovani u main: napravi tag `git tag baseline-v1` na tom commit-u (i push tag, ako je remote podešen). Ovaj tag je fiksna tačka od koje će kasnije kretati dve odvojene sesije - Claude Code i Codex CLI - za poređenje za diplomski. Zato ne sme da sadrži ništa vezano za samu nadogradnju (Gym/Room, AI insights, progress tracking) niti ništa specifično samo za Claude Code, samo čisto, ispravno postojeće stanje i AGENTS.md/CLAUDE.md koji su jednako korisni oba alata.

7. NA KRAJU
Napiši mi kratak rezime (u chatu, ne u fajlu): šta si promenio, šta si otkrio što nisam znao, kako si proverio da aplikacija radi, i da mi javiš tačan naziv/hash tagovanog commita. NE počinji da pišeš frontend, nove feature-e, ili bilo šta vezano za Gym/Room/AI insights/progress tracking u ovoj sesiji - to je namerno ostavljeno za sledeću fazu, po alatu.
```

## 6. Napomene

- Baseline je gotov: `main`, tag `baseline-v1`, `upgrade/claude-code` i `upgrade/codex` su svi na commit-u `7996057` (nakon što je CLAUDE.md symlink zamenjen sa `@AGENTS.md` import linijom, zbog Windows-a bez Developer Mode-a). Sve tri grane potvrđeno identične.
- Radni princip za sve dalje faze: **naizmenično po fazama, ne strogo sekvencijalno ni doslovno paralelno.** Za svaku fazu: prvo pokreneš prompt u Claude Code-u na `upgrade/claude-code`, ja pregledam rezultat, zatim pokreneš identičan prompt u Codex-u na `upgrade/codex`, ja pregledam i taj. Tek kad su oba pregledana za tu fazu, ide se na sledeću. Ovo ublažava rizik da te rešenje jednog alata nesvesno navede da drugi usmeriš ka istom rešenju.
- Nijedan alat ne treba da gleda ili pominje drugu `upgrade/*` granu - to je eksplicitno u promptu ispod.

## 7. Prompt - Faza 1 (Arhitektura + šema baze)

Ovaj prompt je **identičan za Claude Code i za Codex** - kopiraš isti tekst, samo ga pokrećeš na odgovarajućoj grani (`upgrade/claude-code` za Claude Code, `upgrade/codex` za Codex). Pre pokretanja, provери da je odgovarajuća grana checkout-ovana i da je čista (`git status`).

```
Radiš na "fitness-manager" projektu - Spring Boot backend za upravljanje teretanom, osnova za moj diplomski rad. Prvo pročitaj AGENTS.md u rootu repoa - tu su tech stack, domenski model, auth flow, konvencije i poznati problemi. Poštuj te konvencije u svemu što pišeš u ovoj sesiji.

Trenutno si na grani koja se zove upgrade/claude-code ili upgrade/codex (provери sa `git branch --show-current`) - to je namerno, jer se identičan rad radi paralelno na dve grane sa dva različita AI alata (ti si jedan od njih) i posle se poredi za diplomski rad. NE gledaj, ne pominji, i ne pokušavaj da pristupiš drugoj upgrade/* grani ili njenom sadržaju - tretiraj kao da ne postoji. Radi isključivo na grani na kojoj se trenutno nalaziš, nikad na main.

VIZIJA NADOGRADNJE (kontekst za sve buduće faze, ne implementiraj sve odjednom):
Aplikacija se ne gradi kao deljena SaaS platforma - svaka teretana dobija svoju instalaciju koju koristi samo za sebe (proizvod se prodaje/postavlja više puta, jednom po teretani). Zato ne treba tenant izolacija kroz celu bazu, samo Gym entitet (konfiguracija jedne instalacije) i Room entitet vezan za njega.

Tri funkcionalnosti koje se grade kroz sledeće faze:
1. Live plan teretane - vlasnik ručno u 2D editoru iscrta sobe (naziv, kapacitet, tip). Na frontu se plan prikazuje iz ptičije perspektive i sobe "žive" u realnom vremenu - boja/animacija se menja sa trenutnom zauzetošću, računatom iz termina koji su u toku, plus opciono ručni check-in.
2. AI insights za menadžera - servis koji agregira podatke (zauzetost, posećenost, prihodi) i poziva LLM (Claude API) da generiše tekstualne uvide/preporuke, sa keširanjem rezultata.
3. Vizuelno praćenje napretka klijenata - trener vizuelno prati napredak klijenta (težina, mere, lični rekordi, posećenost) kroz vreme, uz LLM-generisan narativni rezime i preporuku.

Tech stack za frontend (kad dođe faza za to): React + TypeScript (Vite), react-konva ili sličan canvas/SVG pristup za editor sala, Recharts za grafike, TanStack Query, @stomp/stompjs za WebSocket (čist WebSocket, bez SockJS-a - poklapa se sa backend-om).

ZADATAK ZA OVU FAZU (Faza 1) - SAMO OVO, NIŠTA VIŠE:
Dizajniraj i implementiraj isključivo sloj podataka za sve tri funkcionalnosti - Flyway migracije, JPA entiteti, repository-ji, DTO-i, MapStruct mapperi. NE pravi servise, kontrolere, WebSocket kod, poziv LLM-a, niti frontend - to su sledeće faze. Cilj je da na kraju ove faze šema baze postoji, migracije prolaze čisto, i entiteti/repozitorijumi se kompajliraju i mogu se koristiti - ali ništa još nije povezano u API.

Konkretno, dizajniraj i implementiraj:
- Gym - konfiguracija jedne instalacije (naziv, adresa, brend/logo - proceni šta još ima smisla). Realno će postojati samo jedan red po instalaciji, ali modeluj je kao pravu tabelu, ne singleton u kodu.
- Room - vezan za Gym; naziv, tip, kapacitet, i geometrija za 2D prikaz. Sam odluči da li su pravougaonici (x/y/width/height/rotation) ili poligoni bolji izbor za ovaj slučaj, i dokumentuj zašto u AGENTS.md.
- Veza Appointment-a sa Room-om (razmisli da li je obavezna ili opciona).
- Occupancy/check-in koncept - odluči da li ti je potreban eksplicitan entitet za check-in događaje (npr. da bi se posle mogla praviti i istorija, ne samo trenutno stanje) ili je dovoljno izvesti trenutnu zauzetost iz Appointment-a bez novog entiteta. Obrazloži odluku.
- ClientProgressEntry - datum, težina, telesne mere (odluči koje - npr. struk/grudi/butina, ili fleksibilnije polje), beleške.
- ClientPersonalRecord - vežba, vrednost, jedinica, datum. Odluči da li je "vežba" slobodan tekst ili referenca na neki katalog vežbi (ako pravish katalog, to je novi entitet - proceni da li se isplati za obim ovog projekta).

Pravila:
- Poštuj postojeće konvencije iz AGENTS.md (paketna struktura, BaseEntity, @Audited, MapStruct, DTO/mapper pattern).
- Ne diraj postojeće Flyway migracije - samo dodaj nove (V1.0011 i dalje, ili kako već numeracija ide na ovoj grani).
- Commituj sa jasnim porukama, na grani na kojoj se nalaziš. Ne pravi merge u main.
- Nakon što završiš: pokreni migracije na svežoj bazi (docker-compose) i potvrdi da prolaze čisto, i da app i dalje startuje bez grešaka.

Na kraju:
Dodaj sekciju u AGENTS.md (npr. "## Upgrade: schema decisions") gde dokumentuješ SVAKU dizajnersku odluku i zašto si je doneo (geometrija sobe, check-in pristup, telesne mere, vežbe kao tekst/katalog). Ovo je bitno - to je materijal za poređenje u diplomskom radu, ne samo interna napomena. Na kraju sesije, u chatu (ne u fajlu), napiši mi kratak rezime odluka i pitanja gde bi voleo moje mišljenje pre sledeće faze.
```

## 8. Status Faze 1 (završeno)

Oba alata su nezavisno stigla do skoro identičnog dizajna sloja podataka (Gym, Room, RoomCheckIn, ClientProgressEntry, ClientPersonalRecord) - zanimljiv podatak za diplomski sam po sebi. Nakon pregleda, jedno pitanje se pokazalo kao stvarni zahtev (ne implementacioni ukus) i postavljeno je objema stranama radi pariteta: da li ručni check-in dozvoljava samo jedan aktivan boravak klijenta ukupno (globalno, ne po sobi) - odgovor je da, i oba alata su to implementirala kao unique parcijalni indeks na nivou baze.

Finalno stanje grana nakon Faze 1:
- `upgrade/claude-code`: `49835a4`
- `upgrade/codex`: `e884a2d`
- `main`/`baseline-v1`: nepromenjeno, `7996057`

## 9. Pre Faze 2 - potreban ti je Anthropic API ključ

Faza 2 uključuje stvarnu integraciju sa Claude API-jem (AI insights za menadžera, AI rezime napretka klijenta). Da bi obe sesije mogle da provere da to stvarno radi (ne samo da se kompajlira), treba ti pravi Anthropic API ključ u `.env` (ne u `.env.example` - taj ostaje placeholder). Isti ključ se može koristiti za obe grane tokom testiranja - to je infrastruktura, ne deo onoga što se poredi, tako da ne narušava pravičnost poređenja. Trošak za testiranje tekstualnih insajta na malom skupu podataka je zanemarljiv, pogotovo sa jeftinijim modelom.

## 10. Prompt - Faza 2 (Backend servisi i API)

Identičan za Claude Code i Codex, isto kao Faza 1. Pokreni u novoj čistoj sesiji, na odgovarajućoj grani.

```
Radiš na "fitness-manager" projektu. Prvo pročitaj AGENTS.md u rootu repoa u celosti, posebno sekciju "Upgrade: schema decisions" - tu je sloj podataka iz prethodne faze (Gym, Room, RoomCheckIn, ClientProgressEntry, ClientPersonalRecord) koji sada povezuješ u API. Poštuj postojeće konvencije (controller -> service/service.impl -> repository, @RoleRequired, MapStruct DTO mapiranje, DTO/mapper struktura).

Trenutno si na grani upgrade/claude-code ili upgrade/codex (provери sa git branch --show-current) - ne gledaj, ne pominji drugu upgrade/* granu, radi isključivo na svojoj, nikad na main.

ZADATAK ZA OVU FAZU (Faza 2) - servisni sloj i API, i dalje bez frontenda:

1. Plan teretane (CRUD)
Endpoints za upravljanje Gym konfiguracijom i Room-ovima (kreiranje/izmena/brisanje sobe, geometrija, kapacitet, tip). Odluči koja rola upravlja time (verovatno MANAGER) i koja rola samo čita (verovatno svi autentifikovani), koristeći postojeći @RoleRequired mehanizam.

2. Check-in / occupancy + real-time preko WebSocket-a
Endpoints za ručni check-in/check-out klijenta u sobu (poštujući constraint iz prethodne faze - jedan aktivan check-in po klijentu globalno; API treba da vrati smislenu grešku ako se pokuša drugi). Endpoint koji vraća trenutnu zauzetost po sobi (kombinacija: check-in-ovi koji su trenutno aktivni + termini koji su u toku). Novi WebSocket topic (npr. /topic/gym/occupancy) na koji se šalje ažuriranje kad se zauzetost promeni - i na check-in/check-out event, i periodično (npr. jednom u minut, po uzoru na postojeći NotificationScheduler @Scheduled pattern) da bi se i promene zauzetosti iz termina (početak/kraj) odrazile na live prikaz bez eksplicitnog eventa.

3. AI insights za menadžera
Servis koji agregira istorijske i uživo podatke (zauzetost soba iz RoomCheckIn istorije, posećenost, prihodi iz Payment-a) i poziva Claude API (Anthropic) da generiše kratke tekstualne uvide/preporuke. Treba ti pravi API ključ - pretpostavi da je u ANTHROPIC_API_KEY environment varijabli (dodaj u .env.example kao placeholder, isto kao MAIL_*/JWT_SECRET). Preporučujem jeftiniji/brži model za ovu vrstu zadatka pošto se poziva relativno često - provери u trenutnoj Anthropic dokumentaciji koji je odgovarajući izbor, ne pretpostavljaj naziv modela iz svog treninga jer se ti nazivi menjaju. Keširaj rezultat (Redis, koji već postoji u projektu - ali razmisli da li ti treba drugačiji TTL/cache region od postojećeg globalnog RedisConfig-a, i dokumentuj odluku) da se API ne poziva pri svakom učitavanju stranice. Endpoint za MANAGER-a da dobije (i po potrebi forsira regenerisanje) insajte.

4. Praćenje napretka klijenata - API
CRUD endpoints za ClientProgressEntry i ClientPersonalRecord (trener unosi/pregleda za svoje klijente; klijent pregleda svoje). Endpoint koji poziva Claude API da generiše kratak narativni rezime napretka i preporuku iz tih podataka, sa keširanjem (odluči kad se cache invalidira - npr. kad se dodaje novi unos napretka).

Pravila:
- Ne diraj postojeće migracije. Ako treba nova kolona/tabela, nova migracija.
- Koristi postojeći WebSocket setup (config/web/WebSocketConfig), ne pravi paralelni mehanizam.
- Svaka netrivijalna odluka (ko sme šta, cache strategija, koji LLM model, format WebSocket poruke) ide u AGENTS.md pod "Upgrade: schema decisions" (ili novu podsekciju "Upgrade: service layer decisions"), sa obrazloženjem - materijal za poređenje u diplomskom, ne interna napomena.
- Commituj sa jasnim porukama na svojoj grani.

Verifikacija pre kraja:
- App startuje bez grešaka.
- Bar jedan kompletan flow radi end-to-end sa pravim pozivom: kreiraj sobu, uradi check-in, provери da se WebSocket poruka šalje (ili bar da endpoint za trenutnu zauzetost vraća tačan podatak), pozovi AI insights endpoint i potvrdi da stvarno vraća tekst generisan od Claude API-ja (ne mock).
- Ako ANTHROPIC_API_KEY nije podešen u okruženju gde radiš, javi mi to eksplicitno u rezimeu umesto da tiho mock-uješ ili preskočiš tu proveru.

Na kraju, u chatu (ne u fajlu): rezime šta je urađeno, koje odluke bi voleo da potvrdim, i da mi kažeš tačan commit hash.
```

## 11. Status Faze 2 (završeno)

Oba alata su implementirala servisni sloj i API, uz proveren pravi Claude API poziv na obe strane (Claude Code: `claude-haiku-4-5`; Codex: `claude-haiku-4-5-20251001` - oba važe, samo razlika u aliasu). Zanimljive razlike za poređenje: Codex je usput dodao globalni exception handler (postojeći poznati nedostatak iz baseline-a), Claude Code je za isti problem napravio samo lokalnu obradu greške i eksplicitno ostavio globalni handler netaknut kao van okvira zadatka. Codex je od početka ograničio pristup trenera na klijente koje je stvarno trenirao (izvedeno iz istorije termina); Claude Code to nije imao, pa je to poslato kao dopunski zahtev (stvaran, ne stilski) i sada je implementirano na obe grane.

Finalno stanje grana nakon Faze 2:
- `upgrade/claude-code`: `7476610`
- `upgrade/codex`: `a126bd0`
- `main`/`baseline-v1`: nepromenjeno, `7996057`

## 12. Prompt - Faza 3 (Frontend scaffold + live plan teretane)

Ovo je veća faza - frontend kreće od nule. Identičan prompt za oba alata, kao i pre. Pošto je ovo UI faza, ja ne mogu da vidim kako stvarno izgleda (nemam pristup browseru na tvom računaru) - nakon što alat završi, **ti sam otvori aplikaciju u browseru i pošalji mi par screenshot-ova** da mogu da procenim vizuelni kvalitet, ne samo kod.

```
Radiš na "fitness-manager" projektu. Prvo pročitaj AGENTS.md u rootu repoa u celosti - posebno "Upgrade: schema decisions" i "Upgrade: service layer decisions", tu je ceo API koji sada konzumiraš (auth, Gym/Room CRUD, occupancy + WebSocket, progress tracking - AI insights UI NIJE u ovoj fazi, to ide u sledeću).

Trenutno si na grani upgrade/claude-code ili upgrade/codex (provери sa git branch --show-current) - ne gledaj, ne pominji drugu upgrade/* granu, radi isključivo na svojoj, nikad na main.

KONTEKST: Frontend/ folder je praznо - kreneš od nule. Backend radi na portu 8088, CORS je već podešen da dozvoljava http://localhost:5173 (Vite default port) - ako biraš drugi port/framework, ažuriraj app.cors.allowed-origins u application.yaml (nova vrednost, ne diraj mehanizam) i napomeni to u AGENTS.md.

PRE FRONTENDA - popravi jedan poznati bug koji bi te frontend rad naterao da otkriješ na teži način: `POST /api/user/login-refresh` nije izuzet iz JwtInterceptor-a (vidi AGENTS.md "Known issues"), pa je efektivno neupotrebljiv nakon što access token istekne - a to je tačno kad frontend treba da ga pozove. Izuzmi tu putanju u WebConfig-u (isti exclude-list pattern kao za login/register), potvrdi da refresh radi i nakon isteka access tokena, i zapiši ovo kao rešeno u AGENTS.md ("Known issues").

ZADATAK ZA OVU FAZU:

1. Scaffold
Novi frontend projekat u Frontend/. Preporučujem React + TypeScript (Vite) zbog ekosistema canvas biblioteka za editor sala (react-konva ili slično) - ali ako imaš dobar razlog za drugačiji izbor, slobodno, samo obrazloži u AGENTS.md. Osnovna struktura, routing, HTTP klijent ka backend-u.

2. Auth
Login ekran (koristi postojeći /api/user/login). Čuvanje access/refresh tokena, automatski silent refresh pre isteka access tokena (15 min - vidi AGENTS.md za tačne vrednosti), logout, zaštićene rute (redirect na login ako nema valjanog tokena).

3. Role-based routing/shell
Nakon login-a, ruta zavisi od role iz JWT-a (MANAGER/TRAINER/CLIENT - jedan nalog može imati više rola, odluči kako to rešavaš - npr. prikaz svih dostupnih oblasti ili izbor aktivne role - i dokumentuj). Za TRAINER i CLIENT oblasti napravi samo minimalan placeholder ekran za sada (npr. "Praćenje napretka - stiže u sledećoj fazi") - njihov pravi sadržaj pravimo u Fazi 4. Sav trud u ovoj fazi ide u MANAGER oblast, tačke 4 i 5 ispod.

4. Editor sala (MANAGER)
2D drag & drop editor za crtanje/pomeranje/resize soba (koristi postojeći Room CRUD - geometrija x/y/width/height/rotation, naziv, tip, kapacitet). Vlasnik teretane treba da može da napravi ceo plan teretane kroz ovaj ekran, bez direktnog diranja baze.

5. Live plan teretane (MANAGER) - GLAVNI "wow" element za odbranu diplomskog
Prikaz plana teretane iz ptičije perspektive, gde se sobe vizuelno menjaju u realnom vremenu na osnovu zauzetosti (postojeći occupancy endpoint za početno stanje, pa pretplata na /topic/gym/occupancy za live ažuriranja). Ovo je ekran koji će profesorka i komisija stvarno gledati na odbrani - uloži stvaran dizajnerski trud (glatke animacije/tranzicije boje pri promeni zauzetosti, jasan prikaz broja ljudi/kapaciteta po sobi, moderan izgled), ne samo funkcionalan wireframe. Slobodan si u izboru stila/biblioteka za UI komponente (Tailwind, MUI, shadcn, po tvom izboru) - samo obrazloži izbor u AGENTS.md.

6. Dev-seed podaci (opciono, ali preporučeno)
Da bi live plan imao šta da prikaže bez ručnog unosa pri svakom pokretanju, razmisli da dodaš dev-only seed podatke (Gym + par Room-ova, po uzoru na postojeći db/dev-data/V1.0009 pattern) - samo za dev profil, ne u produkcionim migracijama.

Pravila:
- Ne diraj postojeće migracije.
- Svaka netrivijalna odluka (frontend stack, state management, kako rešavaš multi-role rutiranje, UI biblioteka) ide u AGENTS.md pod "Upgrade: frontend decisions", sa obrazloženjem.
- Commituj sa jasnim porukama na svojoj grani, po logičnim celinama (scaffold, auth, editor, live view) - ovo je veća faza, nema potrebe da bude jedan ogroman commit.

Verifikacija pre kraja:
- Backend i frontend zajedno pokrenuti, dev server radi bez grešaka u konzoli.
- Kompletan flow radi u browseru: login, kreiranje sobe kroz editor, promena zauzetosti se vidi na live prikazu (ručno uradi check-in preko Swagger-a ili curl-a dok gledaš frontend, da potvrdiš da WebSocket ažuriranje stvarno stiže).
- Pošto ja ne mogu da vidim UI, u rezimeu mi navedi tačne korake (i portove/URL-ove) kako ja da pokrenem i pogledam sam.

Na kraju, u chatu: rezime šta je urađeno, koje odluke bi voleo da potvrdim, tačan commit hash (ili hash-eve ako je više commit-a).
```

## 13. Status Faze 3 (završeno)

Oba alata su napravila frontend od nule sa auth, role-based rutiranjem, editorom sala i live prikazom. Vizuelno su otišli u različitim pravcima - Claude Code: tamna tema, "blueprint" izgled sa tačkastom mrežom, animacije/glow dodati na moj zahtev nakon review-a. Codex: svetla tema, dashboard-card stil, sopstveni browser QA (Playwright) koji je sam otkrio i popravio prave bagove (Konva rendering, preklapanje teksta, login sa ne-email korisničkim imenom) i sačuvao screenshot-ove u `docs/browser-qa/`. Oba stvarno renderuju sobe na tačnim sačuvanim koordinatama (potvrđeno u kodu, ne samo na oko) - provera na screenshot-u može da prevari, geometrija ume da izgleda urednije nego što se očekuje.

Finalno stanje grana nakon Faze 3:
- `upgrade/claude-code`: `7126495`
- `upgrade/codex`: `33a94db`
- `main`/`baseline-v1`: nepromenjeno, `7996057`

Dev kredencijali za testiranje (dev-only, nikad za produkciju):
- `upgrade/claude-code`: `admin`/`password123` (MANAGER), `ogi`/`password123` (TRAINER), `citva`/`password123` (CLIENT)
- `upgrade/codex`: `admin`/`admin` (MANAGER) - postojao je od originala, otkriven testiranjem, nije nova migracija

## 14. Prompt - Faza 4 (Praćenje napretka + AI insights UI)

Identičan za oba alata. Kao i za Fazu 3 - ovo je UI faza, ne mogu sam da vidim rezultat, **pošalji screenshot-ove kad završi** (menadžerski AI insights ekran, ekran trenera sa grafikom napretka klijenta, klijentski ekran).

```
Radiš na "fitness-manager" projektu. Prvo pročitaj AGENTS.md u celosti, posebno sve "Upgrade: ..." sekcije (schema decisions, service layer decisions, frontend decisions) - tu je sav postojeći API i frontend koji nastavljaš.

Trenutno si na grani upgrade/claude-code ili upgrade/codex (provери sa git branch --show-current) - ne gledaj, ne pominji drugu upgrade/* granu, radi isključivo na svojoj, nikad na main.

KONTEKST: U prethodnoj fazi si napravio auth, role-based shell, editor sala i live prikaz za MANAGER-a. TRAINER i CLIENT oblasti su i dalje samo placeholder ekrani ("stiže u sledećoj fazi") - sada ih puniš stvarnim sadržajem. MANAGER dobija još jedan novi ekran (AI insights), pošto to nije bilo u prethodnoj fazi.

ZADATAK ZA OVU FAZU:

1. AI insights (MANAGER) - novi ekran
Prikazuje tekst koji vraća postojeći AI insights endpoint (pronađi ga u svom postojećem kodu/AGENTS.md - ti si ga pravio u prethodnoj fazi). Dugme za ručno regenerisanje (force refresh, ako endpoint to podržava). Prikaži tekst čitljivo (formatiran, ne sirov JSON), sa vremenom kad je insight generisan ako to imaš u odgovoru. Ovo ne treba komplikovan dizajn, ali treba da izgleda smisleno uklopljeno sa ostatkom aplikacije, ne kao odvojena zakrpa.

2. Praćenje napretka klijenata (TRAINER) - zameni placeholder
Trener treba da vidi listu SVOJIH klijenata (onih koje je stvarno trenirao - imaš već ograničenje na backend-u za ovo iz prethodne faze). Ako trenutno ne postoji čist endpoint koji vraća "moje klijente" za ulogovanog trenera, dodaj mali novi endpoint za to (koristeći postojeću logiku vlasništva trener-klijent) - ovo je jedini deo ove faze gde je mala backend izmena opravdana, sve ostalo je frontend.
Za izabranog klijenta: forma za unos novog merenja (ClientProgressEntry) i ličnog rekorda (ClientPersonalRecord), grafik napretka kroz vreme (težina, telesne mere - biblioteka po tvom izboru, Recharts je razuman default), lista ličnih rekorda, i prikaz AI generisanog narativnog rezimea/preporuke za tog klijenta (postojeći endpoint iz prethodne faze) sa dugmetom za regenerisanje ako ima smisla.

3. Praćenje napretka (CLIENT) - zameni placeholder
Klijent vidi SVOJE podatke (read-only) - isti grafik/lista/AI rezime kao kod trenera, ali bez mogućnosti unosa (to radi trener). Koristi postojeće "/me" tipa endpoint-e iz prethodne faze.

Pravila:
- Ne diraj postojeće migracije (osim ako dodaješ mali endpoint iz tačke 2 - to ne zahteva novu migraciju, samo servis/kontroler).
- Držи se postojećeg vizuelnog stila iz prethodne faze (boje, komponente, layout obrasci) - ovo treba da izgleda kao deo iste aplikacije, ne novi dizajn.
- Svaka netrivijalna odluka ide u AGENTS.md pod "Upgrade: frontend decisions" (ili nova podsekcija), sa obrazloženjem.
- Commituj po logičnim celinama (AI insights ekran, trainer progress, client progress), ne kao jedan ogroman commit.

Verifikacija pre kraja:
- Ako imaš radan browser QA alat iz prethodne runde, iskoristi ga - otvori sva tri nova/izmenjena ekrana, uradi bar jedan pravi test (unesi merenje kao trener, pogledaj kao klijent, pozovi AI insights kao menadžer sa pravim API pozivom), slikaj.
- Ako nemaš browser QA, u rezimeu mi navedi tačne korake i URL-ove da ja sam otvorim i pogledam, i pošalji screenshot-ove kad ih uzmem - odnosno, ako TI možeš da ih uzmeš, uzmi ih i sačuvaj u repo (npr. docs/browser-qa/, po uzoru na prethodnu fazu) da ih odmah pregledam.
- Potvrdi da AI insights i AI rezime napretka rade sa pravim Claude API pozivom (ANTHROPIC_API_KEY je već u .env - izvezi ga u terminal pre pokretanja, isto kao ranije), ne mock.

Na kraju, u chatu: rezime šta je urađeno, koje odluke bi voleo da potvrdim, tačan commit hash (ili hash-eve).
```
