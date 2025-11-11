package com.randomhax.addon.modules;

import com.randomhax.addon.RandomHax;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

public class PathMacro extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    public final Setting<Integer> distance = sg.add(new IntSetting.Builder()
        .name("distance")
        .description("Blocks to travel with #thisway.")
        .defaultValue(800)
        .min(1)
        .sliderRange(50, 5000)
        .build()
    );

    public final Setting<Keybind> hotkey = sg.add(new KeybindSetting.Builder()
        .name("trigger-key")
        .description("Press to start (#thisway + #elytra), press again to #stop.")
        .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_P))
        .build()
    );

    // Small delay to ensure Baritone parses #thisway before #elytra gets sent
    public final Setting<Integer> elytraDelayTicks = sg.add(new IntSetting.Builder()
        .name("elytra-delay-ticks")
        .description("Ticks to wait after #thisway before sending #elytra.")
        .defaultValue(3)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private boolean wasPressed = false;
    private boolean running = false;
    private int elytraCountdown = 0;

    public PathMacro() {
        super(RandomHax.CATEGORY, "path-macro",
            "Hotkey: start with '#thisway <distance>' then '#elytra'; press again to '#stop'.");
    }

    @EventHandler
    private void onTick(TickEvent.Post e) {
        if (mc == null || mc.getNetworkHandler() == null) return;

        boolean pressed = hotkey.get().isPressed();

        // Edge-trigger the hotkey
        if (pressed && !wasPressed) {
            if (!running) {
                // Start: #thisway then schedule #elytra
                mc.getNetworkHandler().sendChatMessage("#thisway " + distance.get());
                elytraCountdown = elytraDelayTicks.get();
                running = true;
            } else {
                // Stop
                mc.getNetworkHandler().sendChatMessage("#stop");
                running = false;
                elytraCountdown = 0;
            }
        }
        wasPressed = pressed;

        // After starting, wait a few ticks then send #elytra
        if (running && elytraCountdown > 0) {
            elytraCountdown--;
            if (elytraCountdown == 0) {
                mc.getNetworkHandler().sendChatMessage("#elytra");
            }
        }
    }

    @Override
    public void onDeactivate() {
        running = false;
        elytraCountdown = 0;
        wasPressed = false;
        super.onDeactivate();
    }
}
