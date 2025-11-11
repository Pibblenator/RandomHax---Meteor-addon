package com.randomhax.addon.mixin.meteor;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Timer.class, remap = false)
public abstract class TimerMixin {
    @Shadow @Final private SettingGroup sgGeneral;
    @Shadow @Final private Setting<Double> multiplier;

    @Unique private Setting<Boolean> autoAdjust;
    @Unique private Setting<Boolean> onlyWhenTraveling;
    @Unique private Setting<Double> travelSpeedThreshold;
    @Unique private Setting<Double> minSpeed;
    @Unique private Setting<Double> maxSpeed;
    @Unique private Setting<Integer> checkRadius;
    @Unique private Setting<Integer> unloadedThreshold;
    @Unique private Setting<Double> adjustSpeed;
    @Unique private Setting<Integer> checkInterval;

    @Unique private double targetSpeed = 1.0;
    @Unique private double currentAutoSpeed = 1.0;
    @Unique private int tickCounter = 0;
    @Unique private int lastUnloadedCount = 0;
    @Unique private net.minecraft.util.math.Vec3d lastPlayerPos = null;
    @Unique private double currentSpeed = 0;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        autoAdjust = sgGeneral.add(new BoolSetting.Builder().name("auto-adjust").defaultValue(false).build());
        onlyWhenTraveling = sgGeneral.add(new BoolSetting.Builder().name("only-when-traveling").defaultValue(true).visible(autoAdjust::get).build());
        travelSpeedThreshold = sgGeneral.add(new DoubleSetting.Builder().name("travel-speed-threshold").defaultValue(10.0).min(1.0).sliderRange(1.0, 100.0).visible(() -> autoAdjust.get() && onlyWhenTraveling.get()).build());
        minSpeed = sgGeneral.add(new DoubleSetting.Builder().name("min-speed").defaultValue(0.4).min(0.1).sliderRange(0.1, 1.0).visible(autoAdjust::get).build());
        maxSpeed = sgGeneral.add(new DoubleSetting.Builder().name("max-speed").defaultValue(1.0).min(0.1).sliderRange(0.1, 2.0).visible(autoAdjust::get).build());
        checkRadius = sgGeneral.add(new IntSetting.Builder().name("check-radius").defaultValue(3).min(1).sliderRange(1, 8).visible(autoAdjust::get).build());
        unloadedThreshold = sgGeneral.add(new IntSetting.Builder().name("unloaded-threshold").defaultValue(6).min(1).sliderRange(1, 20).visible(autoAdjust::get).build());
        adjustSpeed = sgGeneral.add(new DoubleSetting.Builder().name("adjust-speed").defaultValue(0.15).min(0.01).sliderRange(0.01, 1.0).visible(autoAdjust::get).build());
        checkInterval = sgGeneral.add(new IntSetting.Builder().name("check-interval").defaultValue(5).min(1).sliderRange(1, 40).visible(autoAdjust::get).build());
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        var mc = MinecraftClient.getInstance();
        if (!Utils.canUpdate() || autoAdjust == null || !autoAdjust.get() || mc.player == null || mc.world == null) return;

        if (lastPlayerPos != null) {
            var currentPos = mc.player.getPos();
            double distanceTraveled = currentPos.subtract(lastPlayerPos).multiply(1, 0, 1).length();
            double speedBPS = distanceTraveled * 20.0;
            currentSpeed = speedBPS * 3.6;
        }
        lastPlayerPos = mc.player.getPos();

        tickCounter++;
        if (tickCounter < checkInterval.get()) return;
        tickCounter = 0;

        if (onlyWhenTraveling.get() && currentSpeed < travelSpeedThreshold.get()) {
            targetSpeed = 1.0;
            currentAutoSpeed = 1.0;
            multiplier.set(1.0);
            lastUnloadedCount = 0;
            return;
        }

        int unloadedChunks = countUnloadedChunks();
        if (unloadedChunks > unloadedThreshold.get()) {
            double severity = Math.min(1.0, (double) unloadedChunks / (unloadedThreshold.get() * 2.0));
            targetSpeed = minSpeed.get() + (maxSpeed.get() - minSpeed.get()) * (1.0 - severity);
        } else {
            targetSpeed = maxSpeed.get();
        }

        double diff = targetSpeed - currentAutoSpeed;
        if (Math.abs(diff) > 0.01) {
            currentAutoSpeed += diff * adjustSpeed.get();
            double clamped = Math.max(minSpeed.get(), Math.min(maxSpeed.get(), currentAutoSpeed));
            multiplier.set(clamped);
        }

        lastUnloadedCount = unloadedChunks;
    }

    @Unique
    private int countUnloadedChunks() {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return 0;
        ClientChunkManager cm = mc.world.getChunkManager();
        ChunkPos pc = mc.player.getChunkPos();
        int r = checkRadius.get();
        int c = 0;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                Chunk ch = cm.getChunk(pc.x + x, pc.z + z, ChunkStatus.FULL, false);
                if (ch == null) c++;
            }
        }
        return c;
    }

    @Unique public boolean isAutoAdjustEnabled() { return autoAdjust != null && autoAdjust.get(); }
    @Unique public double getCurrentAutoSpeed() { return currentAutoSpeed; }
    @Unique public int getLastUnloadedCount() { return lastUnloadedCount; }
}
