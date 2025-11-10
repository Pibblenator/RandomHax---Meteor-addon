package com.randomhax.addon.mixin;

import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Module.class, remap = false)
public interface ModuleAccessor {
    @Accessor("settings") Settings randomhax$getSettings();
}
