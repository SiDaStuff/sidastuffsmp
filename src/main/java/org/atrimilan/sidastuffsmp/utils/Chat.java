package org.atrimilan.sidastuffsmp.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;

public final class Chat {

    private static final TextColor LIGHT_CYAN = TextColor.color(0x88FFFF);
    private static final TextColor GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor DARK_GRAY = TextColor.color(0x666666);

    private Chat() {}

    public static Component prefix() {
        String text = SiDaStuffSmp.getInstance().getConfig().getString("chat-prefix", "sidastuff SMP");
        return Component.text("[" + text + "] ", GRAY)
                .decoration(TextDecoration.BOLD, false);
    }

    public static Component prefixed(String text, NamedTextColor color) {
        return Component.empty()
                .append(prefix())
                .append(Component.text(text, color)
                        .decoration(TextDecoration.BOLD, false));
    }

    public static Component prefixed(Component content) {
        return Component.empty()
                .append(prefix())
                .append(content);
    }

    public static Component gray(String text) {
        return Component.text(text, GRAY)
                .decoration(TextDecoration.BOLD, false);
    }

    public static Component darkGray(String text) {
        return Component.text(text, DARK_GRAY)
                .decoration(TextDecoration.BOLD, false);
    }
}
