package com.marques.hiraishinkunai;

import com.marques.hiraishinkunai.entity.HiraishinMarkEntity;
import com.marques.hiraishinkunai.entity.HiraishinProjectile;
import com.marques.hiraishinkunai.item.HiraishinKunaiItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import java.util.HashMap;
import java.util.Map;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = HiraishinKunai.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HiraishinEvents {
    private static final Map<UUID, Integer> LAST_ENTITY_MARK_TICK = new HashMap<>();

    private HiraishinEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || !(player.getMainHandItem().getItem() instanceof HiraishinKunaiItem) || !player.isShiftKeyDown()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide || !(event.getLevel() instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) return;

        BlockPos pos = event.getPos().immutable();
        UUID owner = player.getUUID();
        HiraishinMarkEntity old = level.getEntitiesOfClass(HiraishinMarkEntity.class, new AABB(pos).inflate(1.0D), mark -> mark.isOwnedBy(owner) && mark.matchesBlock(pos)).stream().findFirst().orElse(null);
        if (old != null) old.discard();
        else level.addFreshEntity(new HiraishinMarkEntity(level, owner, pos, event.getFace()));
    }

    @SubscribeEvent
    public static void onRightClickEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleEntityRightClick(event, event.getTarget());
    }

    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        handleEntityRightClick(event, event.getTarget());
    }

    private static void handleEntityRightClick(PlayerInteractEvent event, Entity target) {
        Player player = event.getEntity();
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || !(player.getMainHandItem().getItem() instanceof HiraishinKunaiItem) || !player.isShiftKeyDown()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide || !(event.getLevel() instanceof ServerLevel level) || !(player instanceof ServerPlayer)) return;

        // Forge may post both EntityInteractSpecific and EntityInteract for the same click.
        // Apply the state change at most once per server tick.
        Integer previousTick = LAST_ENTITY_MARK_TICK.put(player.getUUID(), player.tickCount);
        if (previousTick != null && previousTick == player.tickCount) return;

        if (target instanceof HiraishinMarkEntity mark) {
            if (mark.isOwnedBy(player.getUUID())) mark.discard();
            return;
        }
        if (target instanceof HiraishinProjectile projectile) {
            if (projectile.isOwnedBy(player.getUUID()) && projectile.isStuck()) {
                projectile.returnItemTo(player);
                projectile.discard();
            }
            return;
        }
        if (!(target instanceof LivingEntity living) || !living.isAlive() || living == player) return;

        UUID owner = player.getUUID();
        HiraishinMarkEntity old = level.getEntitiesOfClass(HiraishinMarkEntity.class, living.getBoundingBox().inflate(1.5D), mark -> mark.isOwnedBy(owner) && mark.matchesTarget(living.getUUID())).stream().findFirst().orElse(null);
        if (old != null) old.discard();
        else {
            Vec3 toPlayer = player.position().subtract(living.position());
            Vec3 forward = living.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
            boolean front = toPlayer.x * forward.x + toPlayer.z * forward.z >= 0.0D;
            level.addFreshEntity(new HiraishinMarkEntity(level, owner, living, front));
        }
    }

    @SubscribeEvent
    public static void onShiftRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || !(player.getMainHandItem().getItem() instanceof HiraishinKunaiItem) || !player.isShiftKeyDown()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide || !(event.getLevel() instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) return;
        // Shift-right-click in the air can still mark a living entity in short reach.
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(6.0D));
        AABB search = player.getBoundingBox().expandTowards(player.getLookAngle().scale(6.0D)).inflate(1.0D);
        Entity best = null;
        double bestDistance = 36.0D;
        for (Entity candidate : level.getEntities(player, search, e -> e instanceof LivingEntity && e != player && e.isPickable())) {
            AABB box = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            var intercept = box.clip(start, end);
            if (intercept.isPresent()) {
                double d = start.distanceToSqr(intercept.get());
                if (d < bestDistance) { bestDistance = d; best = candidate; }
            }
        }
        if (best instanceof LivingEntity living) {
            UUID owner = player.getUUID();
            HiraishinMarkEntity old = level.getEntitiesOfClass(HiraishinMarkEntity.class, living.getBoundingBox().inflate(1.5D), mark -> mark.isOwnedBy(owner) && mark.matchesTarget(living.getUUID())).stream().findFirst().orElse(null);
            if (old != null) old.discard();
            else {
                Vec3 toPlayer = player.position().subtract(living.position());
                Vec3 forward = living.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
                boolean front = toPlayer.x * forward.x + toPlayer.z * forward.z >= 0.0D;
                level.addFreshEntity(new HiraishinMarkEntity(level, owner, living, front));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        UUID dead = event.getEntity().getUUID();
        for (HiraishinMarkEntity mark : level.getEntitiesOfClass(HiraishinMarkEntity.class, event.getEntity().getBoundingBox().inflate(4.0D), entity -> entity.matchesTarget(dead))) {
            mark.discard();
        }
    }
}
