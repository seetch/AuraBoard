package me.seetch.auraboard.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class TextUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private TextUtil() {
    }

    /**
     * Converts MiniMessage or legacy (&a, &#RRGGBB) to Component.
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        if (text.contains("§")) {
            return LegacyComponentSerializer.legacySection().deserialize(text);
        }

        if (text.contains("&") && !text.contains("<")) {
            return LEGACY.deserialize(text);
        }

        if (text.contains("&")) {
            text = text.replace("&", "§");
            Component legacy = LegacyComponentSerializer.legacySection().deserialize(text);
            return legacy;
        }

        return MM.deserialize(text);
    }

    public static String stripTags(String text) {
        if (text == null) return "";
        return MM.stripTags(text);
    }
}