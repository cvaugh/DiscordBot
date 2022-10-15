package dev.cvaugh.discordbot;

import java.awt.Color;

public class GuildSettings {
    public long id;
    public long roleToCreatePoll = 0;
    public int defaultPollEmbedColor = Integer.MAX_VALUE;

    public Color getPollEmbedColor() {
        return defaultPollEmbedColor == Integer.MAX_VALUE ?
                Config.getPollEmbedColor() :
                new Color(defaultPollEmbedColor);
    }
}
