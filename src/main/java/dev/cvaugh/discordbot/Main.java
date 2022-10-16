package dev.cvaugh.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class Main {
    private static final File CONFIG_DIR = new File("bot");
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "config.json");
    private static final File POLLS_DIR = new File(CONFIG_DIR, "polls");
    private static final File ROLE_ASSIGNERS_DIR = new File(CONFIG_DIR, "role-assigners");
    private static final File GUILDS_DIR = new File(CONFIG_DIR, "guilds");
    private static final Timer TIMER = new Timer();
    private static final long POLL_UPDATE_FREQUENCY = 300000L;
    private static final TimerTask UPDATE_TASK = new TimerTask() {
        @Override
        public void run() {
            for(Poll poll : Poll.REGISTRY.values()) {
                poll.update();
            }
        }
    };
    public static JDA jda;
    public static Gson gson;
    public static String helpText = "";

    public static void main(String[] args) {
        gson = new Gson();
        try {
            readHelpText();
            loadConfig();
            loadGuilds();
            loadPolls();
            loadRoleAssigners();
        } catch(IOException e) {
            e.printStackTrace();
        }
        JDABuilder builder = JDABuilder.createDefault(Config.getBotToken());
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.MESSAGE_CONTENT);
        jda = builder.build();
        jda.addEventListener(new DiscordListener());
        jda.updateCommands().addCommands(Commands.slash("help", "Shows the bot's documentation"),
                        Commands.slash("flipacoin", "Flips a coin")
                                .addOption(OptionType.INTEGER, "count", "How many coins to flip", false),
                        Commands.slash("ping",
                                "Shows how long it takes for the bot to receive your messages"),
                        Commands.slash("poll", "Creates a poll")
                                .addOption(OptionType.STRING, "title", "The title of the poll", true)
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
                                        "The amount of time until the poll stops accepting new responses",
                                        false).addOption(OptionType.BOOLEAN, "announce",
                                        "Whether the winning option(s) should be announced when the poll ends",
                                        false).addOption(OptionType.STRING, "color",
                                        "The hexadecimal accent color of the poll's embed (e.g. 4372AA)"),
                        Commands.slash("role-assigner",
                                        "Allows users to self-assign roles by reacting to a message")
                                .addOption(OptionType.STRING, "title", "The title of the role assigner",
                                        true).addOption(OptionType.ROLE, "role1", "Role 1", true)
                                .addOption(OptionType.ROLE, "role2", "Role 2", false)
                                .addOption(OptionType.ROLE, "role3", "Role 3", false)
                                .addOption(OptionType.ROLE, "role4", "Role 4", false)
                                .addOption(OptionType.ROLE, "role5", "Role 5", false)
                                .addOption(OptionType.ROLE, "role6", "Role 6", false)
                                .addOption(OptionType.ROLE, "role7", "Role 7", false)
                                .addOption(OptionType.ROLE, "role8", "Role 8", false)
                                .addOption(OptionType.ROLE, "role9", "Role 9", false)
                                .addOption(OptionType.ROLE, "role10", "Role 10", false)
                                .addOption(OptionType.STRING, "label1", "Emoji label for role 1", false)
                                .addOption(OptionType.STRING, "label2", "Emoji label for role 2", false)
                                .addOption(OptionType.STRING, "label3", "Emoji label for role 3", false)
                                .addOption(OptionType.STRING, "label4", "Emoji label for role 4", false)
                                .addOption(OptionType.STRING, "label5", "Emoji label for role 5", false)
                                .addOption(OptionType.STRING, "label6", "Emoji label for role 6", false)
                                .addOption(OptionType.STRING, "label7", "Emoji label for role 7", false)
                                .addOption(OptionType.STRING, "label8", "Emoji label for role 8", false)
                                .addOption(OptionType.STRING, "label9", "Emoji label for role 9", false)
                                .addOption(OptionType.STRING, "label10", "Emoji label for role 10", false)
                                .addOption(OptionType.STRING, "color",
                                        "The hexadecimal accent color of the role assigner's embed (e.g. 4372AA)")
                                .addOption(OptionType.STRING, "image",
                                        "The URL of an image to use as a thumbnail"),
                        Commands.slash("settings", "Modifies server settings for the bot")
                                .addOption(OptionType.ROLE, "poll-role",
                                        "Users with this role can create polls.", false)
                                .addOption(OptionType.STRING, "default-poll-color",
                                        "The default hexadecimal accent color for poll embeds (e.g. 4372AA)",
                                        false).addOption(OptionType.ROLE, "auto-assign-role",
                                        "Automatically assign this role to new users when they join", false))
                .queue();
    }

    private static void loadConfig() throws IOException {
        Logger.info("Loading config");
        if(!CONFIG_DIR.exists()) {
            if(!CONFIG_DIR.mkdirs()) {
                Logger.error("Failed to create config directory at '%s'",
                        CONFIG_DIR.getAbsolutePath());
                System.exit(1);
            }
        }
        if(!CONFIG_FILE.exists()) {
            writeDefaultConfig();
            Logger.error("Please enter your bot token in '%s'", CONFIG_FILE.getAbsolutePath());
            System.exit(1);
        }
        String json = Files.readString(CONFIG_FILE.toPath(), StandardCharsets.UTF_8);
        Config.instance = gson.fromJson(json, Config.class);
    }

    private static void writeDefaultConfig() throws IOException {
        Logger.info("Writing default config");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(CONFIG_FILE.toPath(), gson.toJson(Config.instance));
    }

    private static void readHelpText() throws IOException {
        InputStream in = Main.class.getResourceAsStream("/help.md");
        if(in == null)
            return;
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        helpText = String.join("\n", reader.lines().collect(Collectors.joining()));
        in.close();
    }

    private static void loadPolls() throws IOException {
        Logger.info("Loading polls");
        if(!POLLS_DIR.exists())
            return;
        for(File file : POLLS_DIR.listFiles()) {
            Poll poll = gson.fromJson(Files.readString(file.toPath(), StandardCharsets.UTF_8),
                    Poll.class);
            Poll.REGISTRY.put(poll.id, poll);
        }
    }

    public static void writePoll(Poll poll) throws IOException {
        if(!POLLS_DIR.exists() && !POLLS_DIR.mkdir()) {
            Logger.error("Failed to create polls directory at '%s'", POLLS_DIR.getAbsolutePath());
            System.exit(1);
        }
        File file = new File(POLLS_DIR, poll.id + ".json");
        Files.writeString(file.toPath(), gson.toJson(poll));
    }

    public static boolean deletePoll(long id) {
        File file = new File(POLLS_DIR, id + ".json");
        if(file.exists())
            return file.delete();
        return true;
    }

    private static void loadRoleAssigners() throws IOException {
        Logger.info("Loading role assigners");
        if(!ROLE_ASSIGNERS_DIR.exists())
            return;
        for(File file : ROLE_ASSIGNERS_DIR.listFiles()) {
            RoleAssigner roleAssigner =
                    gson.fromJson(Files.readString(file.toPath(), StandardCharsets.UTF_8),
                            RoleAssigner.class);
            RoleAssigner.REGISTRY.put(roleAssigner.id, roleAssigner);
        }
    }

    public static void writeRoleAssigner(RoleAssigner roleAssigner) throws IOException {
        if(!ROLE_ASSIGNERS_DIR.exists() && !ROLE_ASSIGNERS_DIR.mkdir()) {
            Logger.error("Failed to create role assigners directory at '%s'",
                    ROLE_ASSIGNERS_DIR.getAbsolutePath());
            System.exit(1);
        }
        File file = new File(ROLE_ASSIGNERS_DIR, roleAssigner.id + ".json");
        Files.writeString(file.toPath(), gson.toJson(roleAssigner));
    }

    public static boolean deleteRoleAssigner(long id) {
        File file = new File(ROLE_ASSIGNERS_DIR, id + ".json");
        if(file.exists())
            return file.delete();
        return true;
    }

    private static void loadGuilds() throws IOException {
        if(!GUILDS_DIR.exists() && !GUILDS_DIR.mkdir()) {
            Logger.error("Failed to guild data directory at '%s'", GUILDS_DIR.getAbsolutePath());
            System.exit(1);
        }
        for(File file : GUILDS_DIR.listFiles()) {
            if(file.isDirectory()) {
                File settingsFile = new File(file, "settings.json");
                GuildSettings settings = gson.fromJson(
                        Files.readString(settingsFile.toPath(), StandardCharsets.UTF_8),
                        GuildSettings.class);
                Guilds.put(settings.id, settings, false);
            }
        }
    }

    private static File getGuildDir(long guildId) {
        File dir = new File(GUILDS_DIR, String.valueOf(guildId));
        if(!dir.exists() && !dir.mkdir()) {
            Logger.error("Failed to create directory for guild %d at '%s'", guildId,
                    dir.getAbsolutePath());
            System.exit(1);
        }
        return dir;
    }

    public static void writeGuildSettings(long guildId) throws IOException {
        Files.writeString(new File(getGuildDir(guildId), "settings.json").toPath(),
                gson.toJson(Guilds.get(guildId)));
    }

    public static void startUpdateTask() {
        TIMER.schedule(UPDATE_TASK, 0L, Config.instance.updateTimerPeriod * 1000L);
    }
}
