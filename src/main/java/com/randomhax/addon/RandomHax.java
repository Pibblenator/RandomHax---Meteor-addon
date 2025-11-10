package com.randomhax.addon;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

// Modules
import com.randomhax.addon.modules.RocketMan;

public class RandomHax extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("RandomHax");

    @Override
    public void onInitialize() {
        LOG.info("Initializing RandomHax");
        Modules.get().add(new RocketMan());
        Modules.get().add(new com.randomhax.addon.modules.AutoBedTrap());
        Modules.get().add(new com.randomhax.addon.modules.searcharea.SearchArea());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.randomhax.addon";
    }
}
