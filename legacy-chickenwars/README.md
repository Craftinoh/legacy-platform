# LegacyChickenWars

Minigame **Chicken Wars** per PandaSpigot/Spigot 1.8.8, bytecode Java 8.

Ogni squadra difende una **Gallina Reale**: un'entita' viva con punti vita,
scudo, rigenerazione e alimentazione. Finche' la gallina e' viva la squadra
rinasce; quando muore, ogni morte diventa definitiva.

## Stato di avanzamento

Questa versione implementa la **Fase 1** del documento di progettazione (core
giocabile) piu' gli elementi che danno identita' al minigame:

| Implementato | Non ancora implementato |
|---|---|
| Arene, stati, validazione | Livelli gallina e uova speciali |
| Editor guidato con strumenti e menu | NPC upgrade nell'editor |
| Gestione mondi integrata | Eliminazione cartelle mondo |
| Guida in gioco a sezioni | |
| Squadre, bilanciamento, colori | Abilita' e galline guardiane |
| Lobby, countdown, avvio, vittoria | Upgrade squadra e trappole |
| Gallina Reale: vita, scudo, rigenerazione | Eventi dinamici e overtime |
| Alimentazione, allarmi, morte, Ultima Piuma | Database, ranked, quest, cosmetici |
| Generatori con task centralizzato | Party, matchmaking, replay |
| Shop configurabile con NPC | Modalita' alternative (Egg Lives, King Chicken, ...) |
| Respawn, eliminazione finale, spettatori | |
| Scoreboard, messaggi bilingui | |
| Ripristino mappa incrementale | |
| API pubblica ed eventi Bukkit | |

Nessuna classe e' uno stub: quanto elencato a sinistra e' funzionante.

## Build

```powershell
.\gradlew.bat :legacy-chickenwars:build
```

JAR prodotto: `legacy-chickenwars/build/libs/LegacyChickenWars-0.1.0-SNAPSHOT.jar`

> Il documento di progettazione indicava Maven; il repository usa Gradle per
> tutti i moduli nativi, quindi e' stata seguita la convenzione del repository.

## Installazione

1. Copiare il JAR nella cartella `plugins/` del server 1.8.8.
2. Riavviare il server: vengono estratti `config.yml`, `chickens.yml`,
   `shop.yml`, `messages_it.yml`, `messages_en.yml` e la cartella `arenas/`.
3. Nessuna dipendenza e' obbligatoria. Se `language-common` e' presente sul
   network, la lingua del giocatore viene risolta tramite
   `PlayerLanguageProvider`; altrimenti si usa la lingua di fallback.

## Creazione di un'arena — editor guidato

Il modo consigliato: **un solo comando** fa tutto il resto.

```
/cw admin create farm_kingdom
```

Il comando, in sequenza:

1. crea l'arena `farm_kingdom`;
2. crea il mondo `cw_farm_kingdom` (vuoto, con una piattaforma allo spawn) —
   se la cartella esiste gia', la carica invece di ricrearla;
3. registra il mondo in `worlds.yml` con il suo tipo di generazione;
4. salva la tua posizione e il tuo inventario;
5. **ti teletrasporta dentro il mondo** e apre l'editor con gli strumenti.

Il tipo di generazione si puo' scegliere:

```
/cw admin create farm_kingdom void      # vuoto (predefinito), per schematic
/cw admin create farm_kingdom flat      # superpiatto, per prototipi
/cw admin create farm_kingdom normal    # generazione normale
```

### Usare una mappa gia' pronta

Se il mondo esiste gia' (importato a mano o caricato da Multiverse), **non
crearne uno nuovo**: adottalo.

```
/cw admin create farm_kingdom here      # usa il mondo in cui ti trovi
/cw admin create farm_kingdom cwmap     # usa il mondo "cwmap"
/cw admin setworld farm_kingdom cwmap   # cambia mondo a un'arena esistente
```

Con `here` non avviene nessun teletrasporto: resti dove sei e l'editor si apre
subito.

Un mondo adottato viene registrato come `EXISTING` e **non viene mai
rigenerato**: il plugin lo carica esattamente com'e', leggendo il suo
`level.dat`. Solo i mondi creati dal plugin ricevono un generatore.

> `setworld` su un'arena gia' configurata invalida le posizioni salvate, che
> contengono il nome del mondo precedente: il comando lo segnala e vanno rifatte.

