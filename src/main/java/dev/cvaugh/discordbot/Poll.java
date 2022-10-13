package dev.cvaugh.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Poll {
    public static final Map<Long, Poll> POLLS = new HashMap<>();
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
        eb.addField("", String.format("Created %s by %s%s", TimeFormat.RELATIVE.format(createdAt),
                "<@" + creatorId + ">", endTime == 0 ?
                        "" :
                        String.format("\nPoll %s %s",
                                endTime < System.currentTimeMillis() ? "ended" : "ends",
                                TimeFormat.RELATIVE.format(endTime))), false);
        return eb.build();
    }

    public void update() {
        TextChannel channel = Main.jda.getChannelById(TextChannel.class, channelId);
        if(channel == null)
            return;
        if(!closed && ends() && endTime < System.currentTimeMillis()) {
            closed = true;
            if(announceResults) {
                StringBuilder sb = new StringBuilder();
                Map<Integer, Integer> counts = new HashMap<>();
                for(long key : results.keySet()) {
                    counts.put(results.get(key), counts.getOrDefault(results.get(key), 0) + 1);
                }
                int max = 0;
                for(int i : counts.keySet()) {
                    if(counts.get(i) > max) {
                        max = counts.get(i);
                    }
                }
                counts = Utils.sortByValue(counts);
                for(int i : counts.keySet()) {
                    if(counts.get(i) == max) {
                        sb.append("**");
                    }
                    sb.append(labels.get(i));
                    sb.append(' ');
                    sb.append(options.get(i));
                    sb.append(": ");
                    if(counts.get(i) == max) {
                        sb.append("**");
                    }
                    sb.append(counts.get(i));
                    sb.append(" response");
                    if(counts.get(i) % 2 == 0) {
                        sb.append("s");
                    }
                    sb.append('\n');
                }
                channel.sendMessage("**Poll results:**\n" + sb.toString().trim())
                        .setMessageReference(id).queue();
            }
        }
        channel.editMessageEmbedsById(id, build()).queue();
        save();
    }

    public void save() {
        try {
            Main.writePoll(this);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public boolean ends() {
        return endTime > 0;
    }

    public String toString() {
        return "<poll:" + id + ">";
    }
}
