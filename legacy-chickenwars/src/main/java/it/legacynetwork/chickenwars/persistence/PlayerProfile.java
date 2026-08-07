package it.legacynetwork.chickenwars.persistence;

import it.legacynetwork.chickenwars.statistics.ModeStatistics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class PlayerProfile {
    private final UUID playerId; private final PlayerProgressRecord progress;
    private final List<ModeStatistics> statistics;
    private final List<QuickBuyPresetRecord> quickBuy;
    public PlayerProfile(UUID playerId, PlayerProgressRecord progress,
            List<ModeStatistics> statistics, List<QuickBuyPresetRecord> quickBuy) {
        this.playerId=playerId;this.progress=progress;
        this.statistics=Collections.unmodifiableList(new ArrayList<ModeStatistics>(statistics));
        this.quickBuy=Collections.unmodifiableList(new ArrayList<QuickBuyPresetRecord>(quickBuy));
    }
    public UUID getPlayerId(){return playerId;}
    public PlayerProgressRecord getProgress(){return progress;}
    public List<ModeStatistics> getStatistics(){return statistics;}
    public List<QuickBuyPresetRecord> getQuickBuy(){return quickBuy;}
}
