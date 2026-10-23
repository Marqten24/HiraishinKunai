package com.marques.hiraishinkunai.network;

import com.marques.hiraishinkunai.HiraishinKunai;
import com.marques.hiraishinkunai.entity.HiraishinMarkEntity;
import com.marques.hiraishinkunai.entity.HiraishinProjectile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.function.Supplier;

public final class HiraishinNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(HiraishinKunai.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();
    private static boolean registered;

    private HiraishinNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, TeleportPacket.class, TeleportPacket::encode, TeleportPacket::decode, TeleportPacket::handle);
        CHANNEL.registerMessage(1, RemovePacket.class, RemovePacket::encode, RemovePacket::decode, RemovePacket::handle);
    }

    public static void requestTeleport(int entityId) { CHANNEL.sendToServer(new TeleportPacket(entityId)); }
    public static void requestRemove(int entityId) { CHANNEL.sendToServer(new RemovePacket(entityId)); }

    private static void teleportTo(ServerPlayer player, Vec3 desired) {
        ServerLevel level = player.serverLevel();
        Vec3 safe = findSafePosition(level, player, desired);
        player.teleportTo(level, safe.x, safe.y, safe.z, Set.of(), player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
    }

    private static Vec3 findSafePosition(ServerLevel level, ServerPlayer player, Vec3 desired) {
        double[][] offsets = {{0,0,0},{0,1,0},{0,2,0},{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{1,1,0},{-1,1,0},{0,1,1},{0,1,-1},{1,0,1},{-1,0,-1},{1,0,-1},{-1,0,1}};
        for (double[] offset : offsets) {
            Vec3 candidate = desired.add(offset[0], offset[1], offset[2]);
            AABB moved = player.getBoundingBox().move(candidate.subtract(player.position()));
            if (level.noCollision(player, moved)) return candidate;
        }
        return desired;
    }

    private static void giveKunai(ServerPlayer player) {
        ItemStack stack = new ItemStack(HiraishinKunai.HIRAISHIN_KUNAI.get());
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private record TeleportPacket(int entityId) {
        static void encode(TeleportPacket msg, FriendlyByteBuf buf) { buf.writeVarInt(msg.entityId); }
        static TeleportPacket decode(FriendlyByteBuf buf) { return new TeleportPacket(buf.readVarInt()); }
        static void handle(TeleportPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null || !player.isAlive()) return;
                Entity entity = player.serverLevel().getEntity(msg.entityId);
                if (entity == null || entity.level() != player.level() || !entity.isAlive()) return;

                if (entity instanceof HiraishinProjectile projectile && projectile.isOwnedBy(player.getUUID())) {
                    if (projectile.getLaunchOrigin().distanceTo(projectile.position()) > projectile.getMaxRange() + 2.0D) return;
                    Vec3 destination = projectile.position().add(0.0D, 0.12D, 0.0D);
                    teleportTo(player, destination);
                    projectile.returnItemTo(player);
                    projectile.discard();
                } else if (entity instanceof HiraishinMarkEntity mark && mark.isOwnedBy(player.getUUID())) {
                    teleportTo(player, mark.getTeleportPosition());
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    private record RemovePacket(int entityId) {
        static void encode(RemovePacket msg, FriendlyByteBuf buf) { buf.writeVarInt(msg.entityId); }
        static RemovePacket decode(FriendlyByteBuf buf) { return new RemovePacket(buf.readVarInt()); }
        static void handle(RemovePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;
                Entity entity = player.serverLevel().getEntity(msg.entityId);
                if (entity instanceof HiraishinMarkEntity mark && mark.isOwnedBy(player.getUUID())) {
                    mark.discard();
                } else if (entity instanceof HiraishinProjectile projectile && projectile.isOwnedBy(player.getUUID()) && projectile.isStuck()) {
                    projectile.returnItemTo(player);
                    projectile.discard();
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
