package com.randomhax.addon.modules.searcharea.modes;

import com.randomhax.addon.modules.searcharea.SearchAreaMode;
import com.randomhax.addon.modules.searcharea.SearchAreaModes;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.systems.modules.movement.BoatFly;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileReader;

import static meteordevelopment.meteorclient.utils.player.ChatUtils.info;

public class Rectangle extends SearchAreaMode {
    private PathingDataRectangle pd;
    private boolean goingToStart = true;
    private long startTime;

    public Rectangle() { super(SearchAreaModes.Rectangle); }

    @Override
    public void onActivate() {
        goingToStart = true;
        File file = getJsonFile(super.toString());
        if (file == null || !file.exists()) {
            pd = new PathingDataRectangle(
                searchArea.startPos.get(),
                searchArea.targetPos.get(),
                searchArea.startPos.get(),
                90f, true,
                mc.player != null ? (int) mc.player.getZ() : 0
            );
        } else {
            try (FileReader reader = new FileReader(file)) {
                pd = GSON.fromJson(reader, PathingDataRectangle.class);
            } catch (Exception ignored) { }
        }
        startTime = System.nanoTime();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        super.saveToJson(goingToStart, pd);
    }

    private void printRectangleEstimate() {
        Module boatFly = Modules.get().get(BoatFly.class);
        double speedBPS = 20.0;
        try {
            // BoatFly has a "speed" setting; read reflectively but safely.
            var s = boatFly.settings.get("speed");
            if (s != null && s.get() instanceof Number n) speedBPS = n.doubleValue();
        } catch (Throwable ignored) { }

        double rowDistance = Math.abs(pd.initialPos.getX() - pd.targetPos.getX());
        int rowCount = Math.abs(pd.currPos.getZ() - pd.targetPos.getZ()) / 16 / searchArea.rowGap.get();
        double totalBlocks = rowCount * (rowDistance + (searchArea.rowGap.get() * 16));
        long totalSeconds = (long) (totalBlocks / Math.max(0.1, speedBPS));
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        info("Estimate: %02d:%02d:%02d (speed=%.2f, gap=%d chunks).",
            hours, minutes, seconds, speedBPS, searchArea.rowGap.get());
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // autosave ~0.6s
        if (System.nanoTime() - startTime > 6e8) {
            startTime = System.nanoTime();
            super.saveToJson(goingToStart, pd);
        }

        if (goingToStart) {
            if (mc.player.getBlockPos().getSquaredDistance(pd.currPos.getX(), mc.player.getY(), pd.currPos.getZ()) < 25) {
                goingToStart = false;
                mc.player.setVelocity(0, 0, 0);
                printRectangleEstimate();
            } else {
                mc.player.setYaw((float) Rotations.getYaw(pd.currPos.toCenterPos()));
                setPressed(mc.options.forwardKey, true);
            }
            return;
        }

        setPressed(mc.options.forwardKey, true);
        mc.player.setYaw(pd.yawDirection);

        if (mc.player.getBlockPos().getSquaredDistance(pd.targetPos.getX(), mc.player.getY(), pd.targetPos.getZ()) < 400) {
            setPressed(mc.options.forwardKey, false);
            searchArea.toggle();

            if (searchArea.disconnectOnCompletion.get()) {
                var ar = Modules.get().get(AutoReconnect.class);
                if (ar.isActive()) ar.toggle();
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[Search Area] Path complete")));
            }
            return;
        }

        // main horizontal leg done?
        boolean pastRight = pd.yawDirection == -90.0f && mc.player.getX() >= Math.max(pd.initialPos.getX(), pd.targetPos.getX());
        boolean pastLeft  = pd.yawDirection ==  90.0f && mc.player.getX() <= Math.min(pd.initialPos.getX(), pd.targetPos.getX());

        if (pd.mainPath && (pastRight || pastLeft)) {
            pd.yawDirection = (mc.player.getZ() < pd.targetPos.getZ()) ? 0.0f : 180.0f; // turn north/south
            pd.mainPath = false;
            mc.player.setVelocity(0, 0, 0);
            return;
        }

        // vertical leg gap reached?
        if (!pd.mainPath && Math.abs(mc.player.getZ() - pd.lastCompleteRowZ) >= (16 * searchArea.rowGap.get())) {
            pd.lastCompleteRowZ = (int) mc.player.getZ();
            pd.yawDirection = (pd.initialPos.getX() > mc.player.getX() ? -90.0f : 90.0f); // go opposite horizontal
            pd.mainPath = true;
            mc.player.setVelocity(0, 0, 0);
        }
    }

    public static class PathingDataRectangle extends PathingData {
        public BlockPos targetPos;
        public int lastCompleteRowZ;

        public PathingDataRectangle(BlockPos initialPos, BlockPos targetPos, BlockPos currPos,
                                    float yawDirection, boolean mainPath, int lastCompleteRowZ) {
            this.initialPos = initialPos;
            this.targetPos = targetPos;
            this.currPos = currPos;
            this.yawDirection = yawDirection;
            this.mainPath = mainPath;
            this.lastCompleteRowZ = lastCompleteRowZ;
        }
    }
}
