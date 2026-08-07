package it.legacynetwork.chickenwars.persistence;

/** Le modalita' tracked vengono rifiutate se il profilo non e' persistibile. */
public enum DatabaseOfflinePolicy { REJECT_TRACKED, UNTRACKED_DEGRADED }
