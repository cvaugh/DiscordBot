package dev.cvaugh.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Poll {
    public static final String[] DEFAULT_LABELS = {};
    public long id;
    public long createdAt;
    public long guildId;
    public long channelId;
    public long creatorId;
    public String title;
    public List<String> labels;
    public List<String> options;
    public Map<Long, Integer> results;
    public long endTime = 0;
    public boolean announceResults = false;
    public boolean closed = false;

    public Poll(long guildId, long channelId, long creatorId, String title, List<String> labels,
            List<String> options) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.creatorId = creatorId;
        this.createdAt = System.currentTimeMillis();
        this.title = title;
        this.labels = labels;
        this.options = options;
        this.results = new HashMap<>();
    }

    public MessageEmbed build() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(new Color(0x4372AA));
        eb.setTitle("Poll: " + title);

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < options.size(); i++) {
            sb.append(labels.get(i));
            sb.append(" ");
            sb.append(options.get(i));
            sb.append("\n\n");
        }

        eb.addField("", sb.toString().trim(), false);
        eb.addField("",
                String.format("Created %s\nPoll %s %s", TimeFormat.RELATIVE.format(createdAt),
                        endTime < System.currentTimeMillis() ? "ended" : "ends",
                        TimeFormat.RELATIVE.format(endTime)), false);
        return eb.build();
    }

    public boolean update() {
        Guild guild = Main.jda.getGuildById(guildId);
        if(guild == null)
            return false;
        TextChannel channel = guild.getChannelById(TextChannel.class, channelId);
        if(channel == null)
            return false;
        channel.editMessageEmbedsById(id, build()).queue();
        return true;
    }
}
