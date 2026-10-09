package main.walksy.lib.core.platform.input;

import net.minecraft.util.StringUtil;

public record CharacterEvent(int codepoint, int modifiers) {
    public String codepointAsString() {
        return Character.toString(this.codepoint);
    }

    public boolean isAllowedChatCharacter() {
        return !Character.isBmpCodePoint(this.codepoint) || StringUtil.isAllowedChatCharacter((char) this.codepoint);
    }
}
