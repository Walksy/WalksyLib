package main.walksy.lib.core.mixin.core.keys;

import main.walksy.lib.core.platform.WLKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.Category.class)
public abstract class KeyMappingCategoryMixin {
    @Inject(method = "label", at = @At("HEAD"), cancellable = true)
    private void walksylib$categoryName(final CallbackInfoReturnable<Component> cir) {
        final String name = WLKeys.categoryName((KeyMapping.Category) (Object) this);
        if (name != null) {
            cir.setReturnValue(Component.literal(name));
        }
    }
}
