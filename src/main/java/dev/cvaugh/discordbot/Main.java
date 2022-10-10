package dev.cvaugh.discordbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final File DATA_DIR = new File("bot");
    private static final File CONFIG_FILE = new File(DATA_DIR, "config.properties");
    private static final Map<String, String> CONFIG = new HashMap<>() {
        {
            put("bot-token", "YOUR TOKEN HERE");
            put("command-prefix", "^");
        }
    };
    public static JDA jda;

    public static void main(String[] args) {
        try {
            loadConfig();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        JDABuilder builder = JDABuilder.createDefault(CONFIG.get("bot-token"));
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.MESSAGE_CONTENT);
        jda = builder.build();
        jda.addEventListener(new DiscordListener());
    }

    private static void loadConfig() throws IOException {
        if(!DATA_DIR.exists()) {
            DATA_DIR.mkdirs();
        }
        if(!CONFIG_FILE.exists()) {
            writeDefaultConfig();
        }
        List<String> lines = Files.readAllLines(CONFIG_FILE.toPath());
        for(String line : lines) {
            if(!(line.isBlank() || line.startsWith("#"))) {
                String[] split = line.split("=", 2);
                CONFIG.put(split[0].toLowerCase(), split[1].replace("\\n", "\n"));
            }
        }
    }

    private static void writeDefaultConfig() throws IOException {
        Files.write(CONFIG_FILE.toPath(), configToString().getBytes());
    }

    private static String configToString() {
        StringBuilder sb = new StringBuilder();
        for(String key : CONFIG.keySet()) {
            sb.append(key);
            sb.append('=');
            sb.append(CONFIG.get(key).replace("\n", "\\n"));
            sb.append('\n');
        }
        return sb.toString();
    }

    public static String commandPrefix() {
        return CONFIG.getOrDefault("command-prefix", "^");
    }
}
