package com.randomhax.addon.modules.searcharea;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.*;

public class SearchAreaMode {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected final SearchArea searchArea;
    protected final MinecraftClient mc;
    private final SearchAreaModes type;

    /** simple pause timer you can set from subclasses (System.nanoTime() deadline). */
    protected long paused = 0;

    public SearchAreaMode(SearchAreaModes type) {
        this.searchArea = Modules.get().get(SearchArea.class);
        this.mc = MinecraftClient.getInstance();
        this.type = type;
    }

    public void onActivate() { }
    public void onDeactivate() { setPressed(mc.options.forwardKey, false); }
    public void onTick() { }

    public void disable() { if (searchArea.isActive()) searchArea.toggle(); }

    /** Key helper (drops the bep utils dependency). */
    protected void setPressed(KeyBinding key, boolean down) {
        if (key != null) key.setPressed(down);
    }

    /** Save path json under: .minecraft/.meteor/search-area/<saveName>/<mode>.json */
    protected File getJsonFile(String fileName) {
        try {
            return new File(
                new File(new File(MeteorClient.FOLDER, "search-area"), searchArea.saveLocation.get()),
                fileName + ".json"
            );
        } catch (NullPointerException e) {
            return null;
        }
    }

    protected void saveToJson(boolean goingToStart, PathingData pd) {
        if (pd == null) return;
        if (!goingToStart && mc.player != null) pd.currPos = mc.player.getBlockPos();

        try {
            File file = getJsonFile(type.toString());
            if (file == null) return;
            // ensure dirs
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();

            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(pd, writer);
            }
        } catch (IOException ignored) { }
    }

    /** base payload shared by modes */
    protected static class PathingData {
        public BlockPos initialPos;
        public BlockPos currPos;
        public float yawDirection;
        public boolean mainPath;
    }

    public void clear() {
        File f = getJsonFile(type.toString());
        if (f != null) //noinspection ResultOfMethodCallIgnored
            f.delete();
    }

    public void clear(String mode) {
        File f = getJsonFile(mode);
        if (f != null) //noinspection ResultOfMethodCallIgnored
            f.delete();
    }

    public void clearAll() {
        for (SearchAreaModes m : SearchAreaModes.values()) clear(m.toString());
    }

    @Override public String toString() { return type.toString(); }
}