### Creature preesistenti

Una mappa importata contiene spesso animali salvati nei chunk. Disattivare lo
spawn non li rimuove, quindi il plugin li elimina in tre momenti: quando adotta
un mondo, all'avvio del server e all'ingresso nell'editor.

Gli **armor stand non vengono mai toccati**: fanno parte delle decorazioni della
mappa. Il comportamento si disattiva con `world.clear-entities: false`.

Per rientrare in un'arena gia' creata: `/cw admin edit farm_kingdom`.

L'inventario personale viene salvato, si passa in creativa e la barra rapida
riceve gli strumenti. **Ogni azione usa la posizione in cui ti trovi.**

| Slot | Oggetto | Strumento | Azione |
|---|---|---|---|
| 1 | Bussola | Posizioni Arena | Menu: lobby, spettatori, pos1, pos2, limite costruzione |
| 2 | Lana | Squadre | Menu: crea, seleziona, elimina squadre |
| 3 | Letto | Spawn Squadra | Imposta lo spawn della squadra selezionata |
| 4 | Balla di fieno | Nido Reale | Imposta il nido della squadra selezionata |
| 5 | Uovo spawn | Gallina Reale | Imposta dove nasce la gallina |
| 6 | Smeraldo | Venditore | Imposta la posizione dell'NPC shop |
| 7 | Lingotto di ferro | Generatori | Menu: generatore centrale o di squadra |
| 8 | Libro | Valida Arena | Elenca cosa manca ancora |
| 9 | Blocco di smeraldo | Salva ed Esci | Salva e ripristina l'inventario |

Flusso tipico:

1. **Bussola** → imposta lobby, spettatori e i due angoli della regione.
2. **Lana** → *Aggiungi squadra*, scegli un colore. La squadra creata diventa
   automaticamente quella attiva (marcata con `>`).
3. Posizionati nella base e usa **letto**, **fieno**, **uovo** e **smeraldo**
   per spawn, nido, gallina e venditore.
4. **Lana** → seleziona la squadra successiva e ripeti il punto 3.
5. **Lingotto di ferro** → riga superiore per i generatori centrali, riga
   inferiore per quelli della squadra selezionata.
6. **Libro** → controlla che non manchi nulla.
7. **Blocco di smeraldo** → salva ed esci, poi `/cw admin enable farm_kingdom`.

Note:

- Gli strumenti non si possono piazzare, spostare o buttare via.
- Nel menu squadre: clic sinistro seleziona, clic destro elimina.
- Ogni modifica viene salvata subito su `arenas/farm_kingdom.yml`.
- `/cw admin exit` chiude l'editor salvando e ti riporta dove eri prima;
  anche una disconnessione salva l'arena, e l'inventario torna al rientro.
- L'NPC upgrade non e' nell'editor perche' gli upgrade sono di una fase
  successiva: resta disponibile con `/cw admin team <arena> setupgrade <id>`.

## Creazione di un'arena — da comandi

Equivalente all'editor, utile per scripting:

```
/cw admin create farm_kingdom
/cw admin setpos1 farm_kingdom          # primo angolo della regione
/cw admin setpos2 farm_kingdom          # secondo angolo
/cw admin setlobby farm_kingdom         # lobby pre-partita
/cw admin setspectator farm_kingdom     # spawn spettatori

/cw admin team farm_kingdom add red RED
/cw admin team farm_kingdom add blue BLUE

/cw admin team farm_kingdom setspawn red
/cw admin team farm_kingdom setnest red
/cw admin team farm_kingdom setchicken red
/cw admin team farm_kingdom setshop red
# ripetere per ogni squadra

/cw admin generator farm_kingdom add IRON red     # generatore di base
/cw admin generator farm_kingdom add DIAMOND      # generatore centrale

/cw admin validate farm_kingdom
/cw admin enable farm_kingdom
```

Tutte le posizioni vengono lette dal punto in cui si trova l'amministratore.
`validate` elenca cio' che manca; `enable` rifiuta le arene incomplete.

## Gestione mondi

Il plugin gestisce da solo i mondi delle arene, senza bisogno di Multiverse.

