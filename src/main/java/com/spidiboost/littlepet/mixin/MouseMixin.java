package com.spidiboost.littlepet.mixin;

import com.spidiboost.littlepet.LittlePetClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Mouse.class,priority=2000)
public final class MouseMixin {
    @Inject(method="onMouseButton",at=@At("HEAD"),cancellable=true)
    private void before(long window,int button,int action,int modifiers,CallbackInfo ci) {
        LittlePetClient pets=LittlePetClient.INSTANCE;
        if(pets!=null && pets.beforeMenuMouse(window,button,action))ci.cancel();
    }
}
