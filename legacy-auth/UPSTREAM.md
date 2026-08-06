# LegacyAuth — upstream provenance

LegacyAuth is an unofficial fork of AuthMeReloaded. It is **not** endorsed by, or supported by, the
AuthMe team: do not report issues with this build to them.

| | |
|---|---|
| Upstream repository | https://github.com/AuthMe/AuthMeReloaded |
| Upstream tag | `5.6.0` |
| Upstream commit | `107be9ab083ab43b7ff533ad3155f7fe29fb3b0f` |
| Licence | **GPL-3.0** — unchanged; all upstream copyright and licence headers kept intact |
| Build | Maven (`./mvnw clean package`) — *not* Gradle |

## What LegacyAuth changes

### 1. Per-player message languages

Upstream renders every message from the single file named by `settings.messagesLanguage`.
LegacyAuth resolves the language **per recipient**, in this order:

1. the network `PlayerLanguageProvider` Bukkit service (LegacyLobby), when registered;
2. the client locale — read live for an online player, otherwise the last one detected;
3. `apteris-language.fallback`;
4. English.

Messages with no single recipient keep using `settings.messagesLanguage`. The bridge to the network
service is **reflective and optional**: LegacyAuth has no compile-time or runtime dependency on
LegacyLobby or `language-common`, and starts normally without them.

Deliberately global, because they have no single recipient:

- join/quit broadcast messages (one string sent to the whole server);
- `welcome.txt` (a single file, not a language file);
- whether a help *section* is enabled at all (a structural toggle; section contents are per-player).

### 2. Inventory hiding removed

The ProtocolLib-based inventory hiding during authentication was removed, together with its packet
listener, its restore path and the `ProtectInventoryBeforeLogIn` setting. The public event classes
`ProtectInventoryEvent` and `RestoreInventoryEvent` are **kept** so that plugins compiled against
them still link; they are simply never fired.

ProtocolLib itself is **still required** for the unrelated `DenyTabCompleteBeforeLogin` feature.

### 3. Branding

Build artifacts are branded LegacyAuth (`legacy-auth` artifactId, `LegacyAuth-<version>.jar`, and a
`[LegacyAuth]` console prefix). The **plugin identity is unchanged**: `plugin.yml` still declares
`name: AuthMe` with main class `fr.xephi.authme.AuthMe`, the `fr.xephi.authme` packages are not
renamed, and `AuthMeApi.getInstance()`, API v3, events, commands and permissions keep their
signatures. To an external plugin such as Nyx Ultimate, LegacyAuth behaves as AuthMe 5.6.0.

### 4. Build environment

`pom.xml` gained the `repo.glaremasters.me` repository. Upstream 5.6.0 no longer builds as-is:
`repo.onarandombox.com` is offline and `repo.dmulloy2.net` now serves only ProtocolLib 5.3.0, so
`ProtocolLib:4.8.0` and `Multiverse-Core:4.3.1` cannot be resolved from the repositories upstream
declares. Both are `provided`, compile-time only.

### Not touched

Hashing algorithms, password verification and storage, database schema and queries, UUID handling,
session validation, the login state machine, pre-login protections, rate limiting, email tokens,
premium authentication, and AuthMe API v3. No database migration is required.

## Comparing the fork with upstream

The monorepo copy has no Git history of its own. To diff against upstream, clone the tag and
compare trees:

```bash
git clone --branch 5.6.0 --depth 1 https://github.com/AuthMe/AuthMeReloaded.git /tmp/authme-upstream

# Full diff of the sources
diff -ru /tmp/authme-upstream/src legacy-auth/src

# Just the list of changed files
diff -rq /tmp/authme-upstream/src legacy-auth/src

# The build file
diff -u /tmp/authme-upstream/pom.xml legacy-auth/pom.xml
```

To review the fork as a patch instead, initialise a scratch repository, commit upstream at the tag,
then overlay the LegacyAuth tree and run `git diff`. Do not create a nested Git repository inside
the monorepo working tree.