| Comando | Descrizione |
|---|---|
| `/cw admin world list` | Mondi gestiti, tipo di generazione, stato, giocatori |
| `/cw admin world load <mondo>` | Attiva un mondo presente su disco |
| `/cw admin world unload <mondo>` | Salva e disattiva il mondo |
| `/cw admin world tp <mondo>` | Teletrasporto allo spawn del mondo |

Dettagli:

- All'avvio del server i mondi richiesti dalle arene vengono **caricati
  automaticamente** (`world.auto-load` in `config.yml`).
- Il tipo di generazione di ogni mondo è registrato in `worlds.yml` e
  **riapplicato a ogni caricamento**: senza questo, un mondo `VOID` ricaricato
  tornerebbe a generare terreno normale attorno alla mappa.
- `/cw admin delete <arena>` cancella la configurazione e disattiva il mondo,
  ma **non elimina la cartella**: quella resta una scelta manuale.
- Ai mondi arena vengono applicate regole coerenti: niente ciclo giorno/notte,
  niente spawn di mob, niente fuoco né mob griefing, meteo fisso.

## Comandi

### Guida in gioco

`/cw help` apre una guida a sezioni; `/cw help <sezione>` entra nel dettaglio.

| Sezione | Contenuto |
|---|---|
| `/cw help` | Indice, cos'è il minigame, comandi essenziali |
| `/cw help gioco` | Tutti i comandi giocatore, spiegati |
| `/cw help gallina` | Meccaniche: scudo, vita, nutrizione, Ultima Piuma |
| `/cw help admin` | Gestione arene *(solo staff)* |
| `/cw help setup` | I 9 strumenti dell'editor e l'ordine consigliato *(solo staff)* |
| `/cw help mondi` | Comandi mondo e tipi di generazione *(solo staff)* |

Le sezioni riservate non compaiono né nell'indice né nel completamento
automatico per chi non ha `chickenwars.admin`.

### Giocatore

| Comando | Descrizione |
|---|---|
| `/cw help [sezione]` | Guida in gioco |
| `/cw join <arena>` | Entra in un'arena |
| `/cw quickjoin` | Entra nell'arena piu' vicina all'avvio |
| `/cw leave` | Abbandona la partita |
| `/cw team <squadra>` | Sceglie la squadra prima dell'avvio |
| `/cw shop` | Apre lo shop |
| `/cw list` | Elenco arene e stato |
| `/cw stats` | Statistiche della partita corrente |

Alias: `/chickenwars`, `/chicken`, `/chickens`.

### Amministratore

| Comando | Descrizione |
|---|---|
| `/cw admin help [sezione]` | Guida amministrativa |
| `/cw admin create <arena> [void\|flat\|normal]` | Crea arena e mondo, teletrasporta e apre l'editor |
| `/cw admin delete <arena>` | Elimina l'arena e disattiva il mondo |
| `/cw admin edit <arena>` | Apre l'editor guidato con gli strumenti |
| `/cw admin exit` | Chiude l'editor salvando |
| `/cw admin setworld <arena> [mondo]` | Collega l'arena a un mondo esistente |
| `/cw admin world <list\|load\|unload\|tp> [mondo]` | Gestione mondi |
| `/cw admin setlobby\|setspectator <arena>` | Posizioni generali |
| `/cw admin setpos1\|setpos2 <arena>` | Angoli della regione |
| `/cw admin setbuildlimit <arena> <y>` | Quota massima di costruzione |
| `/cw admin setminplayers <arena> <n>` | Giocatori minimi |
| `/cw admin team <arena> add <id> <colore>` | Crea una squadra |
| `/cw admin team <arena> remove <id>` | Rimuove una squadra |
| `/cw admin team <arena> setspawn\|setnest\|setchicken\|setshop\|setupgrade <id>` | Posizioni squadra |
| `/cw admin generator <arena> add <tipo> [team]` | Aggiunge un generatore |
| `/cw admin generator <arena> remove <id>` | Rimuove un generatore |
| `/cw admin generator <arena> setlevel <id> <livello>` | Livello generatore |
| `/cw admin validate\|save\|enable\|disable <arena>` | Gestione arena |
| `/cw admin start\|stop\|info\|tp <arena>` | Controllo partita |
| `/cw admin list` | Elenco arene con stato |
| `/cw admin reload` | Ricarica configurazioni e arene |

Colori squadra: `RED BLUE GREEN YELLOW AQUA WHITE PINK GRAY ORANGE PURPLE BLACK LIME`.
Risorse: `IRON GOLD DIAMOND EMERALD FEATHER`.

