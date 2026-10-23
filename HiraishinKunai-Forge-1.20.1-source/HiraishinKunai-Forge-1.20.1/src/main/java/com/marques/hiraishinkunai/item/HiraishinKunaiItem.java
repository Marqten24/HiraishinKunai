package com.marques.hiraishinkunai.item;

import com.marques.hiraishinkunai.HiraishinKunai;
import com.marques.hiraishinkunai.entity.HiraishinProjectile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The single Hiraishin Kunai item. Normal right-click charges a throw;
 * Shift + right-click is handled by HiraishinEvents and places/removes marks.
 */
public final class HiraishinKunaiItem extends Item {
    public HiraishinKunaiItem(Properties properties) { super(properties); }

    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            // Sneak-use is reserved for the mark-placement interaction.
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!(living instanceof Player player) || level.isClientSide || player.isShiftKeyDown()) return;

        int chargeTicks = getUseDuration(stack) - timeLeft;
        float charge = Math.min(1.0F, chargeTicks / 20.0F);
        float power = (charge * charge + 2.0F * charge) / 3.0F;
        if (power < 0.08F) return;

        double maxRange = 4.0D + 36.0D * power;
        float velocity = 0.55F + 2.55F * power;
        HiraishinProjectile projectile = new HiraishinProjectile(level, player, maxRange, !player.getAbilities().instabuild);
        projectile.setPos(player.getX(), player.getEyeY() - 0.12D, player.getZ());
        projectile.setLaunchOrigin(projectile.position());
        Vec3 look = player.getLookAngle();
        projectile.shoot(look.x, look.y, look.z, velocity, 0.0F);
        level.addFreshEntity(projectile);

        if (!player.getAbilities().instabuild) stack.shrink(1);
    }
}
