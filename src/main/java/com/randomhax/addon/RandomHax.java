package com.randomhax.addon;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

// Modules
import com.randomhax.addon.modules.RocketMan;
import com.randomhax.addon.modules.AutoBedTrap;
import com.randomhax.addon.modules.searcharea.SearchArea;
import com.randomhax.addon.modules.AdBlocker;
import com.randomhax.addon.modules.Pitch40Util;
import com.randomhax.addon.modules.TrailFollower;
import com.randomhax.addon.modules.AFKVanillaFly;
import com.randomhax.addon.modules.AutoPortal;
import com.randomhax.addon.modules.PathMacro;
import com.randomhax.addon.modules.MapSaver;
import com.randomhax.addon.modules.YesCom;

// Fabric client command bootstrap
import com.randomhax.addon.commands.YesComCommands;

public class RandomHax extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("RandomHax");

    @Override
    public void onInitialize() {
        LOG.info("Initializing RandomHax");

        // Modules
        Modules.get().add(new RocketMan());
        Modules.get().add(new AutoBedTrap());
        Modules.get().add(new SearchArea());
        Modules.get().add(new AdBlocker());
        Modules.get().add(new Pitch40Util());
        Modules.get().add(new TrailFollower());
        Modules.get().add(new AFKVanillaFly());
        Modules.get().add(new AutoPortal());
        Modules.get().add(new PathMacro());
        Modules.get().add(new MapSaver());
        Modules.get().add(new YesCom());

        // Register client-side commands via Fabric (not Meteor Commands API)
        YesComCommands.register();
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
