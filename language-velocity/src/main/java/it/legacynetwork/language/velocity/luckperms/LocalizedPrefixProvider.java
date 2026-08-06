package it.legacynetwork.language.velocity.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class LocalizedPrefixProvider {

    private final LuckPerms luckPerms;
    private final Logger logger;
    private final ConcurrentHashMap<String, CachedPrefix> cache =
            new ConcurrentHashMap<>();
    private final ExecutorService executor =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "lp-prefix");
                t.setDaemon(true);
                return t;
            });

    public LocalizedPrefixProvider(LuckPerms luckPerms, Logger logger) {
        this.luckPerms = luckPerms;
        this.logger = logger;
    }

    public CompletableFuture<String> getPrefix(UUID targetUuid,
                                                String viewerLang,
                                                String viewerLocale) {
        String cacheKey = targetUuid + "|" + viewerLang + "|" + viewerLocale;
        CachedPrefix cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.value);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                User target = luckPerms.getUserManager().loadUser(targetUuid).get();
                if (target == null) return "";

                String prefix = resolveWithFallback(target, viewerLang, viewerLocale);
                cache.put(cacheKey, new CachedPrefix(prefix));
                return prefix;
            } catch (Exception e) {
                logger.warning("LocalizedPrefix failed for " + targetUuid
                        + ": " + e.getMessage());
                return "";
            }
        }, executor);
    }

    private String resolveWithFallback(User target, String lang, String locale) {
        String prefix = resolve(target, buildContext(lang, locale));
        if (prefix != null && !prefix.isEmpty()) return prefix;

        prefix = resolve(target, buildContext(lang, null));
        if (prefix != null && !prefix.isEmpty()) return prefix;

        prefix = resolve(target, buildContext("en", "en_us"));
        if (prefix != null && !prefix.isEmpty()) return prefix;

        prefix = resolveWithoutContext(target);
        return prefix != null ? prefix : "";
    }

    private String resolve(User target, ImmutableContextSet context) {
        QueryOptions options = QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(context).build();
        return target.getCachedData().getMetaData(options).getPrefix();
    }

    private String resolveWithoutContext(User target) {
        QueryOptions options = QueryOptions.nonContextual();
        return target.getCachedData().getMetaData(options).getPrefix();
    }

    private ImmutableContextSet buildContext(String lang, String locale) {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder()
                .add("lang", lang);
        if (locale != null && !locale.isEmpty()) {
            builder.add("locale", locale);
        }
        return builder.build();
    }

    public void invalidate(UUID targetUuid) {
        String prefix = targetUuid.toString() + "|";
        cache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void invalidateAll() {
        cache.clear();
    }

    public void close() {
        executor.shutdown();
        cache.clear();
    }

    static class CacheExpiredException extends RuntimeException {
    }

    static final class CachedPrefix {
        final String value;
        final long timestamp;

        CachedPrefix(String value) {
            this.value = value != null ? value : "";
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 30_000;
        }
    }
}
