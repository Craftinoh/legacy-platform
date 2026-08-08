# Legacy Platform

Monorepo dei plugin e delle librerie che compongono **Legacy Network**.

Il progetto supporta due ambienti distinti:

- **Velocity** per identità, lingua, routing e funzionalità globali del network;
- **PandaSpigot/Spigot 1.8.8** per lobby, minigame e sistemi di gameplay backend.

La lingua del giocatore è gestita centralmente da **NetworkLanguage**. I plugin non devono creare sistemi di traduzione paralleli né includere una seconda copia delle API linguistiche nei propri JAR.

---

## Architettura

```text
Minecraft client
      |
      v
Velocity
├── NetworkLanguage
├── LegacyChickenWarsProxy
├── LegacyReports
└── LegacyScreenshare
      |
      | plugin messaging / trasferimenti / database condiviso
      v
PandaSpigot 1.8.8 backend
├── LanguageBackend
├── LegacyLobby
├── LegacyRegions
├── LegacyItems
├── LegacyMenu
├── LegacyCombat
└── LegacyChickenWars
```

`LegacyAuth` è mantenuto nello stesso repository, ma resta un progetto Maven autonomo e non un subproject Gradle.

---

## Requisiti di sviluppo

- Windows, Linux o macOS;
- **JDK 26** installato per il toolchain di build;
- Gradle Wrapper incluso nel repository;
- Maven Wrapper incluso in `legacy-auth`;
- accesso ai repository Maven configurati nel progetto.

### Bytecode prodotto

| Ambiente | Release Java |
|---|---:|
| Velocity | Java 21 |
| PandaSpigot/Spigot 1.8.8 | Java 8 |
| Contratti condivisi con backend 1.8.8 | Java 8 |

L'uso del toolchain JDK 26 non cambia il runtime richiesto dai JAR: i task di compilazione impostano esplicitamente `--release`.

---

## Moduli

### Lingua

| Modulo | Tipo | Runtime | Descrizione |
|---|---|---|---|
| `language-common` | Libreria | Java 8 | Lingue, traduzioni, placeholder, protocolli e contratti condivisi. |
| `language-velocity` | Plugin | Velocity / Java 21 | Plugin `NetworkLanguage`, sorgente autorevole della lingua del giocatore. |
| `language-backend` | Plugin | PandaSpigot / Java 8 | Plugin `LanguageBackend`, espone le API linguistiche ai plugin backend. |

### Componenti condivisi ChickenWars

| Modulo | Tipo | Runtime | Descrizione |
|---|---|---|---|
| `chickenwars-common` | Libreria | Java 8 | Contratti e codec condivisi tra proxy e backend ChickenWars. |
| `chickenwars-velocity` | Plugin | Velocity / Java 21 | Routing, istanze, prenotazioni, trasferimenti e `/cw rejoin`. |
| `legacy-chickenwars` | Plugin | PandaSpigot / Java 8 | Minigame ChickenWars completo. |

### Plugin backend

| Modulo | Plugin | Descrizione |
|---|---|---|
| `legacy-lobby` | `LegacyLobby` | Lobby, scoreboard, bossbar, messaggi e navigazione backend. |
| `legacy-regions` | `LegacyRegions` | Regioni cuboidali, priorità, flag, protezioni e bypass. |
| `legacy-items` | `LegacyItems` | Custom item riutilizzabili e comandi amministrativi. |
| `legacy-menu` | `LegacyMenu` | Menu e GUI configurabili per i backend legacy. |
| `legacy-combat` | `LegacyCombat` | Regole e servizi di combattimento PvP. |

### Moderazione Velocity

| Modulo | Plugin | Descrizione |
|---|---|---|
| `legacy-reports-velocity` | `LegacyReports` | Report globali, workflow staff, audit e API per integrazioni. |
| `legacy-screenshare-velocity` | `LegacyScreenshare` | Sessioni di controllo, trasferimenti, timeout, recovery e audit. |

### Progetto autonomo

| Directory | Build | Descrizione |
|---|---|---|
| `legacy-auth` | Maven Wrapper | Fork/autenticazione legacy mantenuta fuori dalla build Gradle principale. |

---

## NetworkLanguage

### Responsabilità

`NetworkLanguage` su Velocity:

- rileva il locale del client;
- gestisce preferenza automatica o manuale;
- persiste la scelta del giocatore;
- espone il provider lingua agli altri plugin Velocity;
- sincronizza la lingua con i backend;
- applica la catena di fallback;
- supporta placeholder e traduzioni specifiche per plugin.

