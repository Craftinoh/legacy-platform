# Wiki dei permessi — Legacy Platform

Questa pagina raccoglie i nodi di permesso usati dai plugin presenti nel monorepo. È stata verificata contro il codice e le configurazioni di `main` l'8 agosto 2026.

> I valori configurabili di LegacyReports, LegacyScreenshare e ChickenWars Proxy possono essere rinominati nei rispettivi `config.yml`. In questa pagina sono riportati i valori distribuiti di default.

## Convenzioni

| Indicazione | Significato |
|---|---|
| `true` | Bukkit concede il permesso a tutti per impostazione predefinita. |
| `op` | Bukkit lo concede agli operatori per impostazione predefinita. |
| `false` | Bukkit non lo concede automaticamente. |
| `configurabile` | Il nodo è letto dal file di configurazione del plugin Velocity. |
| `solo codice` | Il nodo è verificato dal codice ma non è dichiarato in `plugin.yml`. Conviene assegnarlo esplicitamente nel permission manager. |
| `nessuno` | Il plugin non contiene nodi di permesso propri. |

Su Velocity non esiste la sezione Bukkit `permissions:`: i nodi vengono risolti dal provider dei permessi del proxy, per esempio LuckPerms. La console e i gruppi staff vanno quindi configurati nel provider effettivamente installato.

## Indice rapido

| Plugin/modulo | Ambiente | Permessi propri |
|---|---|---:|
| NetworkLanguage | Velocity | 1 |
| LanguageBackend | Bukkit | 0 |
| LegacyLobby | Bukkit | 3 |
| LegacyRegions | Bukkit | 17 |
| LegacyItems | Bukkit | 6 dichiarati |
| LegacyMenu | Bukkit | 1 |
| LegacyCombat | Bukkit | 1 |
| LegacyChickenWars | Bukkit | 4, incluso uno solo nel codice |
| LegacyChickenWarsProxy | Velocity | 1 configurabile |
| LegacyReports | Velocity | 7 configurabili |
| LegacyScreenshare | Velocity | 5 configurabili |
| LegacyAuth | Bukkit | catalogo dedicato completo |
| `language-common`, `chickenwars-common` | Librerie | 0 |

---

## NetworkLanguage

Il comando di selezione della lingua è pubblico. Il solo nodo amministrativo trovato è:

| Permesso | Accesso | Note |
|---|---|---|
| `networklang.admin` | Reload di NetworkLanguage (`/networklangreload`) | Verificato direttamente dal comando Velocity. |

La selezione della lingua (`/networklang` e relativi alias registrati) non richiede permessi.

---

## LanguageBackend

`LanguageBackend` è un servizio runtime per i plugin Bukkit e non registra comandi o permessi propri.

---

## LegacyLobby

Comando principale: `/legacylobby` — alias `/ll`, `/lglobby`.

| Permesso | Default | Comando/azione |
|---|---:|---|
| `legacylobby.admin.reload` | `op` | `/legacylobby reload` |
| `legacylobby.admin.slot` | `op` | Gestione e verifica degli slot hotbar tramite `/legacylobby slot ...` |
| `legacylobby.admin.bossbar` | `op` | Gestione e verifica della bossbar tramite `/legacylobby bossbar ...` |

Non è dichiarato un nodo wildcard `legacylobby.admin`.

---

## LegacyRegions

Comando principale: `/legacyregion` — alias `/lregion`.

### Amministrazione

| Permesso | Default | Accesso |
|---|---:|---|
| `legacyregions.admin` | `op` | Tutti i sottocomandi di gestione regioni; funziona anche come bypass generale delle protezioni. |

### Bypass

Il controllo runtime accetta, in ordine, `legacyregions.admin`, `legacyregions.bypass` oppure il bypass specifico della flag interessata.

| Permesso | Default | Protezione ignorata |
|---|---:|---|
| `legacyregions.bypass` | `false` | Tutte le protezioni delle regioni. |
| `legacyregions.bypass.build` | `false` | Flag generale di costruzione. |
| `legacyregions.bypass.block-break` | `false` | Rottura blocchi. |
| `legacyregions.bypass.block-place` | `false` | Posizionamento blocchi. |
| `legacyregions.bypass.pvp` | `false` | PvP. |
| `legacyregions.bypass.damage` | `false` | Danno generico. |
| `legacyregions.bypass.fall-damage` | `false` | Danno da caduta. |
| `legacyregions.bypass.hunger` | `false` | Fame. |
| `legacyregions.bypass.item-drop` | `false` | Lancio oggetti. |
| `legacyregions.bypass.item-pickup` | `false` | Raccolta oggetti. |
| `legacyregions.bypass.explosions` | `false` | Esplosioni. |
| `legacyregions.bypass.fire-spread` | `false` | Propagazione del fuoco. |
| `legacyregions.bypass.mob-spawn` | `false` | Spawn dei mob. |
| `legacyregions.bypass.projectiles` | `false` | Proiettili. |
| `legacyregions.bypass.vehicle-use` | `false` | Uso dei veicoli. |
| `legacyregions.bypass.interact` | `false` | Interazioni. |

