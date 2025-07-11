package main.walksy.lib.core.mixin;

import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextFieldWidget.class)
public interface TextFieldWidgetAccessor {

    @Accessor("maxLength")
    int getMaxLength();

    @Accessor("firstCharacterIndex")
    int getFirstCharacterIndex();

    @Accessor("selectionStart")
    int getSelectionStart();

    @Accessor("selectionEnd")
    int getSelectionEnd();
}
