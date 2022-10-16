package dev.cvaugh.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final TimerTask UPDATE_TASK = new TimerTask() {
        @Override
        public void run() {
            logger.debug("Running update task");
            for(Poll poll : Poll.REGISTRY.values()) {
                poll.update();
            }
        }
    };
    public static Logger logger;
    public static JDA jda;
    public static Gson gson;
    public static String helpText = "";

    public static void main(String[] args) {
        logger = LoggerFactory.getLogger("Bot");
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
        logger.debug("Building JDA instance");
        JDABuilder builder = JDABuilder.createDefault(Config.getBotToken());
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.MESSAGE_CONTENT);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        jda = builder.build();
        logger.debug("Registering commands");
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
                Commands.slash("mute", "Mutes a user")
                        .addOption(OptionType.USER, "user", "The user to mute", true)
                        .addOption(OptionType.STRING, "time", "How long to mute the user", true),
                Commands.slash("unmute", "Unmutes a muted user")
                        .addOption(OptionType.USER, "user", "The user to unmute", true),
                // TODO add/remove role command
                Commands.slash("settings", "Modifies server settings for the bot")
                        .addOption(OptionType.ROLE, "role-to-poll",
                                "Users with this role can create polls", false)
                        .addOption(OptionType.ROLE, "role-to-mute",
                                "Users with this role can mute other users", false)
                        .addOption(OptionType.ROLE, "muted-role",
                                "The role to assign to muted users", false)
                        .addOption(OptionType.STRING, "default-poll-color",
                                "The default hexadecimal accent color for poll embeds (e.g. 4372AA)",
                                false).addOption(OptionType.ROLE, "auto-assign-role",
                                "Automatically assign this role to new users when they join", false)
                        .addOption(OptionType.BOOLEAN, "join-messages",
                                "Send a message when a new user joins the server", false)
                        .addOption(OptionType.CHANNEL, "join-message-channel",
                                "The channel in which to send join messages", false)
                        .addOption(OptionType.BOOLEAN, "leave-messages",
                                "Send a message when a user leaves the server", false)
                        .addOption(OptionType.CHANNEL, "leave-message-channel",
                                "The channel in which to send leave messages", false)).queue();
        logger.debug("Registering shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Saving guild settings");
            Guilds.getAll().forEach(GuildSettings::save);
            logger.info("Saving polls");
            Poll.REGISTRY.values().forEach(Poll::save);
            logger.info("Saving role assigners");
            RoleAssigner.REGISTRY.values().forEach(RoleAssigner::save);
            logger.info("Shutting down");
        }));
    }

    private static void loadConfig() throws IOException {
        logger.info("Loading config");
        if(!CONFIG_DIR.exists()) {
            logger.debug("Creating missing config directory at '{}'", CONFIG_DIR.getAbsolutePath());
            if(!CONFIG_DIR.mkdirs()) {
                logger.error("Failed to create config directory at '{}'",
                        CONFIG_DIR.getAbsolutePath());
                System.exit(1);
            }
        }
        if(!CONFIG_FILE.exists()) {
            writeDefaultConfig();
            logger.error("Please enter your bot token in '{}'", CONFIG_FILE.getAbsolutePath());
            System.exit(1);
        }
        String json = Files.readString(CONFIG_FILE.toPath(), StandardCharsets.UTF_8);
        Config.instance = gson.fromJson(json, Config.class);
    }

    private static void writeDefaultConfig() throws IOException {
        logger.info("Writing default config to '{}'", CONFIG_FILE.getAbsolutePath());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(CONFIG_FILE.toPath(), gson.toJson(Config.instance));
    }

    private static void readHelpText() throws IOException {
        logger.debug("Reading help message from /help.md");
        InputStream in = Main.class.getResourceAsStream("/help.md");
        if(in == null)
            return;
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        helpText = String.join("\n", reader.lines().collect(Collectors.joining()));
        in.close();
    }

    private static void loadPolls() throws IOException {
        logger.info("Loading polls");
        if(!POLLS_DIR.exists())
            return;
        File[] files = POLLS_DIR.listFiles();
        if(files == null)
            return;
        for(File file : files) {
            logger.debug("Loading poll {}", file.getName());
            Poll poll = gson.fromJson(Files.readString(file.toPath(), StandardCharsets.UTF_8),
                    Poll.class);
            Poll.REGISTRY.put(poll.id, poll);
        }
    }

    public static void writePoll(Poll poll) throws IOException {
        logger.debug("Writing poll {}", poll.id);
        if(!POLLS_DIR.exists() && !POLLS_DIR.mkdir()) {
            logger.error("Failed to create polls directory at '{}'", POLLS_DIR.getAbsolutePath());
            System.exit(1);
        }
        File file = new File(POLLS_DIR, poll.id + ".json");
        Files.writeString(file.toPath(), gson.toJson(poll));
    }

    public static void deletePoll(long id) {
        logger.debug("Deleting poll {}", id);
        File file = new File(POLLS_DIR, id + ".json");
        if(file.exists() && !file.delete()) {
            logger.warn("Failed to delete poll {} ({})", id, file.getAbsolutePath());
        }
    }

    private static void loadRoleAssigners() throws IOException {
        logger.info("Loading role assigners");
        if(!ROLE_ASSIGNERS_DIR.exists())
            return;
        File[] files = ROLE_ASSIGNERS_DIR.listFiles();
        if(files == null)
            return;
        for(File file : files) {
            logger.debug("Loading role assigner {}", file.getName());
            RoleAssigner roleAssigner =
                    gson.fromJson(Files.readString(file.toPath(), StandardCharsets.UTF_8),
                            RoleAssigner.class);
            RoleAssigner.REGISTRY.put(roleAssigner.id, roleAssigner);
        }
    }

    public static void writeRoleAssigner(RoleAssigner roleAssigner) throws IOException {
        logger.debug("Writing role assigner {}", roleAssigner.id);
        if(!ROLE_ASSIGNERS_DIR.exists() && !ROLE_ASSIGNERS_DIR.mkdir()) {
            logger.error("Failed to create role assigners directory at '{}'",
                    ROLE_ASSIGNERS_DIR.getAbsolutePath());
            System.exit(1);
        }
        File file = new File(ROLE_ASSIGNERS_DIR, roleAssigner.id + ".json");
        Files.writeString(file.toPath(), gson.toJson(roleAssigner));
    }

    public static void deleteRoleAssigner(long id) {
        logger.debug("Deleting role assigner {}", id);
        File file = new File(ROLE_ASSIGNERS_DIR, id + ".json");
        if(file.exists() && !file.delete()) {
            logger.warn("Failed to delete role assigner {} ({})", id, file.getAbsolutePath());
        }
    }

    private static void loadGuilds() throws IOException {
        logger.info("Loading guilds");
        if(!GUILDS_DIR.exists() && !GUILDS_DIR.mkdir()) {
            logger.error("Failed to guild data directory at '{}'", GUILDS_DIR.getAbsolutePath());
            System.exit(1);
        }
        File[] files = GUILDS_DIR.listFiles();
        if(files == null)
            return;
        for(File file : files) {
            if(file.isDirectory()) {
                logger.debug("Loading guild " + file.getName());
                File settingsFile = new File(file, "settings.json");
                GuildSettings settings = gson.fromJson(
                        Files.readString(settingsFile.toPath(), StandardCharsets.UTF_8),
                        GuildSettings.class);
                Guilds.put(settings.id, settings, false);
                settings.rescheduleMuteTasks();
            }
        }
    }

    private static File getGuildDir(long guildId) {
        File dir = new File(GUILDS_DIR, String.valueOf(guildId));
        if(!dir.exists() && !dir.mkdir()) {
            logger.error("Failed to create directory for guild {} at '{}'", guildId,
                    dir.getAbsolutePath());
            System.exit(1);
        }
        return dir;
    }

    public static void writeGuildSettings(long guildId) throws IOException {
        logger.debug("Writing settings for guild {}", guildId);
        Files.writeString(new File(getGuildDir(guildId), "settings.json").toPath(),
                gson.toJson(Guilds.get(guildId)));
    }

    public static void scheduleUpdateTask() {
        logger.debug("Scheduling update task");
        TIMER.schedule(UPDATE_TASK, 0L, Config.instance.updateTimerPeriod * 1000L);
    }

    public static void scheduleMuteTask(long guildId, long userId, long endTime) {
        logger.debug("Scheduling mute task [{}, {}, {}]", guildId, userId, endTime);
        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                GuildSettings guildSettings = Guilds.get(guildId);
                if(guildSettings == null)
                    return;
                guildSettings.unmute(userId);
            }
        }, endTime - System.currentTimeMillis());
    }
}
