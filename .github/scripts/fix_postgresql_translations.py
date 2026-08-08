#!/usr/bin/env python3
"""Genera in modo idempotente fix JDBC, cataloghi ChickenWars e template runtime."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LANGUAGES = ['en', 'it', 'es', 'fr', 'de', 'pt', 'pt_br', 'nl', 'pl', 'ro', 'hu', 'cs', 'sk', 'sl', 'hr', 'bg', 'el', 'da', 'sv', 'no', 'fi', 'is', 'et', 'lv', 'lt', 'ga', 'mt', 'ru', 'uk', 'tr', 'sr']

NETWORKLANGUAGE_CONFIG = '# NetworkLanguage - storage condiviso tra proxy.\n# Inserire soltanto la password dell\'utente legacy_language.\n\nstorage:\n  type: postgresql\n\nsqlite:\n  file: languages.db\n  busy-timeout-ms: 5000\n  journal-mode: WAL\n  synchronous: NORMAL\n\npostgresql:\n  host: 127.0.0.1\n  port: 55432\n  database: legacy_language\n  username: legacy_language\n  password: ""\n  pool-size: 10\n  connection-timeout-ms: 5000\n  proxy-id: "velocity-legacy-01"\n  notification-channel: legacy_language_updates\n'
CHICKENWARS_PROXY_CONFIG = '# LegacyChickenWarsProxy - usa lo stesso database dei backend ChickenWars.\n# Inserire soltanto la password dell\'utente legacy_chickenwars.\n\nlanguage:\n  fallback: it\n\ndatabase:\n  jdbc-url: "jdbc:postgresql://127.0.0.1:55432/legacy_chickenwars"\n  username: "legacy_chickenwars"\n  password: ""\n  pool-size: 4\n\nchickenwars-rejoin:\n  enabled: true\n  permission: chickenwars.command.rejoin\n  lookup-timeout-millis: 3000\n  transfer-timeout-millis: 5000\n  heartbeat-timeout-millis: 15000\n  reservation-ttl-millis: 30000\n  reconnect-ttl-millis: 120000\n'
LANGUAGE_BACKEND_01 = '# LanguageBackend per chickenwars-classic-01.\n# Il canale reale e\' definito da LanguageProtocol ed e\' NetworkLang.\n\nenabled: true\nbackend-id: "chickenwars-classic-01"\nchannel: "NetworkLang"\nrequest-timeout-millis: 5000\nfallback-language: "it"\ndebug: false\n'
LANGUAGE_BACKEND_02 = '# LanguageBackend per chickenwars-classic-02.\n# Il canale reale e\' definito da LanguageProtocol ed e\' NetworkLang.\n\nenabled: true\nbackend-id: "chickenwars-classic-02"\nchannel: "NetworkLang"\nrequest-timeout-millis: 5000\nfallback-language: "it"\ndebug: false\n'
RUNTIME_README = '# Configurazioni runtime locali\n\nQuesti file sono preparati per il runtime:\n\n`C:\\Users\\Antonio\\Documents\\GitHub\\apteris-server\\legacy`\n\nDopo aver copiato il file nella cartella dati del plugin corrispondente,\ncompilare soltanto i campi `password: ""` con i valori del `.env` locale.\n\nNon committare mai i file dopo aver inserito le password.\n\n## Velocity\n\nCopiare:\n\n- `velocity-legacy/plugins/networklanguage/config.yml`\n  in `servers/velocity-legacy/plugins/networklanguage/config.yml`;\n- `velocity-legacy/plugins/legacy-chickenwars-proxy/config.yml`\n  in `servers/velocity-legacy/plugins/legacy-chickenwars-proxy/config.yml`;\n- `velocity-legacy/plugins/legacyreports/config.yml`\n  in `servers/velocity-legacy/plugins/legacyreports/config.yml`;\n- `velocity-legacy/plugins/legacyscreenshare/config.yml`\n  in `servers/velocity-legacy/plugins/legacyscreenshare/config.yml`.\n\n## ChickenWars classico\n\nPer `chickenwars-classic-01` e `chickenwars-classic-02`, copiare i due file\ndella cartella server scelta dentro le rispettive cartelle dati dei plugin:\n\n- `plugins/LanguageBackend/config.yml`;\n- `plugins/LegacyChickenWars/config.yml`.\n\nI due template hanno identificatori di istanza distinti e condividono\n`legacy_chickenwars`. Entrambi rientrano in `chickenwars-lobby-01`.\n\n## Password richieste\n\n- NetworkLanguage: password dell\'utente `legacy_language`;\n- ChickenWars proxy e backend: password dell\'utente `legacy_chickenwars`;\n- Reports e Screenshare: password dell\'utente `legacy_staff`.\n\nLuckPerms non viene configurato da questi template e puo\' restare su H2\ndurante la fase locale.\n'
TRANSLATION_STATUS = '# Stato traduzioni ChickenWars\n\nChickenWars supporta gli stessi 31 codici lingua di NetworkLanguage.\n\nI cataloghi `messages_it.yml` e `messages_en.yml` sono le versioni curate.\nI restanti cataloghi vengono inizializzati con il contenuto inglese completo,\ncosì ogni lingua dispone di tutte le chiavi e il plugin non mostra messaggi\nmancanti. Possono essere tradotti progressivamente senza cambiare codice.\n\nAll\'avvio `TranslationInstaller` copia i cataloghi mancanti nella cartella\n`plugins/LegacyChickenWars/translations` senza sovrascrivere i file già\npersonalizzati dall\'amministratore.\n'
TRANSLATION_TEST = '''package it.legacynetwork.chickenwars.message;

import it.legacynetwork.language.TranslationInstaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationResourcesTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void packagesAndInstallsEverySupportedLanguage() throws Exception {
        int installed = TranslationInstaller.install(
                temporaryDirectory.toFile(),
                "translations",
                Logger.getLogger("TranslationResourcesTest"),
                getClass().getClassLoader());

        assertEquals(TranslationInstaller.ALL_LANGUAGES.size(), installed);

        Path translations = temporaryDirectory.resolve("translations");
        for (String code : TranslationInstaller.ALL_LANGUAGES) {
            Path file = translations.resolve("messages_" + code + ".yml");
            assertTrue(Files.isRegularFile(file), "Risorsa mancante: " + code);

            String yaml = new String(
                    Files.readAllBytes(file), StandardCharsets.UTF_8);
            assertTrue(yaml.contains("prefix:"),
                    "Catalogo privo della chiave prefix: " + code);
            assertTrue(yaml.contains("command:"),
                    "Catalogo privo della sezione command: " + code);
        }

        int installedAgain = TranslationInstaller.install(
                temporaryDirectory.toFile(),
                "translations",
                Logger.getLogger("TranslationResourcesTest"),
                getClass().getClassLoader());
        assertEquals(0, installedAgain,
                "I file personalizzabili non devono essere sovrascritti");
    }
}
'''

POSTGRES_SERVICE = "it.legacynetwork.shadow.postgresql.Driver\n"
NETWORKLANGUAGE_SERVICE = (
    "it.legacynetwork.shadow.postgresql.Driver\n"
    "org.sqlite.JDBC\n"
)


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8").replace("\r\n", "\n")


def write(relative: str, content: str) -> None:
    path = ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    normalized = content.replace("\r\n", "\n")
    if path.exists() and path.read_text(encoding="utf-8") == normalized:
        return
    path.write_text(normalized, encoding="utf-8", newline="\n")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f"Pattern non trovato per {label}: {old!r}")
    return text.replace(old, new, 1)


def configure_reports() -> str:
    text = read("legacy-reports-velocity/src/main/resources/config.yml")
    text = replace_once(
        text, '  jdbc-url: ""',
        '  jdbc-url: "jdbc:postgresql://127.0.0.1:55432/legacy_staff"',
        "reports jdbc",
    )
    text = replace_once(
        text, '  username: ""', '  username: "legacy_staff"',
        "reports username",
    )
    text = replace_once(
        text, "  proxy-id: proxy", '  proxy-id: "velocity-legacy-01"',
        "reports proxy-id",
    )
    return text


def configure_screenshare() -> str:
    text = read("legacy-screenshare-velocity/src/main/resources/config.yml")
    text = replace_once(
        text, '  jdbc-url: ""',
        '  jdbc-url: "jdbc:postgresql://127.0.0.1:55432/legacy_staff"',
        "screenshare jdbc",
    )
    text = replace_once(
        text, '  username: ""', '  username: "legacy_staff"',
        "screenshare username",
    )
    text = replace_once(
        text, "  proxy-id: proxy", '  proxy-id: "velocity-legacy-01"',
        "screenshare proxy-id",
    )
    text = replace_once(
        text, "  server: screenshare-1", '  server: "screenshare-01"',
        "screenshare server",
    )
    text = replace_once(
        text,
        "  fallback-servers:\n    - lobby-1\n    - lobby-2",
        '  fallback-servers:\n    - "lobby-01"',
        "screenshare fallback",
    )
    return text


def patch_message_service() -> None:
    path = ROOT / (
        "legacy-chickenwars/src/main/java/it/legacynetwork/chickenwars/"
        "message/MessageService.java"
    )
    text = path.read_text(encoding="utf-8").replace("\r\n", "\n")

    import_line = "import it.legacynetwork.language.TranslationInstaller;\n"
    provider_import = "import it.legacynetwork.language.PlayerLanguageProvider;\n"
    if import_line not in text:
        text = replace_once(
            text, provider_import, provider_import + import_line,
            "MessageService import",
        )

    install_block = """        TranslationInstaller.install(
                plugin.getDataFolder(),
                TRANSLATIONS_DIR,
                plugin.getLogger(),
                plugin.getClass().getClassLoader());
