package it.legacynetwork.chickenwars.generator;

public interface GeneratorDropSink {
    boolean drop(GeneratorState state,int amount);
    void cleanup();
}
