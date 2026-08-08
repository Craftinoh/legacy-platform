# Configurazioni runtime locali

Questi file sono preparati per il runtime:

`C:\Users\Antonio\Documents\GitHub\apteris-server\legacy`

Dopo aver copiato il file nella cartella dati del plugin corrispondente,
compilare soltanto i campi `password: ""` con i valori del `.env` locale.

Non committare mai i file dopo aver inserito le password.

## Velocity

Copiare:

- `velocity-legacy/plugins/networklanguage/config.yml`
  in `servers/velocity-legacy/plugins/networklanguage/config.yml`;
- `velocity-legacy/plugins/legacy-chickenwars-proxy/config.yml`
  in `servers/velocity-legacy/plugins/legacy-chickenwars-proxy/config.yml`;
- `velocity-legacy/plugins/legacyreports/config.yml`
  in `servers/velocity-legacy/plugins/legacyreports/config.yml`;
- `velocity-legacy/plugins/legacyscreenshare/config.yml`
  in `servers/velocity-legacy/plugins/legacyscreenshare/config.yml`.

## ChickenWars classico

Per `chickenwars-classic-01` e `chickenwars-classic-02`, copiare i due file
della cartella server scelta dentro le rispettive cartelle dati dei plugin:

- `plugins/LanguageBackend/config.yml`;
- `plugins/LegacyChickenWars/config.yml`.

I due template hanno identificatori di istanza distinti e condividono
`legacy_chickenwars`. Entrambi rientrano in `chickenwars-lobby-01`.

## Password richieste

- NetworkLanguage: password dell'utente `legacy_language`;
- ChickenWars proxy e backend: password dell'utente `legacy_chickenwars`;
- Reports e Screenshare: password dell'utente `legacy_staff`.

LuckPerms non viene configurato da questi template e puo' restare su H2
durante la fase locale.
