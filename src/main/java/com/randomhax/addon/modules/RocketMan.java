package com.randomhax.addon.modules;

import com.randomhax.addon.RandomHax;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class RocketMan extends Module {
    public enum RocketMode { OnKey, Static, Dynamic, Speed }

    private final SettingGroup sgRockets = settings.createGroup("Rocket Usage");

    public final Setting<RocketMode> usageMode = sgRockets.add(new EnumSetting.Builder<RocketMode>()
        .name("mode").defaultValue(RocketMode.OnKey).build());

    public final Setting<Keybind> usageKey = sgRockets.add(new KeybindSetting.Builder()
        .name("rocket-key").defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_R))
        .visible(() -> usageMode.get() == RocketMode.OnKey).build());

    private final Setting<Integer> usageCooldown = sgRockets.add(new IntSetting.Builder()
        .name("cooldown-ticks").defaultValue(40).min(1).sliderRange(2, 120)
        .visible(() -> usageMode.get() == RocketMode.OnKey || usageMode.get() == RocketMode.Speed).build());

    private final Setting<Integer> usageTickRate = sgRockets.add(new IntSetting.Builder()
        .name("static-interval").defaultValue(100).min(1).sliderRange(10, 400)
        .visible(() -> usageMode.get() == RocketMode.Static).build());

    private final Setting<Double> usageSpeed = sgRockets.add(new DoubleSetting.Builder()
        .name("speed-threshold-bps").defaultValue(24.0).min(1.0).sliderRange(2.0, 100.0)
        .visible(() -> usageMode.get() == RocketMode.Speed).build());

    private final Setting<Boolean> disableOnLand = settings.getDefaultGroup().add(new BoolSetting.Builder()
        .name("disable-on-land").defaultValue(true).build());

    private final Setting<Boolean> autoEquip = settings.getDefaultGroup().add(new BoolSetting.Builder()
        .name("auto-equip-elytra").defaultValue(false).build());

    private int timer = 0, sinceUse = 0; private boolean justUsed = false;

    public RocketMan() {
        super(RandomHax.CATEGORY, "rocket-man", "Elytra flight assist using firework rockets.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        if (autoEquip.get() && !mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            for (int i = 0; i < 36; i++) {
                ItemStack s = mc.player.getInventory().getStack(i);
                if (s.isOf(Items.ELYTRA)) { InvUtils.move().from(i).toArmor(2); break; }
            }
        }
        timer = sinceUse = 0; justUsed = false;
    }

    @Override
    public void onDeactivate() {
        timer = sinceUse = 0; justUsed = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre e) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (!mc.player.isGliding()) {
            if (mc.player.isOnGround() && disableOnLand.get()) toggle();
            timer = sinceUse = 0; justUsed = false; return;
        }

        timer++; if (justUsed) sinceUse++;

        switch (usageMode.get()) {
            case OnKey -> {
                if (usageKey.get().isPressed() && canUse()) tryUseRocket();
            }
            case Static -> {
                if (timer >= usageTickRate.get()) { timer = 0; tryUseRocket(); }
            }
            case Dynamic -> {
                if (!hasActiveRocket() && canUse()) tryUseRocket();
            }
            case Speed -> {
                double bps = Utils.getPlayerSpeed().length();
                if (bps <= usageSpeed.get() && canUse()) tryUseRocket();
            }
        }
    }

    private boolean canUse() { return !justUsed || sinceUse >= usageCooldown.get(); }

    private boolean hasActiveRocket() {
        for (var e : mc.world.getEntities()) {
            if (e instanceof FireworkRocketEntity r && r.getOwner() != null && r.getOwner().equals(mc.player)) return true;
        }
        return false;
    }

    private void tryUseRocket() {
        if (useFireworkFromHotbar() || useFireworkFromMainToHotbar0()) { justUsed = true; sinceUse = 0; }
    }

    private boolean useFireworkFromHotbar() {
        for (int slot = 0; slot < 9; slot++) {
            Item item = mc.player.getInventory().getStack(slot).getItem();
            if (item == Items.FIREWORK_ROCKET) {
                InvUtils.swap(slot, true);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                InvUtils.swapBack();
                return true;
            }
        }
        return false;
    }

    private boolean useFireworkFromMainToHotbar0() {
        int source = -1;
        for (int i = 9; i < 36; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (item == Items.FIREWORK_ROCKET) { source = i; break; }
        }
        if (source == -1) return false;

        InvUtils.move().from(source).toHotbar(0);
        InvUtils.swap(0, true);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        InvUtils.swapBack();
        InvUtils.move().fromHotbar(0).to(source);
        return true;
    }
}
