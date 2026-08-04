package it.legacynetwork.language.velocity.tablist;

import java.util.Collections;
import java.util.List;

public final class TabListLanguageSection {
    private final List<String> header;
    private final List<String> footer;

    public TabListLanguageSection(List<String> header, List<String> footer) {
        this.header = header != null
                ? Collections.unmodifiableList(header)
                : Collections.emptyList();
        this.footer = footer != null
                ? Collections.unmodifiableList(footer)
                : Collections.emptyList();
    }

    public List<String> getHeader() {
        return header;
    }

    public List<String> getFooter() {
        return footer;
    }
}