## Permessi

| Permesso | Default | Descrizione |
|---|---|---|
| `chickenwars.command` | `true` | Accesso al comando principale |
| `chickenwars.command.join` | `true` | Ingresso nelle arene |
| `chickenwars.admin` | `op` | Comandi amministrativi e bypass protezioni |

## Configurazione

| File | Contenuto |
|---|---|
| `config.yml` | Partita, respawn, combattimento, mondi, generatori |
| `worlds.yml` | Mondi gestiti e relativo tipo di generazione (automatico) |
| `chickens.yml` | Vita, scudo, rigenerazione, alimentazione, ologramma, morte |
| `shop.yml` | Categorie e articoli acquistabili |
| `messages_it.yml` / `messages_en.yml` | Tutti i testi |
| `arenas/<id>.yml` | Una arena per file, gestita dai comandi admin |

Nessun testo e' scritto nel codice Java: le traduzioni restano nei file lingua.

### Gallina Reale

```yaml
default:
  health: 100
  shield: 25
  health-regeneration:
    enabled: true
    delay-after-damage: 15   # secondi senza danni prima di rigenerare
    amount: 1.0              # vita recuperata al secondo
  feeding:
    enabled: true
    material: SEEDS          # clic destro sulla propria gallina
    heal-amount: 5.0
    shield-amount: 2.0
  last-feather:
    enabled: true
    duration-seconds: 10
    effects: ["SPEED:1", "INCREASE_DAMAGE:0"]
```

Lo scudo assorbe sempre il danno prima della vita. Vita e scudo rigenerano in
modo indipendente, ciascuno con il proprio ritardo dall'ultimo colpo subito.

## API

```java
ChickenWarsService service =
        Bukkit.getServicesManager().load(ChickenWarsService.class);
if (service != null && service.isPlaying(player)) {
    GameTeam team = service.getTeamOf(player);
}
```

Eventi Bukkit disponibili:

| Evento | Annullabile |
|---|---|
| `CWGameStartEvent` | no |
| `CWGameEndEvent` | no |
| `CWChickenDamageEvent` | **si** (anche `setDamage`) |
| `CWChickenDeathEvent` | no |
| `CWTeamEliminateEvent` | no |

## Scelte tecniche

- **Nessun NMS.** Gallina e venditori restano ancorati tramite teletrasporti
  correttivi; gli ologrammi sono armor stand marker.
- **Un solo task ripetitivo.** `GameLoopTask` fa avanzare tutte le arene, tutti
  i generatori e tutte le galline: il numero di task non cresce con le partite.
- **Morte senza schermata di respawn.** Il danno letale viene annullato e
  sostituito dalla logica di partita, evitando perdita di oggetti e desync.
- **Ripristino incrementale.** Vengono registrati solo i blocchi piazzati e lo
  stato di quelli distrutti, quindi il ripristino e' proporzionale all'attivita'
  della partita e non alla dimensione della mappa.
- **Generatore vuoto persistente.** Il tipo di generazione di ogni mondo e'
  salvato in `worlds.yml` e riapplicato al caricamento: e' l'unico modo perche'
  un mondo `VOID` resti vuoto dopo un riavvio. I mondi non registrati sono
  trattati come `EXISTING`, cosi' il plugin non impone mai una generazione a una
  mappa che non ha creato lui.
- **Logica testabile.** `ChickenVitals`, `TeamAssigner`, `ArenaDefinition`,
  `SimpleLocation` e `GeneratorSettings` non dipendono da un server attivo.
  66 test coprono anche la validita' dei file YAML inclusi e la parita' delle
  chiavi tra italiano e inglese, cosi' un refuso non arriva a runtime.

## Collaudo consigliato

- Creare un'arena a due squadre e verificare `validate` prima e dopo il setup.
- Entrare con due account, verificare countdown, assegnazione squadre e kit.
- Colpire la gallina avversaria: controllare scudo, avvisi e ologramma.
- Nutrire la propria gallina con i semi e verificare cura e cooldown.
- Uccidere la gallina: controllare animazione, Ultima Piuma e blocco respawn.
- Eliminare la squadra avversaria e verificare vittoria e ripristino mappa.
- Disconnettersi in partita e rientrare: l'inventario originale deve tornare.
