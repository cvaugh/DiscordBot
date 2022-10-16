package dev.cvaugh.discordbot;

import java.awt.Color;

public class GuildSettings {
    public long id;
    public long roleToCreatePoll = 0;
    public int defaultEmbedColor = Integer.MAX_VALUE;

    public Color getEmbedColor() {
        return defaultEmbedColor == Integer.MAX_VALUE ?
                Config.getEmbedColor() :
                new Color(defaultEmbedColor);
    }
}
