package com.randomhax.addon.modules;

import com.randomhax.addon.RandomHax;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AFKVanillaFly extends Module {
    private long lastRocketUse = 0;
    private boolean launched = false;
    private double yTarget = -1;
    private float targetPitch = 0;

    public AFKVanillaFly() {
        super(RandomHax.CATEGORY, "AFKVanillaFly", "Maintains a level Y-flight with fireworks and smooth pitch control.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> fireworkDelay = sgGeneral.add(new IntSetting.Builder()
        .name("timed-delay")
        .description("Delay between firework usages in milliseconds.")
        .defaultValue(4000)
        .sliderRange(0, 10000)
        .build()
    );

    private final Setting<Boolean> useManualY = sgGeneral.add(new BoolSetting.Builder()
        .name("use-manual-y-level")
        .description("Use a manually set Y level instead of the Y level when activated.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> manualYLevel = sgGeneral.add(new IntSetting.Builder()
        .name("manual-y-level")
        .description("The Y level to maintain when using manual Y level.")
        .defaultValue(256)
        .sliderRange(-64, 320)
        .visible(useManualY::get)
        .onChanged(val -> yTarget = val)
        .build()
    );

    @Override
    public void onActivate() {
        launched = false;
        yTarget = -1;

        if (mc.player == null || !isElytraFlightActive()) {
            info("You must already be gliding before enabling AFKVanillaFly.");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        tickFlyLogic();
    }

    public void tickFlyLogic() {
        if (mc.player == null) return;

        double currentY = mc.player.getY();

        if (isElytraFlightActive()) {
            if (yTarget == -1 || !launched) {
                yTarget = useManualY.get() ? manualYLevel.get() : currentY;
                launched = true;
            }

            if (!useManualY.get()) {
                double yDiffFromLock = currentY - yTarget;
                if (Math.abs(yDiffFromLock) > 10.0) {
                    yTarget = currentY;
                    info("Y-lock reset due to altitude deviation.");
                }
            }

            double yDiff = currentY - yTarget;

            if (Math.abs(yDiff) > 10.0) {
                targetPitch = (float) (Math.atan2(yDiff, 100) * (180 / Math.PI));
            } else if (yDiff > 2.0) {
                targetPitch = 10f;
            } else if (yDiff < -2.0) {
                targetPitch = -10f;
            } else {
                targetPitch = 0f;
            }

            float currentPitch = mc.player.getPitch();
            float pitchDiff = targetPitch - currentPitch;
            mc.player.setPitch(currentPitch + pitchDiff * 0.1f);

            if (System.currentTimeMillis() - lastRocketUse > fireworkDelay.get()) {
                tryUseFirework();
            }
        } else {
            if (!launched) {
                mc.player.jump();
                launched = true;
            } else if (System.currentTimeMillis() - lastRocketUse > 1000) {
                tryUseFirework();
            }
            yTarget = -1;
        }
    }

    public void resetYLock() {
        yTarget = -1;
        launched = false;
    }

    // -------- helpers --------

    private void tryUseFirework() {
        if (mc.player == null || mc.interactionManager == null) return;

        // Ensure a firework is in hotbar; move one if needed.
        FindItemResult hotbar = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!hotbar.found()) {
            FindItemResult inv = InvUtils.find(Items.FIREWORK_ROCKET);
            if (inv.found()) {
                int hotbarSlot = findEmptyHotbarSlot();
                if (hotbarSlot != -1) {
                    InvUtils.move().from(inv.slot()).to(hotbarSlot);
                } else {
                    info("No empty hotbar slot for fireworks.");
                    return;
                }
            } else {
                info("No fireworks found.");
                return;
            }
        }

        // Swap to rocket, use, then swap back (avoids accessing selectedSlot).
        FindItemResult rocket = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (rocket.found()) {
            InvUtils.swap(rocket.slot(), true);  // mark to swap back
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            InvUtils.swapBack();
            lastRocketUse = System.currentTimeMillis();
        }
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    /**
     * Version-proof check across 1.21.x (handles isFallFlying/isGliding/pose name)
     */
    private boolean isElytraFlightActive() {
        if (mc == null || mc.player == null) return false;

        // Try 1: isFallFlying()
        try {
            var m = mc.player.getClass().getMethod("isFallFlying");
            Object r = m.invoke(mc.player);
            if (r instanceof Boolean b) return b;
        } catch (ReflectiveOperationException ignored) {}

        // Try 2: isGliding()
        try {
            var m = mc.player.getClass().getMethod("isGliding");
            Object r = m.invoke(mc.player);
            if (r instanceof Boolean b) return b;
        } catch (ReflectiveOperationException ignored) {}

        // Try 3: pose enum name
        try {
            String poseName = String.valueOf(mc.player.getPose());
            if ("FALL_FLYING".equalsIgnoreCase(poseName)) return true;
        } catch (Throwable ignored) {}

        // Heuristic fallback
        return mc.player.getAbilities() != null
            && !mc.player.getAbilities().flying
            && mc.player.fallDistance > 1.0f
            && mc.player.getVelocity().y < -0.08;
    }
}
