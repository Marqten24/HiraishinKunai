package com.marques.hiraishinkunai.client;

import com.marques.hiraishinkunai.HiraishinKunai;
import com.marques.hiraishinkunai.entity.HiraishinMarkEntity;
import com.marques.hiraishinkunai.entity.HiraishinProjectile;
import com.marques.hiraishinkunai.network.HiraishinNetwork;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = HiraishinKunai.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HiraishinClientEvents {
    private HiraishinClientEvents() {}

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || event.getAction() != GLFW.GLFW_PRESS) return;
        boolean left = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        boolean right = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && mc.player.isShiftKeyDown();
        if (!left && !right) return;

        Entity selected = findIndicatorAtCrosshair(mc);
        if (selected == null) return;
        if (left) HiraishinNetwork.requestTeleport(selected.getId());
        else HiraishinNetwork.requestRemove(selected.getId());
        event.setCanceled(true);
    }

    private static Entity findIndicatorAtCrosshair(Minecraft mc) {
        Camera camera = mc.gameRenderer.getMainCamera();
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();
        // The game captures the mouse in-world; use the crosshair center to target billboards.
        double mouseX = guiWidth / 2.0D;
        double mouseY = guiHeight / 2.0D;
        float fov = mc.options.fov().get().floatValue();
        double focal = guiHeight / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        Quaternionf inverseRotation = new Quaternionf(camera.rotation()).conjugate();
        Vec3 cameraPos = camera.getPosition();

        Entity best = null;
        double bestDepth = Double.MAX_VALUE;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof HiraishinMarkEntity || entity instanceof HiraishinProjectile)) continue;
            boolean owned = entity instanceof HiraishinMarkEntity mark ? mark.isOwnedBy(mc.player.getUUID())
                    : ((HiraishinProjectile) entity).isOwnedBy(mc.player.getUUID());
            if (!owned) continue;

            Vec3 world = entity instanceof HiraishinMarkEntity mark ? mark.getIndicatorPosition(mc.getFrameTime())
                    : ((HiraishinProjectile) entity).getIndicatorPosition(mc.getFrameTime());
            Vec3 rel = world.subtract(cameraPos);
            Vector3f local = new Vector3f((float) rel.x, (float) rel.y, (float) rel.z).rotate(inverseRotation);
            if (local.z >= -0.02F) continue;
            double depth = -local.z;
            double screenX = guiWidth / 2.0D + local.x * focal / depth;
            double screenY = guiHeight / 2.0D - local.y * focal / depth;
            double halfWidth = 14.0D;
            double halfHeight = 25.0D;
            if (Math.abs(mouseX - screenX) <= halfWidth && Math.abs(mouseY - screenY) <= halfHeight && depth < bestDepth) {
                best = entity;
                bestDepth = depth;
            }
        }
        return best;
    }
}