"""
    constructor = """    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
"""
    if install_block not in text:
        text = replace_once(
            text, constructor, constructor + install_block,
            "MessageService installer",
        )
    path.write_text(text, encoding="utf-8", newline="\n")


def create_catalogs() -> None:
    resources = ROOT / "legacy-chickenwars/src/main/resources"
    english = (resources / "messages_en.yml").read_text(encoding="utf-8")
    italian = (resources / "messages_it.yml").read_text(encoding="utf-8")
    for code in LANGUAGES:
        write(
            f"legacy-chickenwars/src/main/resources/translations/messages_{code}.yml",
            italian if code == "it" else english,
        )


def backend_template(instance_id: str) -> str:
    source = read("legacy-chickenwars/src/main/resources/config.yml")
    marker = "rewards:\n"
    position = source.find(marker)
    if position < 0:
        raise SystemExit("Sezione rewards non trovata nel config ChickenWars")
    prefix = f"""# Config pronto per {instance_id}.
# Inserire soltanto la password dell'utente legacy_chickenwars.
# Non committare il file dopo aver aggiunto la password.

language:
  fallback: "it"

database:
  enabled: true
  jdbc-url: "jdbc:postgresql://127.0.0.1:55432/legacy_chickenwars"
  username: "legacy_chickenwars"
  password: ""
  maximum-pool-size: 4
  connection-timeout-millis: 5000
  profile-timeout-millis: 5000
  maximum-retries: 2

