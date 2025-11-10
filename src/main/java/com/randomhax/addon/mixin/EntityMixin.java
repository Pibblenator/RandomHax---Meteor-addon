package com.randomhax.addon.mixin;

import com.randomhax.addon.modules.AFKVanillaFly; // <- change this to a different module if you prefer
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Entity.class)
public abstract class EntityMixin {
    private boolean rh$isSelf() {
        return mc != null && mc.player != null
            && ((Entity) (Object) this).getUuid().equals(mc.player.getUuid());
    }

    private boolean rh$spoofActive() {
        // Enable the spoofing behavior when this module is active.
        // Swap AFKVanillaFly.class to another module class if desired.
        return Modules.get().isActive(AFKVanillaFly.class);
    }

    @Inject(method = "getPose", at = @At("HEAD"), cancellable = true)
    private void rh$spoofPose(CallbackInfoReturnable<EntityPose> cir) {
        if (rh$spoofActive() && rh$isSelf()) {
            cir.setReturnValue(EntityPose.STANDING);
        }
    }

    @Inject(method = "isSprinting", at = @At("HEAD"), cancellable = true)
    private void rh$forceSprint(CallbackInfoReturnable<Boolean> cir) {
        if (rh$spoofActive() && rh$isSelf()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void rh$cancelPush(Entity other, CallbackInfo ci) {
        if (rh$spoofActive() && rh$isSelf()
            && !other.getUuid().equals(((Entity)(Object)this).getUuid())) {
            ci.cancel();
        }
    }
}
