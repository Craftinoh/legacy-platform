# legacy-platform

Primo sistema bilingue del network legacy. Il progetto produce un plugin per
Velocity (`NetworkLanguage`) e un plugin lobby per PandaSpigot 1.8.8
(`LegacyLobby`), entrambi basati sulla libreria interna `language-common`.

## Requisiti

- Windows, Linux o macOS
- JDK 26 (la build seleziona il toolchain 26 installato)
- accesso a Maven Central, PaperMC, SpigotMC, PlaceholderAPI e CodeMC

Il bytecode prodotto è Java 8 per `language-common` e `legacy-lobby`, Java 21
per `language-velocity`.

## Moduli

- `language-common`: lingue, traduzioni, placeholder e protocollo binario.
- `language-velocity`: comando `/lang`, rilevamento locale e persistenza.
- `legacy-lobby`: ricezione lingua, benvenuto, scoreboard, bossbar e messaggi.

## Repository e dipendenze

La build usa Maven Central, PaperMC, SpigotMC, PlaceholderAPI e CodeMC.
Gradle scarica API, JUnit e plugin nella cache standard
`%USERPROFILE%\.gradle\caches`; nessuna dipendenza viene salvata nel progetto.
Velocity, Spigot, PlaceholderAPI e PacketEvents restano dipendenze `compileOnly`.
`language-common` viene incorporato nei JAR finali tramite Shadow.

## Build

```powershell
.\gradlew.bat clean build
.\gradlew.bat buildPlugins
```

JAR prodotti:

- `language-velocity/build/libs/NetworkLanguage-0.1.0-SNAPSHOT.jar`
- `legacy-lobby/build/libs/LegacyLobby-0.1.0-SNAPSHOT.jar`

## Installazione

1. Copiare `NetworkLanguage-0.1.0-SNAPSHOT.jar` nella cartella `plugins` di
   Velocity e riavviare il proxy.
2. Copiare `LegacyLobby-0.1.0-SNAPSHOT.jar` nella cartella `plugins` della lobby
   PandaSpigot 1.8.8 e riavviare il server.
3. **PlaceholderAPI**: installare manualmente PlaceholderAPI (2.11+) nella lobby.
   LegacyLobby funziona anche senza, ma i placeholder PAPI non saranno risolti.
4. **PacketEvents**: installare manualmente PacketEvents (2.4+) nella lobby.
   LegacyLobby funziona anche senza, ma la bossbar sarà disabilitata.
5. Verificare che il forwarding dei plugin message tra proxy e lobby sia
   disponibile. Il canale usato è `NetworkLang`.

Le preferenze manuali sono conservate dal proxy in
`plugins/networklanguage/player-languages.properties`. Ogni UUID ha una lingua
e il tipo di preferenza; il salvataggio è asincrono e sopravvive ai riavvii.

## Comandi

### `/lang` (Velocity)

- `/lang` → mostra lingua attuale e lingue disponibili.
- `/lang it` → imposta italiano (manuale).
- `/lang en` → imposta inglese (manuale).
- `/lang auto` → attiva il rilevamento automatico della lingua del client.
- `/lang italiano`, `/lang inglese`, `/lang italian`, `/lang english` → alias.

Con la modalità manuale la lingua non cambia più in base al locale del client.
Con la modalità automatica il sistema segue il locale del client.

### `/legacylobby` (Lobby)

- `/legacylobby reload` → ricarica tutte le configurazioni (richiede `legacylobby.admin.reload`).
- `/legacylobby bossbar reload` → ricarica la configurazione bossbar (richiede `legacylobby.admin.bossbar`).
- `/legacylobby bossbar preview <id>` → mostra una bossbar specifica solo a chi esegue il comando.
- `/legacylobby bossbar stop` → termina la preview e ripristina la rotazione normale.

## Configurazione Scoreboard

File: `plugins/LegacyLobby/scoreboard.yml`

La scoreboard è completamente configurabile con sezioni per lingua:

```yaml
enabled: true
update:
  ticks: 20
placeholderapi:
  enabled: true
languages:
  it:
    title: "&6&lAPTERIS"
    lines:
      - "&7&m----------------"
      - "&fNome: &a%player_name%"
      - ...
  en:
    title: "&6&lAPTERIS"
    lines:
      - ...
```

