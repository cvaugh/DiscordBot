package dev.cvaugh.discordbot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuildSettings {
    public long id;
    public long roleToCreatePoll = 0;
    public long roleToMute = 0;
    public long mutedRole = 0;
    public int defaultEmbedColor = Integer.MAX_VALUE;
    public long autoAssignRole = 0;
    public Map<Long, Long> muted = new HashMap<>();
    public boolean joinMessages = false;
    public long joinMessageChannel = 0;
    public boolean leaveMessages = false;
    public long leaveMessageChannel = 0;

    public Color getEmbedColor() {
        return defaultEmbedColor == Integer.MAX_VALUE ?
                Config.getEmbedColor() :
                new Color(defaultEmbedColor);
    }

    public boolean isMuted(long userId) {
        if(muted.containsKey(userId) && muted.get(userId) < System.currentTimeMillis()) {
            unmute(userId);
        }
        return muted.containsKey(userId);
    }

    public void mute(long userId, long until) {
        muted.put(userId, until);
        Guild guild = Main.jda.getGuildById(id);
        if(guild == null)
            return;
        Member member = guild.getMemberById(userId);
        if(member == null)
            return;
        Role role = guild.getRoleById(mutedRole);
        if(role != null) {
            guild.modifyMemberRoles(member, List.of(role), Collections.emptyList()).queue();
        }
        Main.scheduleMuteTask(id, userId, until);
        try {
            Main.writeGuildSettings(id);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void unmute(long userId) {
        if(muted.containsKey(userId)) {
            muted.remove(userId);
            Guild guild = Main.jda.getGuildById(id);
            if(guild == null)
                return;
            Member member = guild.getMemberById(userId);
            if(member == null)
                return;
            Role role = guild.getRoleById(mutedRole);
            if(role != null) {
                guild.modifyMemberRoles(member, Collections.emptyList(), List.of(role)).queue();
            }
            try {
                Main.writeGuildSettings(id);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void rescheduleMuteTasks() {
        List<Long> toRemove = new ArrayList<>();
        for(long userId : muted.keySet()) {
            if(muted.get(userId) < System.currentTimeMillis()) {
                toRemove.add(userId);
                continue;
            }
            Main.scheduleMuteTask(id, userId, muted.get(userId));
        }
        for(long userId : toRemove) {
            muted.remove(userId);
        }
    }

    public void save() {
        try {
            Main.writeGuildSettings(id);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}
