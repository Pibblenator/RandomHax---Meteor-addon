package com.randomhax.addon.modules;

import com.randomhax.addon.RandomHax;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

@SuppressWarnings("unchecked")
public class Pitch40Util extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> autoBoundAdjust = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-adjust-bounds")
        .description("Keeps raising bounds so Pitch40 keeps climbing.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> boundGap = sgGeneral.add(new DoubleSetting.Builder()
        .name("bound-gap")
        .description("Distance between upper/lower bounds.")
        .defaultValue(60.0)
        .sliderRange(50.0, 100.0)
        .build()
    );

    public final Setting<Boolean> autoFirework = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-firework")
        .description("Use a rocket if vertical speed is too low.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> velocityThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("velocity-threshold")
        .description("Y velocity must be below this (while going up) to fire a rocket.")
        .defaultValue(-0.05)
        .sliderRange(-0.5, 1.0)
        .visible(autoFirework::get)
        .build()
    );

    public final Setting<Integer> fireworkCooldownTicks = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown-ticks")
        .description("Ticks between rocket uses.")
        .defaultValue(10)
        .sliderRange(0, 100)
        .visible(autoFirework::get)
        .build()
    );

    public Pitch40Util() {
        super(RandomHax.CATEGORY, "Pitch40Util",
            "Keeps Meteor ElytraFly in Pitch40 and auto-updates bounds / rockets.");
    }

    private final ElytraFly elytraFly = Modules.get().get(ElytraFly.class);
    private ElytraFlightModes oldValue;
    private final Setting<ElytraFlightModes> elytraFlyMode =
        (Setting<ElytraFlightModes>) elytraFly.settings.get("mode");

    private int fireworkCooldown = 0;
    private boolean goingUp = true;

    @Override
    public void onActivate() {
        oldValue = elytraFlyMode.get();
        elytraFlyMode.set(ElytraFlightModes.Pitch40);
    }

    @Override
    public void onDeactivate() {
        if (elytraFly.isActive()) elytraFly.toggle();
        elytraFlyMode.set(oldValue);
    }

    private void resetBounds() {
        Setting<Double> upper = (Setting<Double>) elytraFly.settings.get("pitch40-upper-bounds");
        Setting<Double> lower = (Setting<Double>) elytraFly.settings.get("pitch40-lower-bounds");
        double top = mc.player.getY() - 5.0;
        upper.set(top);
        lower.set(top - boundGap.get());
    }

    /** Minimal firework launcher (no external Utils). Returns true if used. */
    private boolean launchFireworkOnce() {
        FindItemResult rockets = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!rockets.found()) return false;

        InvUtils.swap(rockets.slot(), true);
        if (mc.interactionManager != null) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
        InvUtils.swapBack();
        return true;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (elytraFly.isActive()) {
            if (fireworkCooldown > 0) fireworkCooldown--;

            // fell below lower bound far enough -> reset
            double lower = (double) ((Setting<?>) elytraFly.settings.get("pitch40-lower-bounds")).get();
            if (autoBoundAdjust.get() && mc.player.getY() <= lower - 10.0) {
                resetBounds();
                return;
            }

            // pitch -40 means "up"
            if (mc.player.getPitch() == -40.0f) {
                goingUp = true;

                if (autoFirework.get()
                    && mc.player.getVelocity().y < velocityThreshold.get()
                    && mc.player.getY() < (double) ((Setting<?>) elytraFly.settings.get("pitch40-upper-bounds")).get()
                    && fireworkCooldown == 0) {
                    if (launchFireworkOnce()) {
                        fireworkCooldown = fireworkCooldownTicks.get();
                    }
                }
            } else if (autoBoundAdjust.get() && goingUp && mc.player.getVelocity().y <= 0.0) {
                // at apex of climb: lock new bounds
                goingUp = false;
                resetBounds();
            }
        } else {
            // re-enable after queue / world join
            if (!mc.player.getAbilities().allowFlying) {
                elytraFly.toggle();
                resetBounds();
            }
        }
    }
}
