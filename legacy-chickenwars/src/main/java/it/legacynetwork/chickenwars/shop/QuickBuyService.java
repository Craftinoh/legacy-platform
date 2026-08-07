package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.persistence.QuickBuyPresetRecord;
import it.legacynetwork.chickenwars.persistence.QuickBuyRepository;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modello runtime dei preset Quick Buy.
 *
 * <p>Legge e scrive tramite {@link QuickBuyRepository}: la sostituzione della
 * memoria volatile con un database non richiedera' modifiche a menu e
 * servizi.</p>
 *
 * <p>La disposizione degli slot e' condivisa tra SOLO, DOUBLES e TRIO; solo i
 * prezzi cambiano, e vengono risolti dal profilo economico della partita.</p>
 */
public final class QuickBuyService {

    /** Preset creato automaticamente al primo accesso. */
    public static final String DEFAULT_PRESET = "default";

    /** Permesso che rimuove il limite di preset. */
    public static final String UNLIMITED_PERMISSION =
            "chickenwars.quickbuy.unlimited";

    /** Numero massimo di preset per un giocatore senza permesso. */
    public static final int DEFAULT_PRESET_LIMIT = 3;

    /** Slot del menu utilizzabili dal Quick Buy. */
    private static final int FIRST_SLOT = 19;
    private static final int LAST_SLOT = 43;

    private final QuickBuyRepository repository;
    private final Logger logger;

    private final ConcurrentMap<UUID, Map<String, QuickBuyPresetRecord>> cache =
            new ConcurrentHashMap<UUID, Map<String, QuickBuyPresetRecord>>();

    public QuickBuyService(QuickBuyRepository repository, Logger logger) {
        if (repository == null) {
            throw new IllegalArgumentException("Repository Quick Buy mancante");
        }
        this.repository = repository;
        this.logger = logger;
    }

    /**
     * Carica in cache i preset del giocatore, creando quello predefinito.
     *
     * <p>Il caricamento e' asincrono nel contratto del repository; il risultato
     * viene applicato alla cache quando arriva, senza bloccare il thread
     * principale.</p>
     */
    public void load(final UUID playerId) {
        if (playerId == null) {
            return;
        }
        repository.loadPresets(playerId).whenComplete(
                new java.util.function.BiConsumer<List<QuickBuyPresetRecord>,
                        Throwable>() {
                    @Override
                    public void accept(List<QuickBuyPresetRecord> records,
                                       Throwable error) {
                        if (error != null) {
                            logger.log(Level.WARNING,
                                    "Quick Buy non caricato per " + playerId,
                                    error);
                            return;
                        }
                        applyLoaded(playerId, records);
                    }
                });
    }

    private void applyLoaded(UUID playerId, List<QuickBuyPresetRecord> records) {
        Map<String, QuickBuyPresetRecord> presets =
                new LinkedHashMap<String, QuickBuyPresetRecord>();
        if (records != null) {
            for (QuickBuyPresetRecord record : records) {
                presets.put(record.getPresetId(), record);
            }
        }
        if (presets.isEmpty()) {
            QuickBuyPresetRecord fallback = new QuickBuyPresetRecord(playerId,
                    DEFAULT_PRESET, DEFAULT_PRESET, true,
                    Collections.<Integer, String>emptyMap());
            presets.put(DEFAULT_PRESET, fallback);
            repository.savePreset(fallback);
        }
        cache.put(playerId, presets);
    }

    /**
     * Preset attualmente selezionato, creandone uno vuoto se assente.
     *
     * @return il preset selezionato, mai nullo
     */
    public QuickBuyPresetRecord getSelected(UUID playerId) {
        Map<String, QuickBuyPresetRecord> presets = presetsOf(playerId);
        for (QuickBuyPresetRecord record : presets.values()) {
            if (record.isSelected()) {
                return record;
            }
        }
        QuickBuyPresetRecord first = presets.isEmpty()
                ? null : presets.values().iterator().next();
        return first == null ? createEmpty(playerId, DEFAULT_PRESET) : first;
    }

    /**
     * Elenca i preset del giocatore in ordine di creazione.
     */
    public List<QuickBuyPresetRecord> list(UUID playerId) {
        return new ArrayList<QuickBuyPresetRecord>(presetsOf(playerId).values());
    }

