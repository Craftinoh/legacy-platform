package it.legacynetwork.screenshare.repository;

import it.legacynetwork.screenshare.model.ScreenshareSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pagina di risultati, numerata a partire da 1 come la si digita.
 */
public final class ScreensharePage {

    private final List<ScreenshareSession> items;
    private final int page;
    private final int pageSize;
    private final long totalItems;

    public ScreensharePage(List<ScreenshareSession> items, int page,
                           int pageSize, long totalItems) {
        this.items = Collections.unmodifiableList(new ArrayList<>(
                items == null ? Collections.emptyList() : items));
        this.page = Math.max(1, page);
        this.pageSize = Math.max(1, pageSize);
        this.totalItems = Math.max(0L, totalItems);
    }

    public List<ScreenshareSession> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        if (totalItems == 0L) {
            return 1;
        }
        return (int) ((totalItems + pageSize - 1) / pageSize);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean hasNextPage() {
        return page < getTotalPages();
    }

    public boolean hasPreviousPage() {
        return page > 1;
    }
}
