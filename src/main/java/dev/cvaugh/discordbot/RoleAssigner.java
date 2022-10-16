package dev.cvaugh.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoleAssigner {
    public static final Map<Long, RoleAssigner> REGISTRY = new HashMap<>();
    public long id;
    public long guildId;
    public long channelId;
    public String title;
    public List<String> labels;
    public List<Long> roles;
    public String imageURL = "";
    public int accentColor = Integer.MAX_VALUE;

    public RoleAssigner(long guildId, long channelId, String title, List<String> labels,
            List<Long> roles) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.title = title;
        this.labels = labels;
        this.roles = roles;
    }

    public MessageEmbed build() {
        Guild guild = Main.jda.getGuildById(guildId);
        if(guild == null) {
            deleteRoleAssigner(id);
            return null;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(accentColor == Integer.MAX_VALUE ?
                Guilds.get(guildId).getEmbedColor() :
                new Color(accentColor));
        eb.setTitle(title);
        eb.setDescription("React to this message to get your roles.");

        if(!imageURL.isEmpty()) {
            eb.setThumbnail(imageURL);
        }

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < roles.size(); i++) {
            sb.append(labels.get(i));
            sb.append(" **");
            Role role = guild.getRoleById(roles.get(i));
            sb.append(role == null ? "?" : role.getName());
            sb.append("**\n\n");
        }

        eb.addField("", sb.toString().trim(), false);
        return eb.build();
    }

    public void save() {
        try {
            Main.writeRoleAssigner(this);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return "<role-assigner:" + id + ">";
    }

    public static void deleteRoleAssigner(long id) {
        if(!REGISTRY.containsKey(id))
            return;
        REGISTRY.remove(id);
        Main.deleteRoleAssigner(id);
    }
}
