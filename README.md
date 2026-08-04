# legacy-platform

Primo sistema bilingue del network legacy. Il progetto produce un plugin per
Velocity (`NetworkLanguage`) e un plugin lobby per PandaSpigot 1.8.8
(`LegacyLobby`), entrambi basati sulla libreria interna `language-common`.

## Requisiti

- Windows, Linux o macOS
- JDK 26 (la build seleziona il toolchain 26 installato)
- accesso a Maven Central, PaperMC e SpigotMC

Il bytecode prodotto è Java 8 per `language-common` e `legacy-lobby`, Java 21
per `language-velocity`.

## Moduli

- `language-common`: lingue, traduzioni, placeholder e protocollo binario.
- `language-velocity`: comando `/lang`, rilevamento locale e persistenza.
- `legacy-lobby`: ricezione lingua, benvenuto e scoreboard legacy.

## Repository e dipendenze

La build usa Maven Central, il repository pubblico PaperMC e il gruppo pubblico
SpigotMC. Gradle scarica API, JUnit e plugin nella cache standard
`%USERPROFILE%\.gradle\caches`; nessuna dipendenza viene salvata nel progetto.
Velocity e Spigot restano dipendenze `compileOnly`. `language-common` viene
incorporato nei JAR finali tramite Shadow.

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
3. Verificare che il forwarding dei plugin message tra proxy e lobby sia
   disponibile. Il canale usato è `NetworkLang`.

Le preferenze manuali sono conservate dal proxy in
`plugins/networklanguage/player-languages.properties`. Ogni UUID ha una lingua
e il tipo di preferenza; il salvataggio è asincrono e sopravvive ai riavvii.

## Collaudo

- Entrare con client italiano e verificare italiano al primo accesso.
- Entrare con client non italiano e verificare il fallback inglese.
- Provare `/lang`, `/lang it`, `/lang en`, `/lang italiano`, `/lang inglese`,
  `/lang italian` e `/lang english`.
- Cambiare lingua mentre si è nella lobby: messaggio e scoreboard devono
  aggiornarsi senza riconnessione.
- Riavviare il proxy e verificare che la scelta manuale sia conservata.
- Ripetere accesso, cambio lingua e scoreboard con client 1.7.10.
- Ripetere accesso, cambio lingua e scoreboard con client 1.8.9.
