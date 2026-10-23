package com.marques.hiraishinkunai.client;

import com.marques.hiraishinkunai.HiraishinKunai;
import com.marques.hiraishinkunai.entity.HiraishinMarkEntity;
import com.marques.hiraishinkunai.entity.HiraishinProjectile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HiraishinKunai.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HiraishinClientModEvents {
    private HiraishinClientModEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HiraishinKunai.KUNAI_PROJECTILE.get(), HiraishinIndicatorRenderer::new);
        event.registerEntityRenderer(HiraishinKunai.KUNAI_MARK.get(), HiraishinIndicatorRenderer::new);
    }
}