Il codice attuale di `language-common` definisce **31 lingue**. L'architettura è estendibile: aggiungere una lingua non deve richiedere un nuovo sistema di localizzazione dentro ogni plugin.

Una lingua riconosciuta non implica che ogni plugin possieda un bundle completo per quella lingua. Quando una chiave manca, viene usata la normale catena di fallback di NetworkLanguage.

### Regola dei classloader

Su Velocity, `language-common` deve essere caricato una sola volta dal JAR di `NetworkLanguage`.

I plugin Velocity consumatori devono usare:

```kotlin
compileOnly(project(":language-common"))
testImplementation(project(":language-common"))
```

Non devono includere classi sotto:

```text
it/legacynetwork/language/**
```

nei propri Shadow JAR. Una seconda copia verrebbe caricata da un classloader differente e renderebbe incompatibili interfacce e controlli `instanceof`.

Sul backend, la stessa responsabilità appartiene a `LanguageBackend`: i plugin PandaSpigot usano `language-common` come `compileOnly` e dichiarano `LanguageBackend` come dipendenza runtime.

### Regole per i messaggi

Ogni nuovo messaggio visibile deve:

- usare una chiave di traduzione;
- essere risolto nella lingua del destinatario;
- supportare placeholder centralizzati;
- usare il fallback configurato;
- evitare testo hardcoded nel codice Java;
- mantenere separati messaggi per player, staff e console quando necessario.

`LegacyCombat` è un modulo storico ancora da allineare completamente a questa regola.

---

## Dipendenze runtime

### Velocity

Ordine minimo consigliato:

```text
NetworkLanguage
├── LegacyChickenWarsProxy
├── LegacyReports
└── LegacyScreenshare
    └── dipende anche da LegacyReports
```

Dipendenze obbligatorie:

| Plugin | Dipendenze |
|---|---|
| `LegacyChickenWarsProxy` | `NetworkLanguage` |
| `LegacyReports` | `NetworkLanguage` |
| `LegacyScreenshare` | `NetworkLanguage`, `LegacyReports` |

### Backend PandaSpigot

`LanguageBackend` deve essere presente prima dei plugin che usano le API linguistiche.

Dipendenze e integrazioni principali:

| Plugin | Obbligatorie | Opzionali |
|---|---|---|
| `LegacyLobby` | `LanguageBackend` | PlaceholderAPI, PacketEvents, AuthMe |
| `LegacyRegions` | `LanguageBackend` | LegacyLobby, WorldEdit, FAWE |
| `LegacyItems` | `LanguageBackend` | PlaceholderAPI, LegacyLobby, LegacyMenu |
| `LegacyMenu` | `LanguageBackend` | PlaceholderAPI, LegacyLobby |
| `LegacyChickenWars` | `LanguageBackend` | LegacyLobby, LegacyRegions, PlaceholderAPI |
| `LegacyCombat` | — | integrazione lingua da completare |

---

## Funzionalità principali

### NetworkLanguage

- lingua automatica dal locale client;
- selezione manuale;
- fallback configurabile;
- persistenza SQLite o PostgreSQL;
- sincronizzazione proxy/backend;
- integrazione LuckPerms;
- tablist localizzata.

### LegacyLobby

- messaggi personali;
- scoreboard configurabile;
- bossbar legacy tramite PacketEvents;
- PlaceholderAPI opzionale;
- slot hotbar al join;
- reload sicuro delle configurazioni.

### LegacyRegions

- regioni cuboidali;
- priorità;
- flag granulari;
- protezione build, interazioni, PvP, danni, fame, drop, pickup ed eventi ambientali;
- bypass separati per permesso.

### LegacyChickenWars

- Gallina Reale;
- shop e quick buy;
- equipaggiamento;
- upgrade e trappole;
- generatori;
- progressione e statistiche;
- lifecycle completo della partita;
- routing distribuito;
- prenotazioni e heartbeat;
- rejoin tramite Velocity.

### LegacyReports

- `/report` per giocatori online;
- coda report globale;
- claim e workflow investigativo;
- stati append-only con audit;
- ricerca e paginazione;
- notifiche staff localizzate;
- API pubblica per Screenshare e integrazioni future;
- repository JDBC asincrono.

Stati principali:

```text
OPEN
CLAIMED
INVESTIGATING
SCREENSHARE
ACTION_TAKEN
DISMISSED
```

### LegacyScreenshare

