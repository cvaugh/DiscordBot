package dev.cvaugh.discordbot;

import java.awt.Color;
import java.util.List;

public class Config {
    public static Config instance = new Config();

    public String botToken = "YOUR TOKEN HERE";
    public List<String> defaultEmojiLabels =
            List.of("\uD83C\uDDE6", "\uD83C\uDDE7", "\uD83C\uDDE8", "\uD83C\uDDE9", "\uD83C\uDDEA",
                    "\uD83C\uDDEB", "\uD83C\uDDEC", "\uD83C\uDDED", "\uD83C\uDDEE", "\uD83C\uDDEF");
    public int maxCoinFlips = 100;
    public String defaultEmbedColor = "4372AA";
    public long updateTimerPeriod = 300;

    public static String getBotToken() {
        return instance.botToken;
    }

    public static String getDefaultEmojiLabel(int index) {
        if(index >= 0 && index < instance.defaultEmojiLabels.size()) {
            return instance.defaultEmojiLabels.get(index);
        } else {
            return "\u2753";
        }
    }

    public static int getMaxCoinFlips() {
        return instance.maxCoinFlips;
    }

    public static Color getEmbedColor() {
        try {
            return new Color(Integer.parseInt(instance.defaultEmbedColor, 16));
        } catch(NumberFormatException e) {
            Logger.warn("Invalid color code (defaultEmbedColor): %s", instance.defaultEmbedColor);
            return Color.DARK_GRAY;
        }
    }
}
