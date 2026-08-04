# Architettura

```text
Client Minecraft
        |
        | locale del client
        v
NetworkLanguage su Velocity
        |
        | canale NetworkLang
        v
LegacyLobby su PandaSpigot
        |
        +-- Scoreboard renderer
        |       |
        |       +-- Placeholder interni
        |       +-- PlaceholderAPI
        |
        +-- Message renderer
        |       |
        |       +-- Placeholder interni
        |       +-- PlaceholderAPI
        |
        +-- Bossbar renderer
                |
                +-- Placeholder interni
                +-- PlaceholderAPI
                +-- PacketEvents legacy adapter
```

Velocity è la sorgente autorevole della preferenza. Alla connessione risolve il
locale o carica la scelta manuale persistita, quindi invia un messaggio binario
versionato al backend. La lobby conserva solo uno stato temporaneo in memoria e
ridisegna immediatamente scoreboard e bossbar personali.

## `/lang auto`

Il comando `/lang auto` imposta `LanguagePreference.AUTOMATIC` e usa il
`LocaleLanguageResolver` esistente per risolvere il locale corrente del client.
La sincronizzazione verso i backend avviene tramite il canale `NetworkLang`
esattamente come per i cambi manuali. Il listener `PlayerSettingsListener`
continua a funzionare per i cambi di locale successivi.

## PlaceholderAPI

L'integrazione PlaceholderAPI è isolata dietro l'interfaccia
`PlaceholderService`. L'implementazione reale (`PlaceholderApiService`) usa
`PlaceholderAPI.setPlaceholders()`; il fallback (`NoopPlaceholderService`)
restituisce il testo invariato. La risoluzione segue l'ordine:

1. Placeholder interni (`{player}`, `{server}`, `{online}`, ecc.)
2. PlaceholderAPI (`%player_name%`, `%luckperms_prefix%`, ecc.)
3. Colori legacy (`&` → `§`)
4. Limiti visuali (16 caratteri prefix/suffix per scoreboard)

PlaceholderAPI NON è incluso nello Shadow JAR. Deve essere installato
manualmente nella cartella `plugins/` della lobby.

## PacketEvents

La bossbar legacy usa pacchetti Wither fake inviati tramite PacketEvents.
L'adapter è isolato dietro l'interfaccia `BossBarPacketAdapter`.
L'implementazione `PacketEventsBossBarAdapter` invia i pacchetti di spawn,
metadata e destroy dell'entità Wither. In assenza di PacketEvents, viene usato
`NoopBossBarPacketAdapter` e la bossbar rimane disabilitata.

PacketEvents NON è incluso nello Shadow JAR. Deve essere installato
manualmente nella cartella `plugins/` della lobby.

## Configurazioni

| File | Descrizione |
|---|---|
| `config.yml` | Impostazioni generali, canale lingua, slot join |
| `scoreboard.yml` | Scoreboard con sezioni per lingua |
| `bossbar.yml` | Bossbar multiple, rotazione, tipi progresso |
| `messages_it.yml` | Messaggi giocatore in italiano |
| `messages_en.yml` | Messaggi giocatore in inglese |

Tutti i file di configurazione vengono estratti dal JAR alla prima esecuzione
e mai sovrascritti successivamente.
