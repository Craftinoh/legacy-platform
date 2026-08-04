package it.legacynetwork.lobby.bossbar;

import it.legacynetwork.lobby.placeholder.PlaceholderService;
import it.legacynetwork.lobby.util.LegacyColorTranslator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BossBarTextRenderer {
    private final BossBarConfiguration config;
    private final PlaceholderService placeholderService;
    private final String serverId;

    public BossBarTextRenderer(BossBarConfiguration config,
                               PlaceholderService placeholderService,
                               String serverId) {
        this.config = config;
        this.placeholderService = placeholderService;
        this.serverId = serverId;
    }

    public String renderText(Player player, BossBarDefinition bar, String languageCode) {
        String raw = bar.getText(languageCode);
        if (raw == null) {
            return "";
        }
        return resolve(player, raw);
    }

    private int getOnlineCount() {
        try {
            return Bukkit.getOnlinePlayers().size();
        } catch (Exception e) {
            return 0;
        }
    }

    public double calculateProgress(Player player, BossBarProgress progress, int tickCount) {
        switch (progress.getType()) {
            case STATIC:
                return clamp(progress.getStaticValue());
            case COUNTDOWN: {
                double range = progress.getStart() - progress.getEnd();
                double step = range / Math.max(1, progress.getDurationTicks());
                double value = progress.getStart() - (step * tickCount);
                if (progress.getStart() > progress.getEnd()) {
                    return clamp(Math.max(progress.getEnd(), value));
                } else {
                    return clamp(Math.min(progress.getEnd(), value));
                }
            }
            case COUNTUP: {
                double range = progress.getEnd() - progress.getStart();
                double step = range / Math.max(1, progress.getDurationTicks());
                double value = progress.getStart() + (step * tickCount);
                if (progress.getEnd() > progress.getStart()) {
                    return clamp(Math.min(progress.getEnd(), value));
                } else {
                    return clamp(Math.max(progress.getEnd(), value));
                }
            }
            case PLACEHOLDER_RATIO: {
                try {
                    String currentRaw = progress.getCurrentPlaceholder();
                    String maxRaw = progress.getMaximumPlaceholder();

                    currentRaw = currentRaw.replace("{online}",
                            String.valueOf(getOnlineCount()));
                    maxRaw = maxRaw.replace("{online}",
                            String.valueOf(getOnlineCount()));

                    if (placeholderService.isAvailable() && config.isPlaceholderApiEnabled()
                            && player != null) {
                        currentRaw = placeholderService.apply(player, currentRaw);
                        maxRaw = placeholderService.apply(player, maxRaw);
                    }

                    double current = Double.parseDouble(currentRaw.trim());
                    double maximum = Double.parseDouble(maxRaw.trim());
                    if (maximum <= 0) {
                        return clamp(progress.getFallback());
                    }
                    return clamp(current / maximum);
                } catch (NumberFormatException e) {
                    return clamp(progress.getFallback());
                }
            }
            default:
                return clamp(progress.getFallback());
        }
    }

    private String resolve(Player player, String raw) {
        String result = raw;
        String playerName = player != null ? player.getName() : "???";
        String locale = player != null ? player.spigot().getLocale() : "en";
        result = result.replace("{player}", playerName);
        result = result.replace("{server}", serverId);
        result = result.replace("{online}", String.valueOf(getOnlineCount()));
        result = result.replace("{language}", locale);
        result = result.replace("{language_code}", locale);
        if (placeholderService.isAvailable() && config.isPlaceholderApiEnabled()
                && player != null) {
            result = placeholderService.apply(player, result);
        }
        return LegacyColorTranslator.translate(result);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