Concedere `legacyregions.admin` a uno staffer significa anche escluderlo dalle protezioni delle regioni; per chi deve soltanto moderare è preferibile assegnare bypass mirati.

---

## LegacyItems

Comando principale: `/legacyitems` — alias `/litems`, `/li`.

| Permesso | Default | Comando/azione |
|---|---:|---|
| `legacyitems.admin` | `op` | Nodo dichiarato nel `plugin.yml`, ma il comando non lo usa come wildcard. |
| `legacyitems.admin.reload` | `op` | `/legacyitems reload` |
| `legacyitems.admin.list` | `op` | `/legacyitems list` |
| `legacyitems.admin.give` | `op` | `/legacyitems give` e `/legacyitems giveall` |
| `legacyitems.admin.remove` | `op` | `/legacyitems remove` e `/legacyitems removeall` |
| `legacyitems.admin.debug` | `op` | `/legacyitems debug` |

**Attenzione:** concedere soltanto `legacyitems.admin` non abilita automaticamente i cinque sottopermessi nel codice attuale. Assegnare i nodi specifici, oppure configurare esplicitamente una wildcard nel permission manager.

---

## LegacyMenu

| Permesso | Default | Accesso |
|---|---:|---|
| `legacymenu.admin` | `op` | Comando amministrativo `/legacymenu open|reload|list` e alias `/lmenu`, `/lm`. |

I comandi giocatore `/lang` e `/language` sono pubblici.

---

## LegacyCombat

| Permesso | Default | Accesso |
|---|---:|---|
| `legacycombat.admin` | `op` | Comando amministrativo `/legacycombat reload` e alias `/lcombat`. |

---

## LegacyChickenWars — backend Bukkit

Comando principale: `/chickenwars` — alias `/cw`, `/chicken`, `/chickens`.

| Permesso | Default | Accesso | Stato del controllo |
|---|---:|---|---|
| `chickenwars.command` | `true` | Nodo generale dichiarato per il comando principale. | Dichiarato nel `plugin.yml`; non è usato come controllo generale nel command handler. |
| `chickenwars.command.join` | `true` | Entrata e quick join nelle arene. | Verificato dal command handler. |
| `chickenwars.admin` | `op` | Sottocomandi amministrativi e funzioni staff del minigame. | Verificato dal command handler. |
| `chickenwars.quickbuy.unlimited` | `solo codice` | Rimuove il limite standard di 3 preset Quick Buy. | Non dichiarato nel `plugin.yml`; assegnarlo esplicitamente. |

### Nota su `chickenwars.command`

Il nodo è utile come convenzione per i gruppi, ma nel codice attuale non sostituisce `chickenwars.command.join` e non protegge da solo tutti i sottocomandi. Per limitare l'accesso alle arene bisogna agire sul nodo `.join`.

---

## LegacyChickenWarsProxy — Velocity

Il proxy espone soltanto il flusso `/cw rejoin`.

| Permesso predefinito | Tipo | Accesso |
|---|---|---|
| `chickenwars.command.rejoin` | `configurabile` | Ricerca della sessione riconnettibile e trasferimento verso l'istanza ChickenWars. |

Configurazione:

```yaml
chickenwars-rejoin:
  permission: chickenwars.command.rejoin
```

Il permesso del proxy e quello del backend sono valutati in ambienti diversi: va assegnato sul contesto Velocity del permission manager.

---

## LegacyReports — Velocity

Comandi principali: `/report` per i giocatori e `/reports` per lo staff.

Tutti i nodi seguenti sono configurabili sotto `reports.permissions`. `legacyreports.admin` funziona nel codice come override dei permessi staff.

| Permesso predefinito | Accesso |
|---|---|
| `legacyreports.command.report` | Creazione di un report con `/report`. |
| `legacyreports.staff.view` | Accesso base a `/reports`, elenco, informazioni e preferenza notifiche. È richiesto prima di qualsiasi altro sottocomando staff. |
| `legacyreports.staff.claim` | `/reports claim`, `/reports investigate` e `/reports release`. |
| `legacyreports.staff.resolve` | `/reports dismiss` e `/reports action`. |
| `legacyreports.staff.history` | Ricerca dello storico tramite `/reports player`. |
| `legacyreports.admin` | Override di tutti i permessi staff e rilascio amministrativo di report non propri. |
| `legacyreports.protected` | Impedisce che il soggetto venga segnalato quando `reports.protect-staff: true`. |

Configurazione distribuita:

```yaml
reports:
  permissions:
    report: legacyreports.command.report
    staff-view: legacyreports.staff.view
    staff-claim: legacyreports.staff.claim
    staff-resolve: legacyreports.staff.resolve
    staff-history: legacyreports.staff.history
    admin: legacyreports.admin
    protected: legacyreports.protected
```

**Attenzione a `legacyreports.protected`:** non è un generico permesso staff. Con la protezione attiva rende il giocatore non segnalabile; concederlo solo ai ruoli che devono realmente essere esclusi dai report.

---

## LegacyScreenshare — Velocity

Comando principale: `/ss` — alias `/screenshare`.

