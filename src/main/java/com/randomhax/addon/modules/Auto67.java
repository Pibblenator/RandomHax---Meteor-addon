package com.randomhax.addon.modules;

import com.randomhax.addon.RandomHax;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.*;

public class Auto67 extends Module {
    private final SettingGroup sgGeneral   = settings.getDefaultGroup();
    private final SettingGroup sgBehavior  = settings.createGroup("Behavior");
    private final SettingGroup sgRateLimit = settings.createGroup("Rate Limit");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Distance (in blocks) to trigger a whisper.")
        .defaultValue(10.0)
        .min(1.0)
        .sliderRange(3.0, 30.0)
        .build()
    );

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
        .name("message")
        .description("Message to whisper. Use {name} and {dist} placeholders.")
        .defaultValue("yo {name} o7 — you’re {dist}m away")
        .build()
    );

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
        .name("whisper-command")
        .description("Command verb to use for whispering (no slash). Examples: msg, w, tell, m.")
        .defaultValue("msg")
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgBehavior.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Don’t whisper to players marked as friends.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyOncePerPlayer = sgBehavior.add(new BoolSetting.Builder()
        .name("only-once-per-player")
        .description("Whisper each player at most once until you toggle the module off/on.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> triggerOnEntryOnly = sgBehavior.add(new BoolSetting.Builder()
        .name("on-entry-only")
        .description("Only whisper when a player ENTERS the radius (not every tick while inside).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> perPlayerCooldown = sgRateLimit.add(new IntSetting.Builder()
        .name("per-player-cooldown-ms")
        .description("Minimum time between whispers to the same player.")
        .defaultValue(30000)
        .min(0)
        .sliderRange(0, 120000)
        .build()
    );

    private final Setting<Integer> globalCooldown = sgRateLimit.add(new IntSetting.Builder()
        .name("global-cooldown-ms")
        .description("Minimum time between ANY whispers to avoid spam kicks.")
        .defaultValue(1500)
        .min(0)
        .sliderRange(0, 10000)
        .build()
    );

    private final Set<UUID> currentlyInside = new HashSet<>();
    private final Set<UUID> sentEver = new HashSet<>();
    private final Map<UUID, Long> lastSent = new HashMap<>();
    private long lastGlobalSent = 0;

    public Auto67() {
        super(RandomHax.CATEGORY, "auto67", "Automatically whispers nearby players.");
    }

    @Override
    public void onActivate() {
        currentlyInside.clear();
        sentEver.clear();
        lastSent.clear();
        lastGlobalSent = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post e) {
        if (mc.world == null || mc.player == null) return;
        ClientPlayerEntity me = mc.player;

        double r = range.get();
        double r2 = r * r;
        long now = System.currentTimeMillis();

        Set<UUID> newInside = new HashSet<>();

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == me) continue;
            if (p.isSpectator()) continue;
            if (ignoreFriends.get() && Friends.get().isFriend(p)) continue;

            double d2 = me.squaredDistanceTo(p);
            if (d2 <= r2) {
                newInside.add(p.getUuid());

                boolean entry = !currentlyInside.contains(p.getUuid());
                boolean shouldTrigger = triggerOnEntryOnly.get() ? entry : true;

                if (shouldTrigger) {
                    if (onlyOncePerPlayer.get() && sentEver.contains(p.getUuid())) continue;

                    long last = lastSent.getOrDefault(p.getUuid(), 0L);
                    if (now - last < perPlayerCooldown.get()) continue;
                    if (now - lastGlobalSent < globalCooldown.get()) continue;

                    String msg = formatMessage(p, Math.sqrt(d2));
                    String cmdLine = "/" + command.get().trim() + " " + p.getGameProfile().getName() + " " + msg;

                    if (me.networkHandler != null) {
                        me.networkHandler.sendChatMessage(cmdLine);
                        lastSent.put(p.getUuid(), now);
                        lastGlobalSent = now;
                        sentEver.add(p.getUuid());
                    }
                }
            }
        }

        currentlyInside.clear();
        currentlyInside.addAll(newInside);
    }

    private String formatMessage(PlayerEntity target, double dist) {
        String base = message.get();
        String name = target.getGameProfile() != null ? target.getGameProfile().getName() : target.getName().getString();
        String distStr = String.format(Locale.ROOT, "%.1f", dist);
        return base.replace("{name}", name).replace("{dist}", distStr);
    }
}
