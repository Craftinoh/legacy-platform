# LegacyAuth — manual test checklist (PandaSpigot 1.8.8)

Run with `LegacyAuth-5.6.0-legacy.jar` in `plugins/`, on Java 8 or Java 17, **without ProtocolLib**.
Nothing below has been verified on a live server with real clients; the automated suite covers the
logic, but the items marked *(needs a client)* require a real 1.7.10–1.8.9 client to confirm.

## A. Live language switch — the bug this release fixes

| # | Step | Expected |
|---|---|---|
| 1 | Set the player's language to Italian (network `/lang it`) | — |
| 2 | Join without an account | Registration prompt appears |
| 3 | Read the prompt | It is **Italian** |
| 4 | Run `/lang` and choose **English** | Language change acknowledged |
| 5 | Wait for the next periodic prompt (`settings.registration.messageInterval`) | — |
| 6 | Read it | It is **English**, with **no reconnect**, no `/authme reload`, no rejoin |
| 7 | Switch back to Italian | — |
| 8 | Wait for the next prompt | It is **Italian** again |

Repeat 1–8 for the **login** prompt after registering, and check the same live switching for:

- wrong password
- login timeout kick message
- captcha prompt (the captcha code must stay the same across the language change)
- session login message

## B. Core authentication (must be unchanged from AuthMe 5.6.0)

| # | Check | Expected |
|---|---|---|
| 9 | Complete a registration | Account created, success message in the player's language |
| 10 | Log out and reconnect | Login prompt shown |
| 11 | Log in with the correct password | Success |
| 12 | Log in with a wrong password | Wrong-password message, no lockout change |
| 13 | Let the login timeout expire | Kicked with the timeout message in the player's language |
| 14 | Reconnect within the session timeout | Session login message |
| 15 | Existing accounts from the old AuthMe database | Log in normally, no migration prompted |

## C. Provider integration

| # | Check | Expected |
|---|---|---|
| 16 | Start the server **without** LegacyLobby / `language-common` | LegacyAuth enables normally, no `NoClassDefFoundError` |
| 17 | With no provider, join | Messages use `apteris-language.fallback`, then `settings.messagesLanguage` |
| 18 | Enable LegacyLobby **after** LegacyAuth is already running | The next prompt uses the provider — no restart needed |
| 19 | Disable LegacyLobby while players are online | Messages fall back cleanly, no errors spamming the console |
| 20 | `/authme reload` | Config and languages reload, cached locales dropped |

## D. Compatibility

| # | Check | Expected |
|---|---|---|
| 21 | `/plugins` | Plugin is listed as **AuthMe**, console prefix is `[LegacyAuth]` |
| 22 | Nyx Ultimate and other AuthMe-aware plugins | Load and hook without `NoClassDefFoundError` / `NoSuchMethodError` |
| 23 | A plugin calling `AuthMeApi.getInstance()` | Works as with AuthMe 5.6.0 |
| 24 | Inventory during the login phase *(needs a client)* | Inventory is **not** hidden, blanked or restored — the feature was removed |
| 25 | Client 1.7.10 and 1.8.9 *(needs a client)* | Join, register and log in normally |

## E. Languages

| # | Check | Expected |
|---|---|---|
| 26 | First start | `plugins/AuthMe/messages/` contains all 31 shipped `messages_*.yml` |
| 27 | Edit one of them, then `/authme reload` | Your edit survives — files are never overwritten |
| 28 | Set `settings.messagesLanguage: it` and watch the console | Console/log lines are Italian, player messages stay per-player |
| 29 | A client with locale `pt_BR` *(needs a client)* | Brazilian Portuguese, distinct from `pt` |
| 30 | Delete a key from a language file, reload | That message falls back to the configured language, then English |

## Known limitation to verify with the network team

`language-common`'s `Language` enum currently defines only **ITALIAN** and **ENGLISH**. Until it is
extended, the network `PlayerLanguageProvider` can only ever answer `it` or `en`; the other 29
languages are reachable through the client locale and the configured fallback, but not through
`/lang`. LegacyAuth already handles all 31 — no change is needed here when the enum grows.
