package com.randomhax.addon.modules;

import com.randomhax.addon.RandomHax;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AdBlocker extends Module {
    public AdBlocker() {
        super(RandomHax.CATEGORY, "AdBlocker", "Blocks advertisers in chat.");
    }

    public enum IgnoreStyle { None, Ignore, HardIgnore }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<IgnoreStyle> ignoreStyle = sgGeneral.add(new EnumSetting.Builder<IgnoreStyle>()
        .name("ignore-advertisers")
        .description("Whether to ignore accounts which trigger the blocked patterns filter.")
        .defaultValue(IgnoreStyle.Ignore)
        .build()
    );

    private final Setting<List<String>> patterns = sgGeneral.add(new StringListSetting.Builder()
        .name("blocked-patterns")
        .description("Chat messages matching any of these patterns will be blocked, and ignore preferences applied.")
        .defaultValue(List.of(
            "thishttp", "discord.com", "discord.gg", "gg/", "com/", "/invite/", "% off",
            ".store", "cheapest price", "cheapest kit", "cheap price", "cheap kit",
            "use code", "at checkout", "join now", "rusherhack.org", "nox2b", ".shop"
        ))
        .build()
    );

    private final Setting<Boolean> feedback = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-feedback")
        .description("Show a confirmation when an ignore command is sent.")
        .defaultValue(true)
        .build()
    );

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof GameMessageS2CPacket pkt)) return;
        if (mc == null || mc.world == null || mc.getNetworkHandler() == null) return;

        Text contentText = pkt.content();
        if (contentText == null) return;

        String content = contentText.getString();
        if (content == null || content.isBlank()) return;

        if (!matchesAnyPattern(content, patterns.get())) return;

        event.cancel();

        if (ignoreStyle.get() == IgnoreStyle.None) return;

        boolean hard = ignoreStyle.get() == IgnoreStyle.HardIgnore;

        String name = getNameFromMessage(content);
        if (!name.isBlank()) {
            sendIgnore(name, hard);
            return;
        }

        List<String> culprits = new ArrayList<>();
        extractNamesFromDeathMessage(contentText, culprits);
        for (String culprit : culprits) {
            if (!culprit.isBlank()) sendIgnore(culprit, hard);
        }
    }

    private boolean matchesAnyPattern(String msg, List<String> pats) {
        String lower = msg.toLowerCase();
        for (String p : pats) {
            if (lower.contains(p.toLowerCase())) return true;
        }
        return false;
    }

    private void sendIgnore(String name, boolean hard) {
        ClientPlayNetworkHandler nh = mc.getNetworkHandler();
        if (nh == null) return;

        String cmd = (hard ? "ignorehard " : "ignore ") + name;
        nh.sendChatCommand(cmd);

        if (feedback.get()) {
            ChatUtils.info("(AdBlocker) " + (hard ? "Hard-ignored" : "Ignored") + " \"§c" + name + "§7\".");
        }
    }

    private String getNameFromMessage(String message) {
        try {
            String[] parts = message.split(" ");
            if (parts.length >= 3 && parts[1].equalsIgnoreCase("whispers:")) {
                return parts[0].trim();
            }
            if (parts.length >= 1 && parts[0].startsWith("<") && parts[0].endsWith(">")) {
                return parts[0].substring(1, parts[0].length() - 1).trim();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private void extractNamesFromDeathMessage(Text msg, List<String> names) {
        if (msg == null) return;

        HoverEvent hover = msg.getStyle().getHoverEvent();
        if (hover != null && hover.getAction() == HoverEvent.Action.SHOW_TEXT) {
            Object payload = getHoverShowTextPayload(hover);
            if (payload instanceof Text t) {
                String s = t.getString();
                if (s.startsWith("Message ")) {
                    String n = s.substring("Message ".length()).trim();
                    if (!n.isBlank()) names.add(n);
                }
            }
        }

        for (Text sib : msg.getSiblings()) {
            extractNamesFromDeathMessage(sib, names);
        }
    }

    private Object getHoverShowTextPayload(HoverEvent hover) {
        try {
            Method m = HoverEvent.class.getMethod("getValue", HoverEvent.Action.class);
            return m.invoke(hover, HoverEvent.Action.SHOW_TEXT);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
        }
        try {
            Method m = HoverEvent.class.getMethod("value", HoverEvent.Action.class);
            return m.invoke(hover, HoverEvent.Action.SHOW_TEXT);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
        }
        try {
            Field f = HoverEvent.class.getDeclaredField("value");
            f.setAccessible(true);
            return f.get(hover);
        } catch (NoSuchFieldException ignored) {
        } catch (Throwable t) {
        }
        try {
            Field f = HoverEvent.class.getDeclaredField("contents");
            f.setAccessible(true);
            return f.get(hover);
        } catch (Throwable ignored) {
        }
        return null;
    }
}
