package com.randomhax.addon.mixin;

import com.randomhax.addon.mixin.EntityVelocityUpdateS2CPacketAccessor;
import com.randomhax.addon.mixin.ModuleAccessor;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Velocity.class, remap = false)
public abstract class VelocityMixin {
    @Shadow @Final public Setting<Boolean> knockback;
    @Shadow @Final public Setting<Double> knockbackHorizontal;
    @Shadow @Final public Setting<Double> knockbackVertical;
    @Shadow @Final public Setting<Boolean> explosions;

    @Unique private SettingGroup rh$extra;
    @Unique private Setting<Boolean> rh$enableScale;
    @Unique private Setting<Double> rh$hScale;
    @Unique private Setting<Double> rh$vScale;
    @Unique private Setting<Boolean> rh$cancelExplosions;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void rh$onInit(CallbackInfo ci) {
        Settings s = ((ModuleAccessor) this).randomhax$getSettings();
        rh$extra = s.createGroup("Velocity+");
        rh$enableScale = rh$extra.add(new BoolSetting.Builder().name("custom-scale").defaultValue(true).build());
        rh$hScale = rh$extra.add(new DoubleSetting.Builder().name("horizontal-scale").defaultValue(100.0).min(0.0).sliderRange(0.0, 200.0).visible(rh$enableScale::get).build());
        rh$vScale = rh$extra.add(new DoubleSetting.Builder().name("vertical-scale").defaultValue(100.0).min(0.0).sliderRange(0.0, 200.0).visible(rh$enableScale::get).build());
        rh$cancelExplosions = rh$extra.add(new BoolSetting.Builder().name("cancel-explosions").defaultValue(false).build());
    }

    @Inject(method = "onPacketReceive", at = @At("HEAD"), cancellable = true)
    private void rh$cancelMeteorHandler(PacketEvent.Receive event, CallbackInfo ci) {
        if ((rh$enableScale != null && rh$enableScale.get()) || (rh$cancelExplosions != null && rh$cancelExplosions.get())) ci.cancel();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void rh$onPacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityVelocityUpdateS2CPacket p && knockback.get()) {
            var mc = MinecraftClient.getInstance();
            if (mc.player != null && rh$enableScale.get() && p.getEntityId() == mc.player.getId()) {
                double h = rh$hScale.get() / 100.0;
                double v = rh$vScale.get() / 100.0;
                ((EntityVelocityUpdateS2CPacketAccessor) p).setVelocityX((int) (p.getVelocityX() * h));
                ((EntityVelocityUpdateS2CPacketAccessor) p).setVelocityY((int) (p.getVelocityY() * v));
                ((EntityVelocityUpdateS2CPacketAccessor) p).setVelocityZ((int) (p.getVelocityZ() * h));
            }
        } else if (event.packet instanceof ExplosionS2CPacket) {
            if (rh$cancelExplosions.get()) event.cancel();
        }
    }
}
