package dev.cvaugh.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public class Main {
    private static final File DATA_DIR = new File("bot");
    private static final File CONFIG_FILE = new File(DATA_DIR, "config.json");
    private static Map<String, String> config =
            Map.of("bot-token", "YOUR TOKEN HERE", "command-prefix", "^");
    public static JDA jda;
    public static Gson gson;

    public static void main(String[] args) {
        gson = new Gson();
        try {
            loadConfig();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        JDABuilder builder = JDABuilder.createDefault(config.get("bot-token"));
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
            Logger.error("Please enter your bot token in %s", CONFIG_FILE.getAbsolutePath());
            System.exit(1);
        }
        TypeToken<Map<String, String>> mapType = new TypeToken<>() {};
        String json = Files.readString(CONFIG_FILE.toPath(), StandardCharsets.UTF_8);
        config = gson.fromJson(json, mapType.getType());
    }

    private static void writeDefaultConfig() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.write(CONFIG_FILE.toPath(), gson.toJson(config).getBytes());
    }

    public static String commandPrefix() {
        return config.getOrDefault("command-prefix", "^");
    }
}
