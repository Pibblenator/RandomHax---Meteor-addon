package com.randomhax.addon.modules;

import com.randomhax.addon.RandomHax;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.text.Text;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MapSaver extends Module {
    // Settings groups
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgNaming  = settings.createGroup("Naming");

    // --- General ---
    private final Setting<String> folderPath = sgGeneral.add(new StringSetting.Builder()
        .name("folder-path")
        .description("Full folder path to save maps (e.g., C:/Users/You/Desktop/maps)")
        .defaultValue(System.getProperty("user.home") + File.separator + "2b2t-maps")
        .build()
    );

    private final Setting<Boolean> autoSave = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-save-on-view")
        .description("Automatically save maps when you hold them")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> quietMode = sgGeneral.add(new BoolSetting.Builder()
        .name("quiet-mode")
        .description("Only show one message per session instead of every save")
        .defaultValue(false)
        .build()
    );

    // --- Naming ---
    private final Setting<NameFormat> nameFormat = sgNaming.add(new EnumSetting.Builder<NameFormat>()
        .name("name-format")
        .description("How to name the saved map files")
        .defaultValue(NameFormat.MapName)
        .build()
    );

    private final Setting<Boolean> includeTimestamp = sgNaming.add(new BoolSetting.Builder()
        .name("include-timestamp")
        .description("Add timestamp to filename")
        .defaultValue(true)
        .visible(() -> nameFormat.get() != NameFormat.Timestamp)
        .build()
    );

    private final Setting<Boolean> includeMapId = sgNaming.add(new BoolSetting.Builder()
        .name("include-map-id")
        .description("Add map ID to filename")
        .defaultValue(true)
        .visible(() -> nameFormat.get() == NameFormat.MapName)
        .build()
    );

    // State
    private final Set<Integer> savedMaps = new HashSet<>();
    private int checkDelay = 0;
    private boolean hasShownMessage = false;

    public MapSaver() {
        super(RandomHax.CATEGORY, "map-saver", "Saves map art (held/hotbar) to PNG files.");
    }

    @Override
    public void onActivate() {
        savedMaps.clear();
        hasShownMessage = false;
        checkDelay = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!autoSave.get()) return;

        // check ~1s
        if (++checkDelay < 20) return;
        checkDelay = 0;

        // Main-hand
        ItemStack main = mc.player.getMainHandStack();
        if (main.getItem() == Items.FILLED_MAP) saveMapImage(main);

        // Off-hand
        ItemStack off  = mc.player.getOffHandStack();
        if (off.getItem() == Items.FILLED_MAP) saveMapImage(off);

        // Hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.FILLED_MAP) saveMapImage(stack);
        }
    }

    private void saveMapImage(ItemStack mapStack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        // 1) Map ID component
        MapIdComponent mapId = mapStack.get(DataComponentTypes.MAP_ID);
        if (mapId == null) return;

        int id = mapId.id();

        // prevent re-saves this session
        if (savedMaps.contains(id)) return;

        // 2) Map state & pixel data
        MapState mapState = mc.world.getMapState(mapId);
        if (mapState == null || mapState.colors == null || mapState.colors.length < 128 * 128) return;

        try {
            BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 128; x++) {
                for (int z = 0; z < 128; z++) {
                    int index = x + z * 128;
                    byte colorByte = mapState.colors[index];
                    int rgb = getMapColor(colorByte);
                    image.setRGB(x, z, rgb);
                }
            }

            // 3) Ensure folder exists
            File folder = new File(folderPath.get());
            if (!folder.exists() && !folder.mkdirs()) {
                error("Failed to create folder: " + folder.getAbsolutePath());
                return;
            }

            // 4) Filename + write
            String filename = generateFilename(mapStack, id);
            File output = new File(folder, filename);
            ImageIO.write(image, "png", output);

            savedMaps.add(id);

            if (quietMode.get()) {
                if (!hasShownMessage) {
                    info("Maps are being saved to: " + folder.getAbsolutePath());
                    hasShownMessage = true;
                }
            } else {
                info("Saved map to " + output.getAbsolutePath());
            }
        } catch (IOException e) {
            error("Failed to save map: " + e.getMessage());
        } catch (Exception e) {
            error("Unexpected error: " + e.getMessage());
        }
    }

    private String generateFilename(ItemStack mapStack, int id) {
        StringBuilder name = new StringBuilder();
        String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

        switch (nameFormat.get()) {
            case MapName -> {
                Text t = mapStack.getName();
                String mapName = (t == null ? "map" : t.getString());
                mapName = mapName.replaceAll("[^a-zA-Z0-9-_]", "_");

                if (mapName.equalsIgnoreCase("Map") || mapName.equalsIgnoreCase("Filled_Map") || mapName.isBlank()) {
                    name.append("map");
                } else {
                    name.append(mapName);
                }

                if (includeMapId.get()) name.append("_").append(id);
                if (includeTimestamp.get()) name.append("_").append(ts);
            }
            case MapId -> {
                name.append("map_").append(id);
                if (includeTimestamp.get()) name.append("_").append(ts);
            }
            case Timestamp -> {
                name.append("map_").append(ts);
            }
        }

        name.append(".png");
        return name.toString();
    }

    private int getMapColor(byte colorByte) {
        // vanilla color index (0..255)
        int idx = colorByte & 0xFF;

        // 0..3 are "empty" / no color
        if (idx < 4) return 0;

        int baseIndex = idx / 4;
        int shade = idx % 4;

        try {
            net.minecraft.block.MapColor mc = net.minecraft.block.MapColor.get(baseIndex);
            if (mc != null) {
                int rgb = mc.getRenderColor(shade);

                // Fallback path for some colors in certain mappings
                if (rgb == 0 && mc.color != 0) {
                    rgb = mc.color;

                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    double mul = switch (shade) {
                        case 0 -> 180.0 / 255.0;
                        case 1 -> 220.0 / 255.0;
                        case 2 -> 1.0;
                        case 3 -> 135.0 / 255.0;
                        default -> 1.0;
                    };

                    r = (int) (r * mul);
                    g = (int) (g * mul);
                    b = (int) (b * mul);
                    return (r << 16) | (g << 8) | b;
                }

                return rgb;
            }
        } catch (Exception ignored) {}

        // super-safe gray fallback
        int gray = Math.min(255, idx * 2);
        return (gray << 16) | (gray << 8) | gray;
    }

    public enum NameFormat {
        MapName,
        MapId,
        Timestamp
    }
}
