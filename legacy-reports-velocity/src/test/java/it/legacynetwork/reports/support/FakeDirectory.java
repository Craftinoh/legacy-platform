package it.legacynetwork.reports.support;

import it.legacynetwork.reports.platform.OnlinePlayer;
import it.legacynetwork.reports.platform.PlayerDirectory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Elenco dei collegati, controllabile dal test.
 */
public final class FakeDirectory implements PlayerDirectory {

    private final Map<UUID, FakePlayer> online = new LinkedHashMap<>();

    public FakeDirectory add(FakePlayer... players) {
        for (FakePlayer player : players) {
            online.put(player.uniqueId(), player);
        }
        return this;
    }

    public FakeDirectory remove(FakePlayer player) {
        online.remove(player.uniqueId());
        return this;
    }

    @Override
    public Optional<OnlinePlayer> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        for (FakePlayer player : online.values()) {
            if (player.name().equalsIgnoreCase(name.trim())) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<OnlinePlayer> findById(UUID playerId) {
        return Optional.ofNullable(online.get(playerId));
    }

    @Override
    public List<OnlinePlayer> withPermission(String node) {
        List<OnlinePlayer> matches = new ArrayList<>();
        for (FakePlayer player : online.values()) {
            if (player.hasPermission(node)) {
                matches.add(player);
            }
        }
        return matches;
    }

    @Override
    public List<String> names() {
        List<String> names = new ArrayList<>();
        for (FakePlayer player : online.values()) {
            names.add(player.name());
        }
        names.sort((left, right) -> left.toLowerCase(Locale.ROOT)
                .compareTo(right.toLowerCase(Locale.ROOT)));
        return names;
    }
}