    /**
     * Seleziona un preset esistente.
     *
     * @return {@code true} se il preset esisteva ed e' stato selezionato
     */
    public boolean select(UUID playerId, String presetId) {
        Map<String, QuickBuyPresetRecord> presets = presetsOf(playerId);
        String normalized = normalize(presetId);
        if (!presets.containsKey(normalized)) {
            return false;
        }
        Map<String, QuickBuyPresetRecord> updated =
                new LinkedHashMap<String, QuickBuyPresetRecord>();
        for (QuickBuyPresetRecord record : presets.values()) {
            updated.put(record.getPresetId(), copyWithSelection(record,
                    record.getPresetId().equals(normalized)));
        }
        cache.put(playerId, updated);
        repository.selectPreset(playerId, normalized);
        return true;
    }

    /**
     * Esito di una richiesta di eliminazione.
     */
    public enum DeleteResult {
        /** Preset rimosso. */
        DELETED,
        /** Nessun preset con quell'identificatore. */
        NOT_FOUND,
        /** E' l'ultimo preset rimasto e non puo' essere eliminato. */
        LAST_PRESET
    }

    /**
     * Elimina un preset garantendo che ne resti sempre uno selezionato.
     *
     * <p>Eliminando un preset inattivo la selezione non cambia; eliminando
     * quello attivo viene scelto in modo deterministico il primo rimasto
     * nell'ordine di creazione. L'ultimo preset non e' eliminabile, cosi' il
     * giocatore non resta mai senza Quick Buy.</p>
     *
     * @return l'esito dell'operazione, mai nullo
     */
    public DeleteResult delete(UUID playerId, String presetId) {
        Map<String, QuickBuyPresetRecord> presets = presetsOf(playerId);
        String normalized = normalize(presetId);

        QuickBuyPresetRecord target = presets.get(normalized);
        if (target == null) {
            return DeleteResult.NOT_FOUND;
        }
        if (presets.size() <= 1) {
            return DeleteResult.LAST_PRESET;
        }

        Map<String, QuickBuyPresetRecord> remaining =
                new LinkedHashMap<String, QuickBuyPresetRecord>(presets);
        remaining.remove(normalized);

        // Se spariva il preset attivo la selezione viene riassegnata al primo
        // rimasto: non esiste uno stato senza preset attivo.
        if (target.isSelected()) {
            Map<String, QuickBuyPresetRecord> reselected =
                    new LinkedHashMap<String, QuickBuyPresetRecord>();
            boolean first = true;
            for (QuickBuyPresetRecord record : remaining.values()) {
                reselected.put(record.getPresetId(),
                        copyWithSelection(record, first));
                first = false;
            }
            remaining = reselected;
        }

        cache.put(playerId, remaining);
        repository.deletePreset(playerId, normalized);
        for (QuickBuyPresetRecord record : remaining.values()) {
            repository.savePreset(record);
        }
        return DeleteResult.DELETED;
    }

    /**
     * Identificatore libero per un nuovo preset.
     *
     * <p>Deterministico e localizzabile: il primo libero della serie
     * {@code preset_2}, {@code preset_3}, ... senza richiedere un sistema di
     * inserimento testo.</p>
     */
    public String nextPresetId(UUID playerId) {
        Map<String, QuickBuyPresetRecord> presets = presetsOf(playerId);
        int index = presets.size() + 1;
        while (presets.containsKey("preset_" + index)) {
            index++;
        }
        return "preset_" + index;
    }

    /**
     * Crea un nuovo preset rispettando il limite consentito.
     *
     * @return {@code true} se il preset e' stato creato
     */
    public boolean create(Player player, String presetId) {
        if (player == null) {
            return false;
        }
        return create(player.getUniqueId(), presetId, getPresetLimit(player));
    }

    /**
     * Crea un preset con un limite gia' risolto.
     *
     * <p>Unica implementazione della creazione: la variante con {@code Player}
     * si limita a ricavare il limite dal permesso.</p>
     *
     * @param limit numero massimo di preset consentiti
     * @return {@code true} se il preset e' stato creato
     */
    public boolean create(UUID playerId, String presetId, int limit) {
        if (playerId == null) {
            return false;
        }
        Map<String, QuickBuyPresetRecord> presets = presetsOf(playerId);
        String normalized = normalize(presetId);

        if (presets.containsKey(normalized)) {
            return false;
        }
        if (presets.size() >= limit) {
            return false;
        }
        return createEmpty(playerId, normalized) != null;
    }

    /**
     * Limite di preset applicabile al giocatore indicato.
     *
     * @return il limite, oppure {@link Integer#MAX_VALUE} con il permesso
     */
    public int getPresetLimit(Player player) {
        return player != null && player.hasPermission(UNLIMITED_PERMISSION)
                ? Integer.MAX_VALUE : DEFAULT_PRESET_LIMIT;
    }

    /**
     * Indica se il giocatore ha raggiunto il limite di preset.
     */
    public boolean isAtLimit(Player player) {
        return player != null
                && presetsOf(player.getUniqueId()).size()
                >= getPresetLimit(player);
    }

