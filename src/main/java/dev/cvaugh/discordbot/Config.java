package dev.cvaugh.discordbot;

import java.util.List;

public class Config {
    public static Config instance = new Config();

    public String botToken = "YOUR TOKEN HERE";
    public List<String> defaultPollLabels =
            List.of("\uD83C\uDDE6", "\uD83C\uDDE7", "\uD83C\uDDE8", "\uD83C\uDDE9", "\uD83C\uDDEA",
                    "\uD83C\uDDEB", "\uD83C\uDDEC", "\uD83C\uDDED", "\uD83C\uDDEE", "\uD83C\uDDEF");

    public static String getBotToken() {
        return instance.botToken;
    }

    public static String getDefaultPollLabel(int index) {
        if(index >= 0 && index < instance.defaultPollLabels.size()) {
            return instance.defaultPollLabels.get(index);
        } else {
            return "\u2753";
        }
    }
}
