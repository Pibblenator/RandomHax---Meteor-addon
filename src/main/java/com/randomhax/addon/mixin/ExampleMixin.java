package com.randomhax.addon.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Safe no-op so your mixins file is satisfied
@Mixin(MinecraftClient.class)
public class ExampleMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void randomhax$tick(CallbackInfo ci) {}
}