- Massimo 15 righe.
- Supporto righe vuote.
- Placeholder interni: `{player}`, `{server}`, `{online}`, `{language}`, `{language_code}`, `{website}`.
- PlaceholderAPI: `%player_name%`, `%luckperms_prefix%`, ecc.
- Fallback inglese quando manca la sezione della lingua corrente.

## Messaggi Configurabili

File: `plugins/LegacyLobby/messages_it.yml` e `messages_en.yml`

```yaml
welcome:
  enabled: true
  text:
    - "&aBenvenuto su Apteris, &f%player_name%&a!"
```

Ogni categoria supporta `enabled: true/false` e `text` come stringa o lista.
I messaggi sono inviati al join (welcome) e supportano PlaceholderAPI.

## Bossbar Multiple

File: `plugins/LegacyLobby/bossbar.yml`

Supporta un numero arbitrario di bossbar con rotazione:

```yaml
enabled: true
update:
  ticks: 5
rotation:
  enabled: true
  mode: "SEQUENTIAL"  # oppure RANDOM
  interval-ticks: 100
bars:
  welcome:
    enabled: true
    priority: 10
    display-ticks: 100
    languages:
      it:
        text: "&6Benvenuto su &eApteris&6, &f%player_name%&6!"
      en:
        text: "&6Welcome to &eApteris&6, &f%player_name%&6!"
    progress:
      type: "COUNTDOWN"
      start: 1.0
      end: 0.0
      duration-ticks: 100
      fallback: 1.0
```

### Tipi di Progresso

- `STATIC` → valore fisso (es. 0.75).
- `COUNTDOWN` → da `start` a `end` in `duration-ticks`.
- `COUNTUP` → da `start` a `end` in `duration-ticks`.
- `PLACEHOLDER_RATIO` → calcola `current / maximum`, con supporto placeholder.

### Rotazione

- `SEQUENTIAL` → ordina per `priority`, poi ordine file.
- `RANDOM` → sceglie casualmente evitando la ripetizione immediata.

### PacketEvents

La bossbar usa pacchetti legacy Wither fake inviati tramite PacketEvents.
PacketEvents 2.4+ deve essere installato manualmente nella lobby.
Senza PacketEvents, la bossbar è disabilitata e viene mostrato un warning.

## Slot Hotbar al Join

Configurazione in `config.yml`:

```yaml
join:
  selected-slot:
    enabled: true
    slot: 1        # 1-9
    delay-ticks: 2
    force: true    # true = sempre; false = solo se non cambiato manualmente
```

## Placeholder Interni

| Placeholder | Descrizione |
|---|---|
| `{player}` | Nome del giocatore |
| `{server}` | ID del server |
| `{online}` | Giocatori online |
| `{language}` | Codice lingua del client |
| `{language_code}` | Codice lingua del client |
| `{website}` | Sito web configurato |

## Espansioni PlaceholderAPI

Quando PlaceholderAPI è installato, tutti i placeholder PAPI standard sono
disponibili in scoreboard, bossbar e messaggi:

- `%player_name%`
- `%server_online%`
- `%luckperms_prefix%`
- qualsiasi espansione installata

## Reload Sicuro

`/legacylobby reload` ricarica tutte le configurazioni senza perdere:
- lingua dei giocatori
- sessioni attive
- preferenze salvate

Se una configurazione non è valida, conserva l'ultima configurazione funzionante
e segnala l'errore in console.

## Compatibilità

- Minecraft 1.7.10, 1.8.9
- PandaSpigot 1.8.8
- Velocity
- ViaVersion + ViaRewind
- PlaceholderAPI 2.11+
- PacketEvents 2.4+

## Collaudo

- Entrare con client italiano e verificare italiano al primo accesso.
- Entrare con client non italiano e verificare il fallback inglese.
- Provare `/lang`, `/lang it`, `/lang en`, `/lang auto`.
- Verificare che `/lang auto` ripristini il rilevamento del locale client.
- Cambiare lingua mentre si è nella lobby: messaggio, scoreboard e bossbar
  devono aggiornarsi senza riconnessione.
- Verificare la rotazione delle bossbar con `/legacylobby bossbar preview`.
- Verificare lo slot hotbar al join.
- Riavviare il proxy e verificare che la scelta manuale sia conservata.
- Ripetere con client 1.7.10 e 1.8.9.