- avvio e conclusione di sessioni staff;
- trasferimento di staff e target su server configurato;
- blocco dei cambi server durante il controllo;
- filtro dei soli comandi proxy;
- timeout centralizzati;
- gestione disconnect;
- recovery dopo riavvio;
- audit persistente;
- integrazione con LegacyReports;
- porta strutturata per una futura integrazione con LegacyPunishments.

Essendo un plugin esclusivamente Velocity, non controlla direttamente movimento, inventario, danno o interazioni sul backend.

---

## Comandi principali

| Plugin | Comandi |
|---|---|
| NetworkLanguage | `/networklang`, `/networklangreload` |
| LegacyLobby | `/legacylobby` |
| LegacyRegions | `/legacyregion` |
| LegacyItems | `/legacyitems` |
| LegacyMenu | `/legacymenu`, `/lang`, `/language` |
| LegacyCombat | `/legacycombat` |
| LegacyChickenWars | `/chickenwars`, `/cw` |
| LegacyReports | `/report`, `/reports` |
| LegacyScreenshare | `/ss`, `/screenshare` |

Sottocomandi, permessi e messaggi sono configurabili o documentati nei file del rispettivo modulo.

---

## Persistenza

I plugin che accedono al database devono rispettare queste regole:

- nessuna query JDBC sul thread eventi Velocity o sul main thread Bukkit;
- query parametrizzate;
- migrazioni versionate;
- timestamp UTC;
- update condizionali per le transizioni concorrenti;
- audit append-only dove richiesto;
- chiusura ordinata di executor e datasource allo shutdown.

Storage principali:

| Sistema | Storage |
|---|---|
| NetworkLanguage | SQLite o PostgreSQL |
| ChickenWars | PostgreSQL; implementazioni in-memory/test dove previste |
| LegacyReports | JDBC/PostgreSQL in produzione; repository in-memory per test |
| LegacyScreenshare | JDBC/PostgreSQL in produzione; repository in-memory per test |

---

## Build

### Windows PowerShell

Build completa dei plugin Gradle:

```powershell
.\gradlew.bat clean build
.\gradlew.bat buildPlugins
```

Build di LegacyAuth:

```powershell
.\gradlew.bat legacyAuthBuild
```

Test completi:

```powershell
.\gradlew.bat test --rerun-tasks
.\gradlew.bat legacyAuthTest
```

Build mirata dei plugin Velocity di moderazione:

```powershell
.\gradlew.bat `
  :language-common:build `
  :language-velocity:build `
  :legacy-reports-velocity:clean `
  :legacy-reports-velocity:build `
  :legacy-screenshare-velocity:clean `
  :legacy-screenshare-velocity:build `
  --stacktrace
```

Build mirata ChickenWars:

```powershell
.\gradlew.bat `
  :language-common:build `
  :chickenwars-common:build `
  :chickenwars-velocity:clean `
  :chickenwars-velocity:build `
  :legacy-chickenwars:clean `
  :legacy-chickenwars:build `
  --stacktrace
```

Non usare `-x test` per dichiarare una build verificata.

---

## JAR prodotti

Percorsi principali:

```text
language-velocity/build/libs/NetworkLanguage-0.1.0-SNAPSHOT.jar
language-backend/build/libs/LanguageBackend-0.1.0-SNAPSHOT.jar
legacy-lobby/build/libs/LegacyLobby-0.1.0-SNAPSHOT.jar
legacy-regions/build/libs/LegacyRegions-0.1.0-SNAPSHOT.jar
legacy-items/build/libs/LegacyItems-0.1.0-SNAPSHOT.jar
legacy-menu/build/libs/LegacyMenu-0.1.0-SNAPSHOT.jar
legacy-combat/build/libs/LegacyCombat-0.1.0-SNAPSHOT.jar
chickenwars-velocity/build/libs/LegacyChickenWarsProxy-0.1.0-SNAPSHOT.jar
legacy-chickenwars/build/libs/LegacyChickenWars-0.1.0-SNAPSHOT.jar
legacy-reports-velocity/build/libs/LegacyReports-0.1.0-SNAPSHOT.jar
legacy-screenshare-velocity/build/libs/LegacyScreenshare-0.1.0-SNAPSHOT.jar
```

I moduli `language-common` e `chickenwars-common` producono librerie e non vanno copiati direttamente nella cartella `plugins`.

---

## Installazione

### Proxy Velocity

Copiare nella cartella `plugins/` del proxy:

```text
NetworkLanguage-0.1.0-SNAPSHOT.jar
LegacyChickenWarsProxy-0.1.0-SNAPSHOT.jar
LegacyReports-0.1.0-SNAPSHOT.jar
LegacyScreenshare-0.1.0-SNAPSHOT.jar
```

