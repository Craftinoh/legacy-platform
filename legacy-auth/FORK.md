# Apteris AuthMe — fork notes

Unofficial fork of [AuthMe/AuthMeReloaded](https://github.com/AuthMe/AuthMeReloaded).

| | |
|---|---|
| Upstream tag | `5.6.0` |
| Upstream commit | `107be9ab083ab43b7ff533ad3155f7fe29fb3b0f` |
| Licence | GPL-3.0 (unchanged), all upstream notices kept |
| Purpose | Render every message in the recipient's own language (Italian / English) |

Bug reports for this build belong to the fork, not to the AuthMe team.

## What the fork adds

Upstream AuthMe loads exactly one message file — the language named by
`settings.messagesLanguage` — and renders every message from it, for every player. This fork
resolves the language **per recipient** while leaving the storage, security and login logic
untouched.

### Deliberately not touched

Hashing algorithms, password storage, database queries and schema, UUID handling, session
validation, the login state machine, pre-login protections, rate limiting, email tokens, premium
authentication, and the existing commands and permissions. No database migration is required, and
`AuthMeApi` keeps its signatures — the fork only *adds* method overloads.

## Configuration

All keys are optional. A `config.yml` written by upstream AuthMe keeps working unchanged: the
defaults below reproduce upstream behaviour where a language cannot be determined.

```yaml
settings:
    messagesLanguage: en      # upstream key, still used for console and as the base language
    perPlayerLocale: true     # false → behave exactly like upstream

apteris-language:
    enabled: true             # master switch
    use-network-provider: true
    use-client-locale: true
    fallback: en
```

## Language resolution

For a message with a known recipient, `LocaleResolver` tries, in order:

1. the network `PlayerLanguageProvider` service, when registered;
2. the client locale — read live for an online player, otherwise the last one detected for them;
3. `apteris-language.fallback`;
4. English.

Messages with no recipient (console, logs) always use `settings.messagesLanguage`.

If the resolved language has no message file, or the file lacks that specific key, the message
falls back per key to the configured language — an incomplete translation yields translated text
where it exists and configured-language text elsewhere, never an error placeholder.

## Integration with LegacyLobby

The bridge to the network language service is **optional and reflective**. AuthMe has no
compile-time or runtime dependency on LegacyLobby or on `language-common`, and starts normally
without them.

The service `Class` is taken from Bukkit's `ServicesManager` and matched by name
(`it.legacynetwork.language.PlayerLanguageProvider`) rather than being loaded from AuthMe's own
class loader. That detail is what makes it work: LegacyLobby shades `language-common` into its own
jar, so the registered interface belongs to LegacyLobby's plugin class loader. A second copy loaded
here would be a different `Class` object, and `ServicesManager.load()` keys services by `Class`
identity — it would silently never match.

Lookup is lazy on every request, so a service registered after AuthMe enables is picked up without
a restart. If the provider throws, the failure is logged once and resolution falls through to the
next source.

## Build

Unchanged from upstream — Maven, not Gradle:

```
mvn clean package
```

`target/AuthMe-5.6.0-legacy.jar` comes from the `shaded-jar-legacy` shade execution, which bundles
the MySQL/MariaDB JDBC drivers. Note that the `-legacy` classifier refers to those bundled drivers,
**not** to Minecraft 1.8.8 compatibility.

### One build-environment change

`pom.xml` gained the `repo.glaremasters.me` repository. Upstream 5.6.0 no longer builds as-is:
`repo.onarandombox.com` is offline and `repo.dmulloy2.net` now serves only ProtocolLib 5.3.0, so
`ProtocolLib:4.8.0` and `Multiverse-Core:4.3.1` cannot be resolved from the repositories upstream
declares. Both are `provided` scope, compile-time only — no shipped code or behaviour changes.
