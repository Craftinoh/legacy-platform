package it.legacynetwork.language.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import it.legacynetwork.language.velocity.repository.FileLanguageRepository;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ProxyShutdownListener {
    private final FileLanguageRepository repository;
    private final AtomicBoolean closed = new AtomicBoolean();

    public ProxyShutdownListener(FileLanguageRepository repository) {
        this.repository = repository;
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (closed.compareAndSet(false, true)) {
            repository.close();
        }
    }
}
