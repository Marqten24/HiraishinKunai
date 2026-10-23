package com.marques.hiraishinkunai.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.marques.hiraishinkunai.HiraishinKunai;
import com.marques.hiraishinkunai.entity.HiraishinMarkEntity;
import com.marques.hiraishinkunai.entity.HiraishinProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class HiraishinIndicatorRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(HiraishinKunai.MOD_ID, "textures/gui/hiraishin_kunai.png");
    private static final float UNIT_HALF_SIZE = 1.0F;

    public HiraishinIndicatorRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        Vec3 indicator = indicatorPosition(entity, partialTick);
        float alpha = entity instanceof HiraishinMarkEntity mark ? mark.getAppearanceAlpha(partialTick) : 1.0F;
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
        Minecraft minecraft = Minecraft.getInstance();
        double cameraDistance = minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(indicator);
        double safeDistance = Math.max(0.5D, cameraDistance);
        double focal = minecraft.getWindow().getGuiScaledHeight() /
                (2.0D * Math.tan(Math.toRadians(minecraft.options.fov().get().doubleValue()) / 2.0D));
        float halfWorldSize = (float) (12.0D * safeDistance / focal); // 24 GUI pixels regardless of depth.
        float textScale = (float) (safeDistance / focal);            // Text remains the same apparent size too.
        double textDrop = 20.0D * safeDistance / focal;

        poseStack.pushPose();
        poseStack.translate(0.0D, entity instanceof HiraishinMarkEntity ? 0.34D : 0.0D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(halfWorldSize, halfWorldSize, halfWorldSize);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        quad(consumer, matrix, poseStack.last().normal(), -UNIT_HALF_SIZE, -UNIT_HALF_SIZE, UNIT_HALF_SIZE, UNIT_HALF_SIZE, a, packedLight);
        poseStack.popPose();

        if (minecraft.player != null) {
            double distance = minecraft.player.position().distanceTo(indicator);
            Component label = Component.literal(Math.round(distance) + " m");
            poseStack.pushPose();
            poseStack.translate(0.0D, entity instanceof HiraishinMarkEntity ? 0.34D : 0.0D, 0.0D);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.translate(0.0D, -textDrop, 0.0D);
            poseStack.scale(-textScale, -textScale, textScale);
            float width = minecraft.font.width(label);
            minecraft.font.drawInBatch(label, -width / 2.0F, 0.0F, 0xFFFFFFFF, false,
                    poseStack.last().pose(), buffers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private static Vec3 indicatorPosition(Entity entity, float partialTick) {
        if (entity instanceof HiraishinMarkEntity mark) return mark.getIndicatorPosition(partialTick);
        if (entity instanceof HiraishinProjectile projectile) return projectile.getIndicatorPosition(partialTick);
        return entity.position();
    }

    private static void quad(VertexConsumer out, Matrix4f m, Matrix3f normal, float left, float bottom, float right, float top, int alpha, int light) {
        out.vertex(m, left, bottom, 0.0F).color(255, 255, 255, alpha).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        out.vertex(m, right, bottom, 0.0F).color(255, 255, 255, alpha).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        out.vertex(m, right, top, 0.0F).color(255, 255, 255, alpha).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        out.vertex(m, left, top, 0.0F).color(255, 255, 255, alpha).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
    }

    @Override public ResourceLocation getTextureLocation(T entity) { return TEXTURE; }
}
