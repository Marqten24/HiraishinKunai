package com.marques.hiraishinkunai;

import com.marques.hiraishinkunai.entity.HiraishinMarkEntity;
import com.marques.hiraishinkunai.entity.HiraishinProjectile;
import com.marques.hiraishinkunai.item.HiraishinKunaiItem;
import com.marques.hiraishinkunai.network.HiraishinNetwork;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(HiraishinKunai.MOD_ID)
public final class HiraishinKunai {
    public static final String MOD_ID = "hiraishinkunai";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<Item> HIRAISHIN_KUNAI = ITEMS.register(
            "hiraishin_kunai", () -> new HiraishinKunaiItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<EntityType<HiraishinProjectile>> KUNAI_PROJECTILE = ENTITY_TYPES.register(
            "kunai_projectile", () -> EntityType.Builder.<HiraishinProjectile>of(HiraishinProjectile::new, MobCategory.MISC)
                    .sized(0.18F, 0.18F).clientTrackingRange(96).updateInterval(1).build("kunai_projectile"));

    public static final RegistryObject<EntityType<HiraishinMarkEntity>> KUNAI_MARK = ENTITY_TYPES.register(
            "kunai_mark", () -> EntityType.Builder.<HiraishinMarkEntity>of(HiraishinMarkEntity::new, MobCategory.MISC)
                    .sized(0.22F, 0.22F).clientTrackingRange(96).updateInterval(1).build("kunai_mark"));

    public static final RegistryObject<CreativeModeTab> HIRAISHIN_TAB = CREATIVE_TABS.register("hiraishin_kunai",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hiraishinkunai"))
                    .icon(() -> new ItemStack(HIRAISHIN_KUNAI.get()))
                    .displayItems((parameters, output) -> output.accept(HIRAISHIN_KUNAI.get()))
                    .build());

    public HiraishinKunai() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        ENTITY_TYPES.register(bus);
        CREATIVE_TABS.register(bus);
        HiraishinNetwork.register();
    }
}
