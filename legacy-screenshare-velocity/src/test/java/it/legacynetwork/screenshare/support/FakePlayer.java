package it.legacynetwork.screenshare.support;

import it.legacynetwork.screenshare.message.ChatLine;
import it.legacynetwork.screenshare.platform.CommandActor;
import it.legacynetwork.screenshare.platform.OnlinePlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Giocatore di prova: mittente di comandi e destinatario di messaggi.
 */
public final class FakePlayer implements OnlinePlayer, CommandActor {

    private final UUID id;
    private final String name;
    private final Set<String> permissions = new LinkedHashSet<>();
    private final List<ChatLine> received = new ArrayList<>();

    private String serverId = "lobby-1";

    public FakePlayer(String name) {
        this(UUID.randomUUID(), name);
    }

    public FakePlayer(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public FakePlayer allow(String... nodes) {
        Collections.addAll(permissions, nodes);
        return this;
    }

    public FakePlayer on(String serverId) {
        this.serverId = serverId;
        return this;
    }

    @Override
    public UUID uniqueId() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String serverId() {
        return serverId;
    }

    @Override
    public boolean hasPermission(String node) {
        return permissions.contains(node);
    }

    @Override
    public void send(ChatLine line) {
        received.add(line);
    }

    public List<ChatLine> received() {
        return Collections.unmodifiableList(received);
    }

    public void clear() {
        received.clear();
    }

    public String text() {
        StringBuilder builder = new StringBuilder();
        for (ChatLine line : received) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line.plainText());
        }
        return builder.toString();
    }
}
