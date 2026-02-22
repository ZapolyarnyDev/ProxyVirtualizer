package io.github.zapolyarnydev.proxyvirtualizer.plugin.text;

import net.kyori.adventure.text.Component;

import java.lang.reflect.Method;

public final class AdventureComponentParser {

    public Component parse(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return Component.empty();
        }

        String input = rawInput.trim();

        if (hasPrefix(input, "mm:") || hasPrefix(input, "mini:")) {
            return parseMiniMessage(stripPrefix(input));
        }

        if (hasPrefix(input, "legacy:")) {
            return parseLegacy(stripPrefix(input));
        }

        if (hasPrefix(input, "json:")) {
            return parseJson(stripPrefix(input));
        }

        if (input.startsWith("&")) {
            return parseLegacy(input);
        }

        return parseMiniMessage(input);
    }

    private Component parseMiniMessage(String input) {
        try {
            Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Object miniMessage = miniMessageClass.getMethod("miniMessage").invoke(null);
            Object component = miniMessageClass.getMethod("deserialize", String.class).invoke(miniMessage, input);
            if (component instanceof Component parsed) {
                return parsed;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {

        }
        return Component.text(input);
    }

    private Component parseLegacy(String input) {
        try {
            Class<?> serializerClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
            Method factory = serializerClass.getMethod("legacyAmpersand");
            Object serializer = factory.invoke(null);
            Object component = serializerClass.getMethod("deserialize", String.class).invoke(serializer, input);
            if (component instanceof Component parsed) {
                return parsed;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {

        }
        return Component.text(input);
    }

    private Component parseJson(String input) {
        try {
            Class<?> serializerClass = Class.forName("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
            Object serializer = serializerClass.getMethod("gson").invoke(null);
            Object component = serializerClass.getMethod("deserialize", String.class).invoke(serializer, input);
            if (component instanceof Component parsed) {
                return parsed;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {

        }
        return Component.text(input);
    }

    private static boolean hasPrefix(String input, String prefix) {
        return input.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static String stripPrefix(String input) {
        int separatorIndex = input.indexOf(':');
        if (separatorIndex < 0 || separatorIndex + 1 >= input.length()) {
            return "";
        }
        return input.substring(separatorIndex + 1);
    }
}
