package it.legacynetwork.chickenwars.routing;

/** Pubblica lo snapshot corrente senza possedere task Bukkit. */
public final class InstanceHeartbeat {
    public interface DescriptorSupplier { GameInstanceDescriptor current(long now); }
    private final InstanceRegistry registry;
    private final DescriptorSupplier supplier;
    public InstanceHeartbeat(InstanceRegistry registry, DescriptorSupplier supplier) {
        if (registry == null || supplier == null) throw new IllegalArgumentException("Heartbeat incompleto");
        this.registry = registry; this.supplier = supplier;
    }
    public void pulse(long now) { registry.heartbeat(supplier.current(now)); }
}
