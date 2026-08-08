package it.legacynetwork.reports.repository;

import it.legacynetwork.reports.model.Report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pagina di risultati.
 *
 * <p>La numerazione mostrata allo staff parte da 1: e' quella che si digita nel
 * comando, quindi e' quella che viene conservata qui.</p>
 */
public final class ReportPage {

    private final List<Report> items;
    private final int page;
    private final int pageSize;
    private final long totalItems;

    public ReportPage(List<Report> items, int page, int pageSize,
                      long totalItems) {
        this.items = Collections.unmodifiableList(
                new ArrayList<>(items == null ? Collections.emptyList() : items));
        this.page = Math.max(1, page);
        this.pageSize = Math.max(1, pageSize);
        this.totalItems = Math.max(0L, totalItems);
    }

    public static ReportPage empty(int page, int pageSize) {
        return new ReportPage(Collections.emptyList(), page, pageSize, 0L);
    }

    public List<Report> getItems() {
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
