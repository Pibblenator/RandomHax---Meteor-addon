package com.randomhax.addon.modules.searcharea.modes;

import com.randomhax.addon.modules.searcharea.SearchAreaMode;
import com.randomhax.addon.modules.searcharea.SearchAreaModes;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static meteordevelopment.meteorclient.utils.player.ChatUtils.info;

public class Spiral extends SearchAreaMode {
    private PathingDataSpiral pd;
    private boolean goingToStart = true;
    private long startTime;

    public Spiral() { super(SearchAreaModes.Spiral); }

    @Override
    public void onActivate() {
        startTime = System.nanoTime();
        goingToStart = true;

        File file = getJsonFile(super.toString());
        if (file == null || !file.exists()) {
            BlockPos here = mc.player != null ? mc.player.getBlockPos() : BlockPos.ORIGIN;
            pd = new PathingDataSpiral(here, here, -90.0f, true, 0, 0);
        } else {
            try (FileReader reader = new FileReader(file)) {
                pd = GSON.fromJson(reader, PathingDataSpiral.class);
                info("Loaded saved spiral; heading to last point.");
            } catch (IOException e) {
                info("Failed to load saved spiral; disabling.");
                this.disable();
            }
        }
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        super.saveToJson(goingToStart, pd);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        if (System.nanoTime() - startTime > 6e8) {
            startTime = System.nanoTime();
            super.saveToJson(goingToStart, pd);
        }

        if (System.nanoTime() < paused) {
            setPressed(mc.options.forwardKey, false);
            return;
        }

        if (goingToStart) {
            if (mc.player.getBlockPos().getSquaredDistance(pd.currPos.getX(), mc.player.getY(), pd.currPos.getZ()) < 25) {
                goingToStart = false;
                mc.player.setVelocity(0, 0, 0);
            } else {
                mc.player.setYaw((float) Rotations.getYaw(pd.currPos.toCenterPos()));
                setPressed(mc.options.forwardKey, true);
            }
            return;
        }

        setPressed(mc.options.forwardKey, true);
        mc.player.setYaw(pd.yawDirection);

        int blockGap = 16 * searchArea.rowGap.get();

        if (pd.mainPath && Math.abs(mc.player.getX() - pd.initialPos.getX()) >= (blockGap + pd.spiralWidth)) {
            pd.yawDirection += 90.0f;
            pd.initialPos = new BlockPos((int) mc.player.getX(), pd.initialPos.getY(), pd.initialPos.getZ());
            pd.spiralWidth += blockGap;
            pd.mainPath = false;
            mc.player.setVelocity(0, 0, 0);
        } else if (!pd.mainPath && Math.abs(mc.player.getZ() - pd.initialPos.getZ()) >= (blockGap + pd.spiralHeight)) {
            pd.yawDirection += 90.0f;
            pd.initialPos = new BlockPos(pd.initialPos.getX(), pd.initialPos.getY(), (int) mc.player.getZ());
            pd.spiralHeight += blockGap;
            pd.mainPath = true;
            mc.player.setVelocity(0, 0, 0);
        }
    }

    public static class PathingDataSpiral extends PathingData {
        public int spiralWidth;
        public int spiralHeight;

        public PathingDataSpiral(BlockPos initialPos, BlockPos currPos, float yawDirection,
                                 boolean mainPath, int spiralWidth, int spiralHeight) {
            this.initialPos = initialPos;
            this.currPos = currPos;
            this.yawDirection = yawDirection;
            this.mainPath = mainPath;
            this.spiralWidth = spiralWidth;
            this.spiralHeight = spiralHeight;
        }
    }
}