routing:
  instance-id: "{instance_id}"
  server-name: "{instance_id}"
  heartbeat-timeout-millis: 15000
  reservation-timeout-millis: 10000
  reconnect-timeout-millis: 120000
  lobby-server: "chickenwars-lobby-01"

"""
    return prefix + source[position:]


def main() -> None:
    reports = configure_reports()
    screenshare = configure_screenshare()

    write("language-velocity/src/main/resources/config.yml", NETWORKLANGUAGE_CONFIG)
    write("chickenwars-velocity/src/main/resources/config.yml", CHICKENWARS_PROXY_CONFIG)
    write("legacy-reports-velocity/src/main/resources/config.yml", reports)
    write("legacy-screenshare-velocity/src/main/resources/config.yml", screenshare)

    write(
        "language-velocity/src/main/resources/META-INF/services/java.sql.Driver",
        NETWORKLANGUAGE_SERVICE,
    )
    for module in (
        "chickenwars-velocity",
        "legacy-reports-velocity",
        "legacy-screenshare-velocity",
    ):
        write(
            f"{module}/src/main/resources/META-INF/services/java.sql.Driver",
            POSTGRES_SERVICE,
        )

    patch_message_service()
    create_catalogs()
    write(
        "legacy-chickenwars/src/test/java/it/legacynetwork/chickenwars/"
        "message/TranslationResourcesTest.java",
        TRANSLATION_TEST,
    )
    write("legacy-chickenwars/TRANSLATIONS.md", TRANSLATION_STATUS)

    write("runtime-configs/README.md", RUNTIME_README)
    write(
        "runtime-configs/velocity-legacy/plugins/networklanguage/config.yml",
        NETWORKLANGUAGE_CONFIG,
    )
    write(
        "runtime-configs/velocity-legacy/plugins/legacy-chickenwars-proxy/config.yml",
        CHICKENWARS_PROXY_CONFIG,
    )
    write(
        "runtime-configs/velocity-legacy/plugins/legacyreports/config.yml",
        reports,
    )
    write(
        "runtime-configs/velocity-legacy/plugins/legacyscreenshare/config.yml",
        screenshare,
    )
    write(
        "runtime-configs/chickenwars-classic-01/plugins/LanguageBackend/config.yml",
        LANGUAGE_BACKEND_01,
    )
    write(
        "runtime-configs/chickenwars-classic-01/plugins/LegacyChickenWars/config.yml",
        backend_template("chickenwars-classic-01"),
    )
    write(
        "runtime-configs/chickenwars-classic-02/plugins/LanguageBackend/config.yml",
        LANGUAGE_BACKEND_02,
    )
    write(
        "runtime-configs/chickenwars-classic-02/plugins/LegacyChickenWars/config.yml",
        backend_template("chickenwars-classic-02"),
    )


if __name__ == "__main__":
    main()
