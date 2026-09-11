package dev.anchorlight.zander.hub.portal;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * How a portal's region is drawn in the world. {@code NONE} leaves it invisible,
 * {@code NETHER} fills empty space with real nether portal blocks, and {@code TINT}
 * renders a translucent panel in {@link #argb()}. The colour is kept when switching
 * styles so an admin can toggle back to their previous tint.
 */
public record PortalAppearance(Style style, int argb) {
    public enum Style { NONE, NETHER, TINT }

    /// Alpha applied when a colour is given without one (#RRGGBB or a name).
    public static final int DEFAULT_ALPHA = 0x80;
    public static final int DEFAULT_TINT = (DEFAULT_ALPHA << 24) | 0x8932B8;
    public static final PortalAppearance NONE = new PortalAppearance(Style.NONE, DEFAULT_TINT);

    /// Minecraft dye colours, so admins can use familiar names instead of hex.
    private static final Map<String, Integer> NAMED_COLOURS = new LinkedHashMap<>();

    static {
        NAMED_COLOURS.put("white", 0xF9FFFE);
        NAMED_COLOURS.put("orange", 0xF9801D);
        NAMED_COLOURS.put("magenta", 0xC74EBD);
        NAMED_COLOURS.put("light_blue", 0x3AB3DA);
        NAMED_COLOURS.put("yellow", 0xFED83D);
        NAMED_COLOURS.put("lime", 0x80C71F);
        NAMED_COLOURS.put("pink", 0xF38BAA);
        NAMED_COLOURS.put("gray", 0x474F52);
        NAMED_COLOURS.put("light_gray", 0x9D9D97);
        NAMED_COLOURS.put("cyan", 0x169C9C);
        NAMED_COLOURS.put("purple", 0x8932B8);
        NAMED_COLOURS.put("blue", 0x3C44AA);
        NAMED_COLOURS.put("brown", 0x835432);
        NAMED_COLOURS.put("green", 0x5E7C16);
        NAMED_COLOURS.put("red", 0xB02E26);
        NAMED_COLOURS.put("black", 0x1D1D21);
    }

    public PortalAppearance {
        Objects.requireNonNull(style, "style");
    }

    public PortalAppearance withStyle(Style newStyle) {
        return new PortalAppearance(newStyle, argb);
    }

    public static Set<String> colourNames() {
        return NAMED_COLOURS.keySet();
    }

    /// Parses `#RRGGBB`, `#AARRGGBB` (the `#` is optional) or a dye colour name.
    public static Optional<Integer> parseColour(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        String name = value.replace('-', '_').replace("grey", "gray");
        if (NAMED_COLOURS.containsKey(name)) {
            return Optional.of((DEFAULT_ALPHA << 24) | NAMED_COLOURS.get(name));
        }

        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (!hex.matches("[0-9a-f]{6}|[0-9a-f]{8}")) {
            return Optional.empty();
        }
        long parsed = Long.parseLong(hex, 16);
        return Optional.of(hex.length() == 6 ? (DEFAULT_ALPHA << 24) | (int) parsed : (int) parsed);
    }

    public static String formatColour(int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    public static Optional<Style> parseStyle(String input) {
        if (input == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Style.valueOf(input.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
