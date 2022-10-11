package dev.cvaugh.discordbot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

public final class Utils {
    public static long durationToTimestamp(String duration, long relativeTo) {
        String[] split = duration.split(" ");
        long years = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
        for(String s : split) {
            char c = Character.toLowerCase(s.charAt(s.length() - 1));
            s = s.substring(0, s.length() - 1);
            switch(c) {
            case 'y' -> {
                years += Integer.parseInt(s);
            }
            case 'd' -> {
                days += Integer.parseInt(s);
            }
            case 'h' -> {
                hours += Integer.parseInt(s);
            }
            case 'm' -> {
                minutes += Integer.parseInt(s);
            }
            case 's' -> {
                seconds += Integer.parseInt(s);
            }
            default -> {return 0L;}
            }
        }
        return relativeTo + seconds * 1000L + minutes * 60000L + hours * 3600000L +
                days * 86400000L + years * 31536000000L;
    }

    public static long durationToTimestamp(String duration) {
        return durationToTimestamp(duration, System.currentTimeMillis());
    }

    public static RichCustomEmoji getGuildEmoji(String s, long guildId) {
        Guild guild = Main.jda.getGuildById(guildId);
        if(guild == null) {
            return null;
        } else if(s.startsWith("<:") && s.endsWith(">")) {
            return guild.getEmojiById(s.substring(s.lastIndexOf(':') + 1, s.length() - 1));
        } else {
            return null;
        }
    }
}
