package com.randomhax.addon.modules.searcharea;

import com.randomhax.addon.RandomHax;
import com.randomhax.addon.modules.searcharea.modes.Rectangle;
import com.randomhax.addon.modules.searcharea.modes.Spiral;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;

public class SearchArea extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<SearchAreaModes> chunkLoadMode = sgGeneral.add(new EnumSetting.Builder<SearchAreaModes>()
        .name("mode").description("How to traverse chunks.")
        .defaultValue(SearchAreaModes.Rectangle)
        .onModuleActivated(m -> onModeChanged(m.get()))
        .onChanged(this::onModeChanged)
        .build()
    );

    public final Setting<BlockPos> startPos = sgGeneral.add(new BlockPosSetting.Builder()
        .name("start-position").description("Start of rectangle (Y ignored).")
        .defaultValue(new BlockPos(0, 0, 0))
        .visible(() -> chunkLoadMode.get() == SearchAreaModes.Rectangle)
        .build()
    );

    public final Setting<BlockPos> targetPos = sgGeneral.add(new BlockPosSetting.Builder()
        .name("end-position").description("End of rectangle (Y ignored).")
        .defaultValue(new BlockPos(0, 0, 0))
        .visible(() -> chunkLoadMode.get() == SearchAreaModes.Rectangle)
        .build()
    );

    public final Setting<Integer> rowGap = sgGeneral.add(new IntSetting.Builder()
        .name("path-gap").description("Chunk gap between passes.")
        .defaultValue(12).min(1).sliderRange(0, 32)
        .build()
    );

    public final Setting<String> saveLocation = sgGeneral.add(new StringSetting.Builder()
        .name("save-name").description("Folder name for saving progress (blank = no save).")
        .defaultValue("")
        .build()
    );

    public final Setting<Boolean> disconnectOnCompletion = sgGeneral.add(new BoolSetting.Builder()
        .name("disconnect-on-completion")
        .description("Disconnect when a rectangle run completes (disables AutoReconnect first).")
        .defaultValue(false)
        .visible(() -> chunkLoadMode.get() == SearchAreaModes.Rectangle)
        .build()
    );

    public SearchArea() {
        super(RandomHax.CATEGORY, "search-area",
            "Loads chunks by flying a rectangle between two points or an endless spiral from your position.");
    }

    private SearchAreaMode currentMode = new Rectangle();

    @Override public void onActivate() { currentMode.onActivate(); }
    @Override public void onDeactivate() { currentMode.onDeactivate(); }

    @EventHandler
    private void onTick(TickEvent.Post e) { currentMode.onTick(); }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        WButton clear = list.add(theme.button("Clear Current Save")).widget();
        clear.action = currentMode::clear;

        WButton clearAll = list.add(theme.button("Clear All Saves")).widget();
        clearAll.action = currentMode::clearAll;
        return list;
    }

    private void onModeChanged(SearchAreaModes mode) {
        switch (mode) {
            case Rectangle -> currentMode = new Rectangle();
            case Spiral    -> currentMode = new Spiral();
        }
    }
}
