package it.legacynetwork.combat.fireball;

import it.legacynetwork.combat.config.CombatConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FireballTracker {

    private final Map<UUID, FireballData> tracked = new ConcurrentHashMap<>();

    public void register(UUID projectileUuid, FireballData data) {
        tracked.put(projectileUuid, data);
    }

    public FireballData find(UUID projectileUuid) {
        return tracked.get(projectileUuid);
    }

    public FireballData remove(UUID projectileUuid) {
        return tracked.remove(projectileUuid);
    }

    public void cleanup() {
        tracked.clear();
    }

    public static final class FireballData {

        private final UUID shooterUuid;
        private final long launchTime;
        private final double damage;
        private final double explosionPower;
        private final boolean blockDamage;
        private final boolean fire;
        private final int shooterImmunityTicks;
        private boolean exploded;

        public FireballData(UUID shooterUuid, long launchTime,
                            double damage, double explosionPower,
                            boolean blockDamage, boolean fire,
                            int shooterImmunityTicks) {
            this.shooterUuid = shooterUuid;
            this.launchTime = launchTime;
            this.damage = damage;
            this.explosionPower = explosionPower;
            this.blockDamage = blockDamage;
            this.fire = fire;
            this.shooterImmunityTicks = shooterImmunityTicks;
            this.exploded = false;
        }

        public FireballData(UUID shooterUuid, long launchTime,
                            CombatConfig config) {
            this(shooterUuid, launchTime, config.getFireballDamage(),
                    config.getFireballExplosionPower(),
                    config.isFireballBlockDamage(),
                    config.isFireballFire(),
                    config.getFireballShooterImmunityTicks());
        }

        public UUID getShooterUuid() {
            return shooterUuid;
        }

        public long getLaunchTime() {
            return launchTime;
        }

        public double getDamage() {
            return damage;
        }

        public double getExplosionPower() {
            return explosionPower;
        }

        public boolean isBlockDamage() {
            return blockDamage;
        }

        public boolean isFire() {
            return fire;
        }

        public int getShooterImmunityTicks() {
            return shooterImmunityTicks;
        }

        public boolean hasExploded() {
            return exploded;
        }

        public void setExploded(boolean exploded) {
            this.exploded = exploded;
        }
    }
}