Tutti i nodi sono configurabili sotto `screenshare.permissions`. `legacyscreenshare.admin` funziona come override e permette di chiudere o annullare sessioni appartenenti ad altri staffer.

| Permesso predefinito | Accesso |
|---|---|
| `legacyscreenshare.staff.view` | Accesso base a `/ss`, `/ss status` e `/ss list`. È richiesto prima di ogni sottocomando. |
| `legacyscreenshare.staff.start` | `/ss start <player> [report-id]`. |
| `legacyscreenshare.staff.stop` | `/ss stop ...` e `/ss cancel ...`. |
| `legacyscreenshare.staff.note` | `/ss note <player> <nota>`. |
| `legacyscreenshare.admin` | Override di tutti i permessi e gestione di sessioni non proprie. |

Configurazione distribuita:

```yaml
screenshare:
  permissions:
    start: legacyscreenshare.staff.start
    stop: legacyscreenshare.staff.stop
    view: legacyscreenshare.staff.view
    note: legacyscreenshare.staff.note
    admin: legacyscreenshare.admin
```

`staff.start`, `staff.stop` o `staff.note` da soli non bastano: il command handler verifica prima anche `staff.view`, salvo il possesso di `admin`.

---

## LegacyAuth

LegacyAuth contiene già un catalogo dedicato, auto-generato e più dettagliato di quanto sarebbe prudente duplicare qui:

- [`legacy-auth/docs/permission_nodes.md`](legacy-auth/docs/permission_nodes.md) — elenco completo dei nodi;
- [`legacy-auth/docs/commands.md`](legacy-auth/docs/commands.md) — associazione tra comandi e permessi.

Famiglie principali:

| Famiglia | Scopo |
|---|---|
| `authme.admin.*` | Tutti i comandi amministrativi. |
| `authme.player.*` | Tutti i comandi utente non amministrativi. |
| `authme.player.email.*` | Gestione e recupero email. |
| `authme.debug.*` | Sezioni del comando di diagnostica. |
| `authme.allow*` | Eccezioni come chat prima del login e account multipli. |
| `authme.bypass*` | Bypass di AntiBot, paese, purge, force-survival e inoltro Bungee. |
| `authme.vip` | Accesso al server pieno secondo la logica AuthMe. |

Il file dedicato è generato dagli strumenti del fork: per modificare i nodi AuthMe bisogna aggiornare la sorgente/generatore del progetto, non questa wiki centrale.

---

## Librerie senza permessi

I moduli seguenti sono contratti/librerie e non plugin installabili direttamente:

- `language-common`;
- `chickenwars-common`.

Non registrano comandi né nodi di permesso.

---

## Ruoli consigliati

Questi bundle sono **raccomandazioni operative**, non gerarchie applicate automaticamente dal codice.

### Giocatore

```text
chickenwars.command
chickenwars.command.join
chickenwars.command.rejoin
```

La selezione lingua è già pubblica. Per LegacyAuth usare i nodi giocatore previsti dalla sua configurazione, normalmente tramite `authme.player.*` o assegnazioni più granulari.

### Helper / supporto

```text
legacyreports.staff.view
legacyreports.staff.history
legacyscreenshare.staff.view
```

### Moderatore

```text
legacyreports.staff.view
legacyreports.staff.claim
legacyreports.staff.resolve
legacyreports.staff.history
legacyscreenshare.staff.view
legacyscreenshare.staff.start
legacyscreenshare.staff.stop
legacyscreenshare.staff.note
```

Aggiungere `legacyreports.protected` soltanto quando il ruolo deve essere escluso dai report.

### Amministratore di rete

```text
networklang.admin
legacyreports.admin
legacyscreenshare.admin
```

I permessi amministrativi Bukkit vanno assegnati separatamente in base al server su cui il relativo plugin è installato.

---

## Esempi LuckPerms

Esempi generici; adattare il contesto server/proxy alla topologia della rete.

```text
/lp group moderator permission set legacyreports.staff.view true
/lp group moderator permission set legacyreports.staff.claim true
/lp group moderator permission set legacyreports.staff.resolve true
/lp group moderator permission set legacyreports.staff.history true

/lp group moderator permission set legacyscreenshare.staff.view true
/lp group moderator permission set legacyscreenshare.staff.start true
/lp group moderator permission set legacyscreenshare.staff.stop true
/lp group moderator permission set legacyscreenshare.staff.note true
```

Per il rejoin sul proxy:

```text
/lp group default permission set chickenwars.command.rejoin true server=velocity
```

Il nome del contesto (`server=velocity` nell'esempio) dipende dalla configurazione LuckPerms della rete.

---

## Regole di manutenzione

Quando viene aggiunto o modificato un permesso:

1. aggiornare il `plugin.yml` o il `config.yml` proprietario;
2. aggiungere o aggiornare il controllo nel codice;
3. aggiungere un test del ramo autorizzato e di quello negato;
4. aggiornare questa pagina nello stesso commit;
5. evitare nodi dichiarati ma non applicati e nodi applicati ma non dichiarati;
6. per LegacyAuth, rigenerare il catalogo dedicato anziché modificarlo a mano.
