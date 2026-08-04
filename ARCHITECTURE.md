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
        v
Scoreboard tradotta
```

Velocity è la sorgente autorevole della preferenza. Alla connessione risolve il
locale o carica la scelta manuale persistita, quindi invia un messaggio binario
versionato al backend. La lobby conserva solo uno stato temporaneo in memoria e
ridisegna immediatamente la scoreboard personale.
