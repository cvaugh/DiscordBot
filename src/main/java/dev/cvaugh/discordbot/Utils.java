package dev.cvaugh.discordbot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Utils {
    public static long durationToTimestamp(String duration, long relativeTo) {
        String[] split = duration.split(" ");
        long years = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
        for(String s : split) {
            char c = Character.toLowerCase(s.charAt(s.length() - 1));
            s = s.substring(0, s.length() - 1);
            switch(c) {
            case 'y' -> years += Integer.parseInt(s);
            case 'd' -> days += Integer.parseInt(s);
            case 'h' -> hours += Integer.parseInt(s);
            case 'm' -> minutes += Integer.parseInt(s);
            case 's' -> seconds += Integer.parseInt(s);
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

    /**
     * @see <a href="https://stackoverflow.com/a/2581754">Stack Overflow</a>
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for(Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public static boolean isEmoji(String s) {
        if(s.length() == 0)
            return false;
        Character.UnicodeBlock block = Character.UnicodeBlock.of(s.charAt(0));
        if(block == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS ||
                block == Character.UnicodeBlock.DINGBATS ||
                block == Character.UnicodeBlock.ARROWS) {
            return true;
        } else if(Character.isHighSurrogate(s.charAt(0)) && s.length() > 1 &&
                Character.isSurrogatePair(s.charAt(0), s.charAt(1))) {
            block = Character.UnicodeBlock.of(surrogatesToCharacter(s.charAt(0), s.charAt(1)));
            return block == Character.UnicodeBlock.ENCLOSED_ALPHANUMERIC_SUPPLEMENT ||
                    block == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS;
        } else {
            return false;
        }
    }

    public static int surrogatesToCharacter(char high, char low) {
        return (((int) high - 0xD800) << 8) + ((int) low - 0xDC00) + 0xD800 + 0xDC00;
    }
}
