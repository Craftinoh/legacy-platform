# Legacy Platform

Monorepo dei plugin e delle librerie che compongono **Legacy Network**.

Il progetto supporta due ambienti distinti:

- **Velocity** per identità, lingua, routing e funzionalità globali del network;
- **PandaSpigot/Spigot 1.8.8** per lobby, minigame e sistemi di gameplay backend.

La lingua del giocatore è gestita centralmente da **NetworkLanguage**. I plugin non devono creare sistemi di traduzione paralleli né includere una seconda copia delle API linguistiche nei propri JAR.