Installare soltanto i plugin effettivamente utilizzati, rispettandone le dipendenze.

### Backend condivisi

Copiare `LanguageBackend` su ogni backend che ospita plugin localizzati:

```text
LanguageBackend-0.1.0-SNAPSHOT.jar
```

Aggiungere poi i plugin richiesti dal tipo di server.

Esempio lobby:

```text
LanguageBackend
LegacyLobby
LegacyRegions
LegacyItems
LegacyMenu
PlaceholderAPI      opzionale
PacketEvents        opzionale
```

Esempio ChickenWars:

```text
LanguageBackend
LegacyChickenWars
LegacyRegions       opzionale secondo configurazione
PlaceholderAPI      opzionale
```

---

## Convenzioni di sviluppo

### Velocity

- Java 21;
- `velocity-api` come `compileOnly`;
- accesso asincrono al database;
- `Player.createConnectionRequest(RegisteredServer)` per i trasferimenti;
- dipendenze plugin dichiarate con `@Dependency`;
- nessuna classe Bukkit nel JAR;
- `language-common` non shaded nei plugin consumatori.

### PandaSpigot 1.8.8

- bytecode Java 8;
- nessuna API moderna non disponibile su 1.8.8;
- dipendenza da `LanguageBackend` per i messaggi localizzati;
- PlaceholderAPI e PacketEvents come integrazioni opzionali quando previste;
- task centralizzati al posto di un task infinito per ogni entità o sessione.

### Git

- staging selettivo;
- non usare `git add .` o `git add -A` nei workflow automatizzati;
- non versionare output di build, file IDE o credenziali;
- eseguire `git diff --check` prima del commit;
- non effettuare push automatici dai task di sviluppo.

---

## Test e qualità

Ogni nuovo modulo deve includere test per:

- flussi riusciti;
- errori e timeout;
- concorrenza e idempotenza;
- shutdown e recovery;
- fallback lingua;
- placeholder;
- confini tra API pubbliche e implementazione;
- contenuto dei Shadow JAR;
- compatibilità del bytecode con il runtime dichiarato.

I JAR finali non devono contenere dipendenze di test come JUnit, Mockito o Byte Buddy.

I plugin Velocity consumatori di NetworkLanguage non devono contenere classi `it/legacynetwork/language/**`. LegacyScreenshare non deve includere l'implementazione di LegacyReports.

---

## Compatibilità

### Backend

- PandaSpigot 1.8.8;
- Spigot API 1.8.8;
- client legacy supportati tramite la configurazione del network;
- ViaVersion/ViaRewind dove installati dal network.

### Proxy

- Velocity;
- Java 21;
- API Velocity configurata dal progetto.

### Integrazioni opzionali

- PlaceholderAPI;
- PacketEvents;
- LuckPerms;
- WorldEdit/FastAsyncWorldEdit;
- AuthMe/LegacyAuth.

---

## Limitazioni note

- `LegacyScreenshare` è attualmente Velocity-only: il freeze di movimento e inventario richiederà un adapter backend dedicato.
- `LegacyPunishments` non è ancora presente: le violazioni Screenshare vengono emesse tramite una porta strutturata e non eseguono comandi di ban.
- `LegacyReports` segnala giocatori online; non esegue lookup Mojang remoto.
- snapshot chat, inventario, combat state e anticheat non vengono inventati quando manca un provider reale.
- `LegacyCombat` deve ancora essere allineato completamente a NetworkLanguage.
- il numero di lingue attualmente definite dall'enum condiviso è 31; nuove lingue possono essere aggiunte estendendo il sistema comune.

---

## Roadmap indicativa

Componenti previsti ma non ancora implementati:

- `LegacyPunishments`;
- `LegacyStaff`;
- `LegacyEconomy`;
- `LegacyStats`;
- `LegacyHolograms`;
- `LegacyNPCs`;
- `LegacyKitPvP` e sistemi collegati.

La roadmap non costituisce un contratto di compatibilità: prima di creare nuovi moduli bisogna riutilizzare i contratti e i servizi già presenti nel monorepo.

---

## Licenza e distribuzione

Verificare la licenza dei componenti derivati o integrati prima di redistribuire build pubbliche. In particolare, `legacy-auth` conserva la propria storia e struttura di progetto.

Non includere nei JAR finali dipendenze esterne che devono essere installate separatamente sul server, salvo quando il build le riloca esplicitamente per evitare conflitti.
