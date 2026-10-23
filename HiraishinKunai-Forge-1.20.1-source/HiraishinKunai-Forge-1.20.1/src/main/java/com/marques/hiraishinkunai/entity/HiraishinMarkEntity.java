package com.marques.hiraishinkunai.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

import java.util.Optional;
import java.util.UUID;

/** Persistent visual mark anchored to one block face or to the front/back of a living entity's chest. */
public final class HiraishinMarkEntity extends Entity {
    public static final int BLOCK_ANCHOR = 0;
    public static final int ENTITY_ANCHOR = 1;

    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(HiraishinMarkEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> ANCHOR_TYPE = SynchedEntityData.defineId(HiraishinMarkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> BLOCK_POS = SynchedEntityData.defineId(HiraishinMarkEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(HiraishinMarkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> TARGET_UUID = SynchedEntityData.defineId(HiraishinMarkEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(HiraishinMarkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIDE = SynchedEntityData.defineId(HiraishinMarkEntity.class, EntityDataSerializers.INT);

    public HiraishinMarkEntity(EntityType<? extends HiraishinMarkEntity> type, Level level) { super(type, level); }

    public HiraishinMarkEntity(Level level, UUID owner, BlockPos pos, Direction face) {
        this(com.marques.hiraishinkunai.HiraishinKunai.KUNAI_MARK.get(), level);
        this.entityData.set(OWNER, Optional.of(owner));
        this.entityData.set(ANCHOR_TYPE, BLOCK_ANCHOR);
        this.entityData.set(BLOCK_POS, pos.immutable());
        this.entityData.set(FACE, face.ordinal());
        moveToBlockFace();
    }

    public HiraishinMarkEntity(Level level, UUID owner, LivingEntity target, boolean front) {
        this(com.marques.hiraishinkunai.HiraishinKunai.KUNAI_MARK.get(), level);
        this.entityData.set(OWNER, Optional.of(owner));
        this.entityData.set(ANCHOR_TYPE, ENTITY_ANCHOR);
        this.entityData.set(TARGET_UUID, Optional.of(target.getUUID()));
        this.entityData.set(TARGET_ID, target.getId());
        this.entityData.set(SIDE, front ? 1 : -1);
        moveToEntityFace(target);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(ANCHOR_TYPE, BLOCK_ANCHOR);
        this.entityData.define(BLOCK_POS, BlockPos.ZERO);
        this.entityData.define(FACE, Direction.UP.ordinal());
        this.entityData.define(TARGET_UUID, Optional.empty());
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(SIDE, 1);
    }

    @Override public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        if (getAnchorType() == BLOCK_ANCHOR) {
            if (!this.level().isClientSide && !this.level().getBlockState(getBlockPos()).isAir()) {
                moveToBlockFace();
            } else if (!this.level().isClientSide && this.level().getBlockState(getBlockPos()).isAir()) {
                this.discard();
            } else {
                moveToBlockFace();
            }
            return;
        }

        LivingEntity target = findTarget();
        if (target == null) return; // Keep the saved mark while the target is unloaded.
        if (!target.isAlive()) { if (!this.level().isClientSide) this.discard(); return; }
        moveToEntityFace(target);
    }

    private LivingEntity findTarget() {
        Entity target = this.entityData.get(TARGET_ID) < 0 ? null : this.level().getEntity(this.entityData.get(TARGET_ID));
        UUID targetUuid = this.entityData.get(TARGET_UUID).orElse(null);
        if (target instanceof LivingEntity living && (targetUuid == null || targetUuid.equals(living.getUUID()))) return living;
        if (!this.level().isClientSide && targetUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity byUuid = serverLevel.getEntity(targetUuid);
            if (byUuid instanceof LivingEntity living) {
                this.entityData.set(TARGET_ID, living.getId());
                return living;
            }
        }
        return null;
    }

    private void moveToBlockFace() {
        BlockPos pos = getBlockPos();
        Direction direction = getFace();
        Vec3 faceCenter = Vec3.atCenterOf(pos).add(direction.getStepX() * 0.56D, direction.getStepY() * 0.56D, direction.getStepZ() * 0.56D);
        // The entity's origin is offset down from the center of the billboard quad.
        this.setPos(faceCenter.x, faceCenter.y - 0.34D, faceCenter.z);
    }

    private void moveToEntityFace(LivingEntity target) {
        Vec3 forward = target.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() < 1.0E-5D) forward = new Vec3(0, 0, 1);
        else forward = forward.normalize();
        forward = forward.scale(this.entityData.get(SIDE));
        Vec3 chest = target.position().add(0.0D, target.getBbHeight() * 0.56D, 0.0D);
        Vec3 point = chest.add(forward.scale(target.getBbWidth() * 0.5D + 0.12D));
        this.setPos(point.x, point.y - 0.34D, point.z);
    }

    public int getAnchorType() { return this.entityData.get(ANCHOR_TYPE); }
    public BlockPos getBlockPos() { return this.entityData.get(BLOCK_POS); }
    public Direction getFace() { int i = this.entityData.get(FACE); Direction[] values = Direction.values(); return i >= 0 && i < values.length ? values[i] : Direction.UP; }
    public Optional<UUID> getOwnerUUID() { return this.entityData.get(OWNER); }
    public Optional<UUID> getTargetUUID() { return this.entityData.get(TARGET_UUID); }
    public boolean isOwnedBy(UUID uuid) { return uuid != null && getOwnerUUID().filter(uuid::equals).isPresent(); }
    public boolean matchesBlock(BlockPos pos) { return getAnchorType() == BLOCK_ANCHOR && getBlockPos().equals(pos); }
    public boolean matchesTarget(UUID uuid) { return getAnchorType() == ENTITY_ANCHOR && getTargetUUID().filter(uuid::equals).isPresent(); }

    public Vec3 getIndicatorPosition(float partialTick) { return this.position().add(0.0D, 0.34D, 0.0D); }
    public float getAppearanceAlpha(float partialTick) { return Math.min(1.0F, (this.tickCount + partialTick) / 8.0F); }

    public Vec3 getTeleportPosition() {
        if (getAnchorType() == BLOCK_ANCHOR) {
            Direction face = getFace();
            Vec3 center = Vec3.atCenterOf(getBlockPos()).add(face.getStepX() * 0.505D, face.getStepY() * 0.505D, face.getStepZ() * 0.505D);
            return center.add(face.getStepX() * 0.9D, face.getStepY() * 0.9D + 0.05D, face.getStepZ() * 0.9D);
        }
        LivingEntity target = findTarget();
        if (target != null) {
            Vec3 forward = target.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
            if (forward.lengthSqr() < 1.0E-5D) forward = new Vec3(0, 0, 1); else forward = forward.normalize();
            forward = forward.scale(this.entityData.get(SIDE));
            return target.position().add(forward.scale(target.getBbWidth() * 0.5D + 1.0D));
        }
        return this.position().add(0.0D, 0.4D, 0.0D);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public EntityDimensions getDimensions(Pose pose) { return EntityDimensions.fixed(0.22F, 0.22F); }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        if (getOwnerUUID().isPresent()) tag.putUUID("HiraishinOwner", getOwnerUUID().get());
        tag.putInt("AnchorType", getAnchorType());
        tag.putLong("BlockPos", getBlockPos().asLong());
        tag.putInt("Face", getFace().ordinal());
        if (getTargetUUID().isPresent()) tag.putUUID("TargetUUID", getTargetUUID().get());
        tag.putInt("TargetId", this.entityData.get(TARGET_ID));
        tag.putInt("Side", this.entityData.get(SIDE));
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("HiraishinOwner")) this.entityData.set(OWNER, Optional.of(tag.getUUID("HiraishinOwner")));
        this.entityData.set(ANCHOR_TYPE, tag.getInt("AnchorType"));
        this.entityData.set(BLOCK_POS, BlockPos.of(tag.getLong("BlockPos")));
        this.entityData.set(FACE, tag.getInt("Face"));
        this.entityData.set(TARGET_UUID, tag.hasUUID("TargetUUID") ? Optional.of(tag.getUUID("TargetUUID")) : Optional.empty());
        this.entityData.set(TARGET_ID, tag.getInt("TargetId"));
        this.entityData.set(SIDE, tag.contains("Side") ? tag.getInt("Side") : 1);
        if (getAnchorType() == BLOCK_ANCHOR) moveToBlockFace();
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
