package com.marques.hiraishinkunai.entity;

import com.marques.hiraishinkunai.HiraishinKunai;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

import java.util.Optional;
import java.util.UUID;

/** Thrown Hiraishin Kunai. It remains at block impacts and can be picked up or clicked to teleport. */
public final class HiraishinProjectile extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> STUCK = SynchedEntityData.defineId(HiraishinProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> MAX_RANGE = SynchedEntityData.defineId(HiraishinProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RETURN_ITEM = SynchedEntityData.defineId(HiraishinProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(HiraishinProjectile.class, EntityDataSerializers.OPTIONAL_UUID);

    private Vec3 launchOrigin;
    private boolean itemReturned;

    public HiraishinProjectile(EntityType<? extends HiraishinProjectile> type, Level level) {
        super(type, level);
    }

    public HiraishinProjectile(Level level, Player owner, double maxRange, boolean returnItem) {
        super(HiraishinKunai.KUNAI_PROJECTILE.get(), owner, level);
        this.entityData.set(MAX_RANGE, (float) Math.max(4.0D, Math.min(40.0D, maxRange)));
        this.entityData.set(RETURN_ITEM, returnItem);
        this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
        this.launchOrigin = owner.position();
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STUCK, false);
        this.entityData.define(MAX_RANGE, 40.0F);
        this.entityData.define(RETURN_ITEM, true);
        this.entityData.define(OWNER_UUID, Optional.empty());
    }

    @Override protected Item getDefaultItem() { return HiraishinKunai.HIRAISHIN_KUNAI.get(); }

    public void setLaunchOrigin(Vec3 origin) { this.launchOrigin = origin; }
    public boolean isStuck() { return this.entityData.get(STUCK); }
    public double getMaxRange() { return this.entityData.get(MAX_RANGE); }
    public Vec3 getLaunchOrigin() { return this.launchOrigin == null ? this.position() : this.launchOrigin; }
    public boolean shouldReturnItem() { return this.entityData.get(RETURN_ITEM); }
    public boolean isOwnedBy(UUID uuid) { return uuid != null && this.entityData.get(OWNER_UUID).filter(uuid::equals).isPresent(); }
    public Optional<UUID> getOwnerUUID() { return this.entityData.get(OWNER_UUID); }
    public Vec3 getIndicatorPosition(float partialTick) { return this.position(); }

    @Override
    public void tick() {
        if (this.launchOrigin == null) this.launchOrigin = this.position();
        super.tick();
        if (this.isRemoved()) return;

        if (!this.level().isClientSide) {
            if (!isStuck() && this.launchOrigin != null) {
                Vec3 fromOrigin = this.position().subtract(this.launchOrigin);
                if (fromOrigin.lengthSqr() >= getMaxRange() * getMaxRange()) {
                    Vec3 direction = fromOrigin.lengthSqr() < 1.0E-7 ? new Vec3(0, 0, 1) : fromOrigin.normalize();
                    this.setPos(this.launchOrigin.add(direction.scale(getMaxRange())));
                    stopInPlace();
                }
            }
            if (isStuck()) {
                this.setDeltaMovement(Vec3.ZERO);
                tryPickupByNearbyPlayer();
            }
        } else if (isStuck()) {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.level().isClientSide) return;
        Vec3 normal = new Vec3(result.getDirection().getStepX(), result.getDirection().getStepY(), result.getDirection().getStepZ());
        this.setPos(result.getLocation().add(normal.scale(0.12D)));
        stopInPlace();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide) return;
        Entity hit = result.getEntity();
        if (hit instanceof LivingEntity living && living.isAlive() && !(hit instanceof Player p && isOwnedBy(p.getUUID()))) {
            UUID owner = this.entityData.get(OWNER_UUID).orElse(null);
            if (owner != null) {
                Vec3 look = living.getLookAngle();
                Vec3 impact = result.getLocation().subtract(living.position().add(0.0D, living.getBbHeight() * 0.55D, 0.0D));
                boolean front = impact.x * look.x + impact.z * look.z >= 0.0D;
                HiraishinMarkEntity mark = new HiraishinMarkEntity(this.level(), owner, living, front);
                this.level().addFreshEntity(mark);
            }
        }
        // A hit on a living entity becomes a tag. It does not physically stick or remain as an item.
        this.discard();
    }

    private void stopInPlace() {
        this.entityData.set(STUCK, true);
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
        this.setNoPhysics(true);
    }

    private void tryPickupByNearbyPlayer() {
        if (this.itemReturned || !isStuck()) return;
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(0.65D))) {
            if (!player.isAlive() || player.isSpectator() || !isOwnedBy(player.getUUID())) continue;
            // Vanilla arrow-style pickup: only the owner can recover this thrown item.
            returnItemTo(player);
            this.discard();
            return;
        }
    }

    public void returnItemTo(Player player) {
        if (this.itemReturned || !shouldReturnItem()) return;
        this.itemReturned = true;
        ItemStack item = new ItemStack(HiraishinKunai.HIRAISHIN_KUNAI.get());
        if (!player.getInventory().add(item)) player.drop(item, false);
        player.take(this, 1);
    }

    @Override public boolean isPickable() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Stuck", isStuck());
        tag.putFloat("MaxRange", this.entityData.get(MAX_RANGE));
        tag.putBoolean("ReturnItem", shouldReturnItem());
        if (this.entityData.get(OWNER_UUID).isPresent()) tag.putUUID("HiraishinOwner", this.entityData.get(OWNER_UUID).get());
        if (launchOrigin != null) {
            tag.putDouble("OriginX", launchOrigin.x);
            tag.putDouble("OriginY", launchOrigin.y);
            tag.putDouble("OriginZ", launchOrigin.z);
        }
        tag.putBoolean("ItemReturned", itemReturned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(STUCK, tag.getBoolean("Stuck"));
        this.entityData.set(MAX_RANGE, tag.contains("MaxRange") ? tag.getFloat("MaxRange") : 40.0F);
        this.entityData.set(RETURN_ITEM, !tag.contains("ReturnItem") || tag.getBoolean("ReturnItem"));
        if (tag.hasUUID("HiraishinOwner")) this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("HiraishinOwner")));
        if (tag.contains("OriginX") && tag.contains("OriginY") && tag.contains("OriginZ")) {
            launchOrigin = new Vec3(tag.getDouble("OriginX"), tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
        } else launchOrigin = this.position();
        itemReturned = tag.getBoolean("ItemReturned");
        if (isStuck()) { this.setNoGravity(true); this.setNoPhysics(true); }
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
