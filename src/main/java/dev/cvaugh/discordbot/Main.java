package dev.cvaugh.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public class Main {
    private static final File DATA_DIR = new File("bot");
    private static final File CONFIG_FILE = new File(DATA_DIR, "config.json");
    private static final File POLLS_DIR = new File(DATA_DIR, "polls");
    private static Map<String, String> config =
            Map.of("botToken", "YOUR TOKEN HERE", "commandPrefix", "^");
    public static JDA jda;
    public static Gson gson;

    public static void main(String[] args) {
        gson = new Gson();
        try {
            loadConfig();
            loadPolls();
        } catch(IOException e) {
            e.printStackTrace();
        }
        JDABuilder builder = JDABuilder.createDefault(config.get("botToken"));
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.MESSAGE_CONTENT);
        jda = builder.build();
        jda.addEventListener(new DiscordListener());
        jda.updateCommands().addCommands(
                Commands.slash("help", "Sends the bot's help information to you in a DM."),
                Commands.slash("flipacoin", "Flips a coin.")
                        .addOption(OptionType.INTEGER, "count", "How many coins to flip.", false),
                Commands.slash("ping",
                        "Shows how long it takes for the bot to receive your messages."),
                Commands.slash("poll", "Creates a poll.")
                        .addOption(OptionType.STRING, "title", "The title of the poll.", true)
                        .addOption(OptionType.STRING, "option1", "Poll option 1", true)
                        .addOption(OptionType.STRING, "option2", "Poll option 2", true)
                        .addOption(OptionType.STRING, "option3", "Poll option 3", false)
                        .addOption(OptionType.STRING, "option4", "Poll option 4", false)
                        .addOption(OptionType.STRING, "option5", "Poll option 5", false)
                        .addOption(OptionType.STRING, "option6", "Poll option 6", false)
                        .addOption(OptionType.STRING, "option7", "Poll option 7", false)
                        .addOption(OptionType.STRING, "option8", "Poll option 8", false)
                        .addOption(OptionType.STRING, "option9", "Poll option 9", false)
                        .addOption(OptionType.STRING, "option10", "Poll option 10", false)
                        .addOption(OptionType.STRING, "label1", "Emoji label for option 1", false)
                        .addOption(OptionType.STRING, "label2", "Emoji label for option 2", false)
                        .addOption(OptionType.STRING, "label3", "Emoji label for option 3", false)
                        .addOption(OptionType.STRING, "label4", "Emoji label for option 4", false)
                        .addOption(OptionType.STRING, "label5", "Emoji label for option 5", false)
                        .addOption(OptionType.STRING, "label6", "Emoji label for option 6", false)
                        .addOption(OptionType.STRING, "label7", "Emoji label for option 7", false)
                        .addOption(OptionType.STRING, "label8", "Emoji label for option 8", false)
                        .addOption(OptionType.STRING, "label9", "Emoji label for option 9", false)
                        .addOption(OptionType.STRING, "label10", "Emoji label for option 10", false)
                        .addOption(OptionType.STRING, "duration",
                                "The amount of time until the poll stops accepting new responses.",
                                false).addOption(OptionType.BOOLEAN, "announce",
                                "Whether the winning option(s) should be announced when the poll ends.",
                                false)).queue();
    }

    private static void loadConfig() throws IOException {
        if(!DATA_DIR.exists()) {
            DATA_DIR.mkdirs();
        }
        if(!CONFIG_FILE.exists()) {
            writeDefaultConfig();
            Logger.error("Please enter your bot token in '%s'", CONFIG_FILE.getAbsolutePath());
            System.exit(1);
        }
        TypeToken<Map<String, String>> mapType = new TypeToken<>() {};
        String json = Files.readString(CONFIG_FILE.toPath(), StandardCharsets.UTF_8);
        config = gson.fromJson(json, mapType.getType());
    }

    private static void writeDefaultConfig() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(CONFIG_FILE.toPath(), gson.toJson(config));
    }

    private static void loadPolls() throws IOException {
        if(!POLLS_DIR.exists())
            return;
        for(File file : POLLS_DIR.listFiles()) {
            Poll poll = gson.fromJson(Files.readString(file.toPath(), StandardCharsets.UTF_8),
                    Poll.class);
            Poll.POLLS.put(poll.id, poll);
        }
        System.out.println(Poll.POLLS);
    }

    public static void writePoll(Poll poll) throws IOException {
        if(!POLLS_DIR.exists()) {
            POLLS_DIR.mkdir();
        }
        File file = new File(POLLS_DIR, poll.id + ".json");
        Files.writeString(file.toPath(), gson.toJson(poll));
    }

    public static String getDefaultPollLabel(int index) {
        return config.get("defaultPollLabel" + index);
    }
}
