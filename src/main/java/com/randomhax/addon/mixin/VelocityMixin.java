package com.randomhax.addon.mixin;

import com.randomhax.addon.mixin.EntityVelocityUpdateS2CPacketAccessor;
import com.randomhax.addon.mixin.ModuleAccessor;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Shadow; // <-- THIS was missing

/**
 * Velocity+:
 * - Custom horizontal/vertical scaling for your own velocity
 * - Optional cancel of ExplosionS2CPacket
 * - Optional "no-kb while gliding" to ignore KB when elytra-flying
 */
@Mixin(value = Velocity.class, remap = false)
public abstract class VelocityMixin {
    // Respect Meteor's existing settings
    @Shadow @Final public Setting<Boolean> knockback;
    @Shadow @Final public Setting<Double> knockbackHorizontal;
    @Shadow @Final public Setting<Double> knockbackVertical;
    @Shadow @Final public Setting<Boolean> explosions;

    // Velocity+ settings
    @Unique private SettingGroup rh$extra;
    @Unique private Setting<Boolean> rh$enableScale;
    @Unique private Setting<Double> rh$hScale;
    @Unique private Setting<Double> rh$vScale;
    @Unique private Setting<Boolean> rh$cancelExplosions;
    @Unique private Setting<Boolean> rh$noKbWhileGliding;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void rh$onInit(CallbackInfo ci) {
        Settings s = ((ModuleAccessor) this).randomhax$getSettings();

        rh$extra = s.createGroup("Velocity+");

        rh$enableScale = rh$extra.add(new BoolSetting.Builder()
            .name("custom-scale")
            .description("Scale your own velocity packet instead of Meteor's coarse scaling.")
            .defaultValue(true)
            .build()
        );

        rh$hScale = rh$extra.add(new DoubleSetting.Builder()
            .name("horizontal-scale")
            .description("Horizontal velocity scale for your EntityVelocityUpdateS2C.")
            .defaultValue(100.0)
            .min(0.0)
            .sliderRange(0.0, 200.0)
            .visible(rh$enableScale::get)
            .build()
        );

        rh$vScale = rh$extra.add(new DoubleSetting.Builder()
            .name("vertical-scale")
            .description("Vertical velocity scale for your EntityVelocityUpdateS2C.")
            .defaultValue(100.0)
            .min(0.0)
            .sliderRange(0.0, 200.0)
            .visible(rh$enableScale::get)
            .build()
        );

        rh$cancelExplosions = rh$extra.add(new BoolSetting.Builder()
            .name("cancel-explosions")
            .description("Cancel ExplosionS2CPacket entirely.")
            .defaultValue(false)
            .build()
        );

        rh$noKbWhileGliding = rh$extra.add(new BoolSetting.Builder()
            .name("no-kb-while-gliding")
            .description("Ignore knockback on you while elytra gliding.")
            .defaultValue(true)
            .build()
        );
    }

    // If we handle scaling/explosion here, stop Meteor's default handler
    @Inject(method = "onPacketReceive", at = @At("HEAD"), cancellable = true)
    private void rh$cancelMeteorHandler(PacketEvent.Receive event, CallbackInfo ci) {
        if ((rh$enableScale != null && rh$enableScale.get()) || (rh$cancelExplosions != null && rh$cancelExplosions.get())) {
            ci.cancel();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void rh$onPacket(PacketEvent.Receive event) {
        var mc = MinecraftClient.getInstance();
        if (mc == null) return;

        // Optional explosion cancel
        if (event.packet instanceof ExplosionS2CPacket) {
            if (rh$cancelExplosions != null && rh$cancelExplosions.get()) {
                event.cancel();
            }
            return;
        }

        // Only touch player knockback if Meteor's knockback is on
        if (!(event.packet instanceof EntityVelocityUpdateS2CPacket p) || !knockback.get()) return;
        if (mc.player == null || p.getEntityId() != mc.player.getId()) return;

        // Ignore KB while elytra-gliding if enabled
        if (rh$noKbWhileGliding != null && rh$noKbWhileGliding.get() && rh$isElytraFlying(mc.player)) {
            event.cancel();
            return;
        }

        // Custom scale
        if (rh$enableScale != null && rh$enableScale.get()) {
            double h = (rh$hScale != null ? rh$hScale.get() : 100.0) / 100.0;
            double v = (rh$vScale != null ? rh$vScale.get() : 100.0) / 100.0;

            ((EntityVelocityUpdateS2CPacketAccessor) p).setVelocityX((int) Math.round(p.getVelocityX() * h));
            ((EntityVelocityUpdateS2CPacketAccessor) p).setVelocityY((int) Math.round(p.getVelocityY() * v));
            ((EntityVelocityUpdateS2CPacketAccessor) p).setVelocityZ((int) Math.round(p.getVelocityZ() * h));
        }
    }

    // ---- helpers ----

    /**
     * 1.21.8-safe gliding check:
     * - Pose string check (no direct enum)
     * - Reflection: isFallFlying / isGliding
     * - Heuristic fallback (elytra equipped, not on ground, falling)
     */
    @Unique
    private boolean rh$isElytraFlying(ClientPlayerEntity p) {
        try {
            String poseName = String.valueOf(p.getPose());
            if ("FALL_FLYING".equals(poseName) || "GLIDING".equals(poseName)) return true;
        } catch (Throwable ignored) {}

        try {
            var m = p.getClass().getMethod("isFallFlying");
            Object o = m.invoke(p);
            if (o instanceof Boolean b && b) return true;
        } catch (Throwable ignored) {}
        try {
            var m = p.getClass().getMethod("isGliding");
            Object o = m.invoke(p);
            if (o instanceof Boolean b && b) return true;
        } catch (Throwable ignored) {}

        var chest = p.getEquippedStack(EquipmentSlot.CHEST);
        boolean hasElytra = chest != null && chest.getItem().toString().toLowerCase().contains("elytra");
        return hasElytra && !p.isOnGround() && !p.isSubmergedInWater() && p.getVelocity().y < 0.0;
    }
}
