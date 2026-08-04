package it.legacynetwork.items.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomItemFingerprintTest {

    @Test
    void computeProducesDeterministicId() {
        java.util.List<it.legacynetwork.items.definition.CustomItemTrigger> triggers =
                new java.util.ArrayList<>();
        triggers.add(it.legacynetwork.items.definition.CustomItemTrigger.JOIN);
        java.util.Map<String, it.legacynetwork.items.definition.CustomItemLanguage> langs =
                new java.util.LinkedHashMap<>();
        java.util.Map<String, it.legacynetwork.items.definition.CustomItemClickActions> acts =
                new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> enchants = new java.util.LinkedHashMap<>();
        java.util.List<String> flags = new java.util.ArrayList<>();

        it.legacynetwork.items.definition.CustomItemFlags itemFlags =
                new it.legacynetwork.items.definition.CustomItemFlags(
                        true, true, true, true, true, true, true);

        it.legacynetwork.items.definition.CustomItemDefinition def =
                new it.legacynetwork.items.definition.CustomItemDefinition(
                        "server-selector", true, "COMPASS", 0, 1, 1, "", triggers,
                        "ALL", new java.util.ArrayList<>(), false, "",
                        langs, itemFlags, acts, enchants, flags);

        String fp1 = CustomItemFingerprint.compute(def);
        String fp2 = CustomItemFingerprint.compute(def);
        assertEquals(fp1, fp2);
        assertTrue(fp1.contains("server-selector"));
        assertTrue(fp1.contains("COMPASS"));
    }
}