    /**
     * Assegna un articolo a uno slot del preset selezionato.
     *
     * <p>Assegnare lo stesso articolo allo stesso slot non crea duplicati:
     * la mappa viene riscritta per intero.</p>
     *
     * @param slot   slot del menu, deve appartenere all'area Quick Buy
     * @param itemId articolo da associare, {@code null} per liberare lo slot
     * @return {@code true} se la disposizione e' cambiata
     */
    public boolean assign(UUID playerId, int slot, String itemId) {
        if (!isQuickBuySlot(slot)) {
            return false;
        }
        QuickBuyPresetRecord selected = getSelected(playerId);
        Map<Integer, String> slots =
                new LinkedHashMap<Integer, String>(selected.getSlots());

        String normalized = itemId == null || itemId.trim().isEmpty()
                ? null : itemId.trim().toLowerCase(Locale.ROOT);
        String previous = slots.get(Integer.valueOf(slot));
        if (normalized == null && previous == null) {
            return false;
        }
        if (normalized != null && normalized.equals(previous)) {
            return false;
        }

        if (normalized == null) {
            slots.remove(Integer.valueOf(slot));
        } else {
            slots.put(Integer.valueOf(slot), normalized);
        }

        QuickBuyPresetRecord updated = new QuickBuyPresetRecord(playerId,
                selected.getPresetId(), selected.getDisplayName(), true, slots);
        store(playerId, updated);
        return true;
    }

    /**
     * Disposizione del preset selezionato, ripulita dagli articoli non piu'
     * presenti nella configurazione.
     *
     * @param configuration catalogo corrente, usato per validare gli articoli
     * @return mappa slot verso articolo, mai nulla
     */
    public Map<Integer, ShopItemDefinition> resolveSlots(
            UUID playerId, ShopConfiguration configuration) {
        Map<Integer, ShopItemDefinition> resolved =
                new LinkedHashMap<Integer, ShopItemDefinition>();
        if (configuration == null) {
            return resolved;
        }
        for (Map.Entry<Integer, String> entry
                : getSelected(playerId).getSlots().entrySet()) {
            ShopItemDefinition item = configuration.getItem(entry.getValue());
            if (item != null && isQuickBuySlot(entry.getKey().intValue())) {
                resolved.put(entry.getKey(), item);
            }
        }
        return resolved;
    }

    /**
     * Indica se lo slot appartiene all'area utilizzabile dal Quick Buy.
     */
    public static boolean isQuickBuySlot(int slot) {
        return slot >= FIRST_SLOT && slot <= LAST_SLOT;
    }

    private QuickBuyPresetRecord createEmpty(UUID playerId, String presetId) {
        QuickBuyPresetRecord record = new QuickBuyPresetRecord(playerId,
                presetId, presetId, false,
                Collections.<Integer, String>emptyMap());
        store(playerId, record);
        return record;
    }

    private void store(UUID playerId, QuickBuyPresetRecord record) {
        Map<String, QuickBuyPresetRecord> presets = presetsOf(playerId);
        Map<String, QuickBuyPresetRecord> updated =
                new LinkedHashMap<String, QuickBuyPresetRecord>(presets);
        updated.put(record.getPresetId(), record);
        cache.put(playerId, updated);
        repository.savePreset(record);
    }

    private QuickBuyPresetRecord copyWithSelection(QuickBuyPresetRecord record,
                                                   boolean selected) {
        return new QuickBuyPresetRecord(record.getPlayerId(),
                record.getPresetId(), record.getDisplayName(), selected,
                record.getSlots());
    }

    private Map<String, QuickBuyPresetRecord> presetsOf(UUID playerId) {
        Map<String, QuickBuyPresetRecord> presets = cache.get(playerId);
        if (presets != null) {
            return presets;
        }
        Map<String, QuickBuyPresetRecord> created =
                new LinkedHashMap<String, QuickBuyPresetRecord>();
        QuickBuyPresetRecord fallback = new QuickBuyPresetRecord(playerId,
                DEFAULT_PRESET, DEFAULT_PRESET, true,
                Collections.<Integer, String>emptyMap());
        created.put(DEFAULT_PRESET, fallback);
        Map<String, QuickBuyPresetRecord> existing =
                cache.putIfAbsent(playerId, created);
        return existing == null ? created : existing;
    }

    private String normalize(String presetId) {
        return presetId == null || presetId.trim().isEmpty()
                ? DEFAULT_PRESET : presetId.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Dimentica la cache di un giocatore uscito dal server.
     */
    public void unload(UUID playerId) {
        cache.remove(playerId);
    }
}
