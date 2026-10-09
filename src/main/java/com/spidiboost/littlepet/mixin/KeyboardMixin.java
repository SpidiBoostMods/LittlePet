package com.spidiboost.littlepet.mixin;

import com.spidiboost.littlepet.LittlePetClient;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Keyboard.class,priority=2000)
public final class KeyboardMixin {
    @Inject(method="onKey",at=@At("HEAD"),cancellable=true)
    private void before(long window,int key,int scan,int action,int modifiers,CallbackInfo ci) {
        LittlePetClient pets=LittlePetClient.INSTANCE;
        if(pets!=null && pets.beforeMenuKey(window,key,scan,action))ci.cancel();
    }
}
