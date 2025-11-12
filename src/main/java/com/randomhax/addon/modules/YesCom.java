package com.randomhax.addon.modules;

import com.randomhax.addon.RandomHax;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class YesCom extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> announce = sgGeneral.add(new BoolSetting.Builder()
        .name("announce")
        .description("Chat message when a player enters visual range.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> tickInterval = sgGeneral.add(new IntSetting.Builder()
        .name("scan-interval-ticks")
        .description("How often to scan for players in visual range.")
        .defaultValue(10).min(1).sliderRange(1, 40)
        .build()
    );

    private int tick;
    private final Set<UUID> seenNow = new HashSet<>();

    // ===== Shared storage (read by the command) =====
    // key = lowercased username
    public static final class LastPos {
        public final String name;
        public final BlockPos pos;
        public final String dim; // "overworld", "nether", "end" (client name)
        public final long timeMs;
        public LastPos(String name, BlockPos pos, String dim, long timeMs) {
            this.name = name; this.pos = pos; this.dim = dim; this.timeMs = timeMs;
        }
    }
    public static final Map<String, LastPos> LAST_SEEN = new ConcurrentHashMap<>();

    public YesCom() {
        super(RandomHax.CATEGORY, "yescom", "Logs players entering visual range and remembers last seen coords.");
    }

    @Override
    public void onActivate() {
        tick = 0;
        seenNow.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post e) {
        tick++;
        if (tick % tickInterval.get() != 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // collect current in-range players
        List<AbstractClientPlayerEntity> players = mc.world.getPlayers();
        Set<UUID> current = new HashSet<>();

        for (AbstractClientPlayerEntity p : players) {
            if (p == mc.player) continue; // skip self
            current.add(p.getUuid());

            boolean firstTimeSeen = !seenNow.contains(p.getUuid());
            if (firstTimeSeen) {
                // entering visual range now
                String name = p.getGameProfile().getName();
                BlockPos pos = p.getBlockPos();

                String dim = "unknown";
                if (mc.world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) dim = "overworld";
                else if (mc.world.getRegistryKey() == net.minecraft.world.World.NETHER) dim = "nether";
                else if (mc.world.getRegistryKey() == net.minecraft.world.World.END) dim = "end";

                LAST_SEEN.put(name.toLowerCase(Locale.ROOT),
                    new LastPos(name, pos, dim, System.currentTimeMillis()));

                if (announce.get()) {
                    info(Text.of(String.format(
                        "[YesCom] %s entered visual range at %d %d %d (%s)",
                        name, pos.getX(), pos.getY(), pos.getZ(), dim
                    )));
                }
            }
        }

        // refresh the set of "currently visible"
        seenNow.clear();
        seenNow.addAll(current);
    }

    // Ensure visibility matches Module.info(Text) (public), not private.
    @Override
    public void info(Text text) {
        super.info(text);
    }
}
