package it.legacynetwork.language.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;

public final class NetworkLangReloadCommand implements SimpleCommand {
    private final NetworkLanguagePlugin plugin;

    public NetworkLangReloadCommand(NetworkLanguagePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("networklang.admin")) {
            source.sendMessage(Component.text("Permission denied."));
            return;
        }
        plugin.reloadAll();
        source.sendMessage(Component.text("NetworkLanguage reloaded successfully."));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("networklang.admin");
    }
}
