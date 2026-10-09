package com.spidiboost.littlepet.mixin;

import com.spidiboost.littlepet.BindConflicts;
import com.spidiboost.littlepet.LittlePetClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.minecraft.client.gui.screen.option.ControlsListWidget$KeyBindingEntry")
public abstract class ControlsEntryMixin {
    @Shadow @Final private KeyBinding binding;
    @Shadow @Final private ButtonWidget editButton;
    @Shadow private boolean duplicate;
    @Inject(method="update",at=@At("TAIL"))
    private void conflict(CallbackInfo ci) {
        LittlePetClient pets=LittlePetClient.INSTANCE;
        if(pets!=null && binding==pets.getMenuKey() && BindConflicts.atools(binding)) {
            duplicate=true;
            editButton.setMessage(Text.literal("[ ").append(binding.getBoundKeyLocalizedText()).append(" ]").formatted(Formatting.RED));
            editButton.setTooltip(Tooltip.of(Text.literal("Эта кнопка также назначена в ATools")));
        }
    }
}
