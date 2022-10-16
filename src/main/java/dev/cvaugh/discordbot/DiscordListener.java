package dev.cvaugh.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DiscordListener extends ListenerAdapter {
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        Logger.info("Ready event received");
        for(Guild guild : Main.jda.getGuilds()) {
            long id = guild.getIdLong();
            if(!Guilds.hasEntry(id)) {
                Logger.warn("Creating missing settings.json for guild %d", id);
                Guilds.put(id);
            }
        }
        Main.schedulePollUpdates();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch(event.getName()) {
        case "help" -> {
            event.reply(Main.helpText).setEphemeral(true).queue();
        }
        case "flipacoin" -> {
            EmbedBuilder eb = new EmbedBuilder();
            OptionMapping count = event.getOption("count");
            int flips = count == null ? 1 : count.getAsInt();
            if(flips < 1 || flips > Config.getMaxCoinFlips()) {
                event.reply("`count` must be between 1 and " + Config.getMaxCoinFlips() +
                        ", inclusive.").setEphemeral(true).queue();
                return;
            } else if(flips == 1) {
                eb.setTitle("\uD83E\uDE99 " +
                        (ThreadLocalRandom.current().nextFloat() > 0.5f ? "Heads!" : "Tails!"));
            } else {
                eb.setTitle("\uD83E\uDE99 Coin Flip");
                StringBuilder sb = new StringBuilder();
                int headsCount = 0;
                int tailsCount = 0;
                for(int i = 0; i < flips; i++) {
                    boolean heads = ThreadLocalRandom.current().nextFloat() > 0.5f;
                    if(heads) {
                        headsCount++;
                    } else {
                        tailsCount++;
                    }
                    sb.append(heads ? "`H` " : "`T` ");
                }
                eb.addField(flips + " flips", sb.toString().trim(), false);
                eb.addField("", String.format("%d heads\n%d tails", headsCount, tailsCount), false);
            }
            event.replyEmbeds(eb.build()).queue();
        }
        case "ping" -> {
            long time = System.currentTimeMillis();
            event.reply("Waiting for response...").setEphemeral(true)
                    .queue(response -> response.editOriginal(String.format("Response time: %d ms",
                            System.currentTimeMillis() - time)).queue());
        }
        case "poll" -> {
            handlePollCommand(event);
        }
        case "role-assigner" -> {
            handleRoleAssignerCommand(event);
        }
        case "settings" -> {
            Guild guild = event.getGuild();
            if(guild == null)
                return;
            if(event.getUser().getIdLong() != guild.getOwnerIdLong()) {
                event.reply("This command can only be used by the server owner.").setEphemeral(true)
                        .queue();
                return;
            }
            GuildSettings settings = Guilds.get(guild.getIdLong());
            boolean noArgs = true;
            OptionMapping pollRole = event.getOption("poll-role");
            if(pollRole != null) {
                noArgs = false;
                Role role = pollRole.getAsRole();
                settings.roleToCreatePoll =
                        role.getAsMention().equals("@everyone") ? 0 : role.getIdLong();
                ;
                event.reply("The role required to create polls is now " + role.getAsMention() + ".")
                        .setEphemeral(true).queue();
            }
            OptionMapping defaultPollColor = event.getOption("default-poll-color");
            if(defaultPollColor != null) {
                noArgs = false;
                String hexColor = defaultPollColor.getAsString();
                if(hexColor.startsWith("#"))
                    hexColor = hexColor.substring(1);
                try {
                    String oldColor =
                            String.format("%X", settings.getEmbedColor().getRGB()).substring(2);
                    settings.defaultEmbedColor = Integer.parseInt(hexColor, 16);
                    event.reply(String.format(
                            "Default poll accent color updated to `%X` (previously `%s`).",
                            settings.defaultEmbedColor, oldColor)).setEphemeral(true).queue();
                } catch(NumberFormatException e) {
                    event.reply("Invalid color code: `" + defaultPollColor.getAsString() + "`")
                            .setEphemeral(true).queue();
                    return;
                }
            }
            OptionMapping autoAssignRole = event.getOption("auto-assign-role");
            if(autoAssignRole != null) {
                noArgs = false;
                Role role = autoAssignRole.getAsRole();
                settings.autoAssignRole =
                        role.getAsMention().equals("@everyone") ? 0 : role.getIdLong();
                event.reply("Auto-assign role set to " + role.getAsMention()).setEphemeral(true)
                        .queue();
            }
            if(noArgs) {
                event.reply("No arguments were supplied.").setEphemeral(true).queue();
            } else {
                try {
                    Guilds.save(guild.getIdLong());
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
        default -> {}
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if(event.getUser() == null || event.getUser().isBot())
            return;
        if(Poll.REGISTRY.containsKey(event.getMessageIdLong())) {
            handlePollReactionAdd(event);
        } else if(RoleAssigner.REGISTRY.containsKey(event.getMessageIdLong())) {
            handleRoleAssignerReactionAdd(event);
        }
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        if(event.getUser() == null || event.getUser().isBot()) {return;}
        if(Poll.REGISTRY.containsKey(event.getMessageIdLong())) {
            handlePollReactionRemove(event);
        } else if(RoleAssigner.REGISTRY.containsKey(event.getMessageIdLong())) {
            handleRoleAssignerReactionRemove(event);
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if(Poll.REGISTRY.containsKey(event.getMessageIdLong())) {
            Poll.deletePoll(event.getMessageIdLong());
        } else if(RoleAssigner.REGISTRY.containsKey(event.getMessageIdLong())) {
            RoleAssigner.deleteRoleAssigner(event.getMessageIdLong());
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Logger.info("Joined guild: %s (ID %d)", event.getGuild().getName(),
                event.getGuild().getIdLong());
        Guilds.put(event.getGuild().getIdLong());
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        long roleId = Guilds.get(event.getGuild().getIdLong()).autoAssignRole;
        Role role = event.getGuild().getRoleById(roleId);
        if(role == null)
            return;
        event.getGuild()
                .modifyMemberRoles(event.getMember(), List.of(role), Collections.emptyList())
                .queue();
    }

    private static void handlePollCommand(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if(guild == null) {
            event.reply("Polls must be created in a server.").setEphemeral(true).queue();
            return;
        }
        long requiredRoleId = Guilds.get(guild.getIdLong()).roleToCreatePoll;
        Member creator = guild.getMemberById(event.getUser().getIdLong());
        if(creator == null) {
            event.reply("You are not a member of this server.").setEphemeral(true).queue();
            return;
        }
        if(requiredRoleId != 0 && creator.getIdLong() != guild.getOwnerIdLong()) {
            Role pollRole = guild.getRoleById(requiredRoleId);
            if(pollRole == null) {
                event.reply("The role required to create polls is missing from the server.\n" +
                        "Please contact the server owner.").setEphemeral(true).queue();
                return;
            }
            boolean canInteract = false;
            for(Role role : creator.getRoles()) {
                if(role.canInteract(pollRole)) {
                    canInteract = true;
                    break;
                }
            }
            if(!canInteract) {
                event.reply("You do not have permission to create polls.").setEphemeral(true)
                        .queue();
                return;
            }
        }
        List<String> labels = new ArrayList<>();
        List<String> options = new ArrayList<>();
        for(int i = 1; i <= 10; i++) {
            OptionMapping option = event.getOption("option" + i);
            if(option == null) {
                break;
            }
            OptionMapping label = event.getOption("label" + i);
            if(label != null) {
                String emoji = label.getAsString();
                Emoji e = Utils.getGuildEmoji(emoji, event.getGuild().getIdLong());
                if(e == null) {
                    if(!Utils.isEmoji(emoji)) {
                        event.reply(
                                        "`label" + i + "` must be an emoji. Invalid value: `" + emoji + "`")
                                .setEphemeral(true).queue();
                        return;
                    }
                }
                labels.add(emoji);
            } else {
                labels.add(Config.getDefaultEmojiLabel(i - 1));
            }
            options.add(option.getAsString());
        }
        OptionMapping title = event.getOption("title");
        if(title == null) {
            event.reply("The poll's title must not be empty.").setEphemeral(true).queue();
            return;
        }
        Poll poll = new Poll(guild.getIdLong(), event.getGuildChannel().getIdLong(),
                creator.getIdLong(), title.getAsString(), labels, options);
        OptionMapping duration = event.getOption("duration");
        if(duration != null) {
            long end = Utils.durationToTimestamp(duration.getAsString());
            if(end == 0L) {
                event.reply("Invalid duration: `" + duration.getAsString() + "`").setEphemeral(true)
                        .queue();
                return;
            }
            poll.endTime = end;
        }
        OptionMapping announce = event.getOption("announce");
        if(announce != null) {
            poll.announceResults = announce.getAsBoolean();
        }
        OptionMapping color = event.getOption("color");
        if(color != null) {
            String accentColor = color.getAsString();
            if(accentColor.startsWith("#"))
                accentColor = accentColor.substring(1);
            try {
                poll.accentColor = Integer.parseInt(accentColor, 16);
            } catch(NumberFormatException e) {
                event.reply("Invalid color code: `" + color.getAsString() + "`").setEphemeral(true)
                        .queue();
                return;
            }
        }

        event.replyEmbeds(poll.build())
                .queue(response -> response.retrieveOriginal().queue(message -> {
                    poll.id = message.getIdLong();
                    Poll.REGISTRY.put(poll.id, poll);
                    for(final String label : poll.labels) {
                        Emoji emoji = Utils.getGuildEmoji(label, poll.guildId);
                        if(emoji == null) {
                            emoji = Emoji.fromFormatted(label);
                        }
                        message.addReaction(emoji).queue();
                    }
                    poll.save();
                }));
    }

    private static void handlePollReactionAdd(MessageReactionAddEvent event) {
        Poll poll = Poll.REGISTRY.get(event.getMessageIdLong());
        if(poll.closed || poll.results.containsKey(event.getUserIdLong())) {
            TextChannel channel = Main.jda.getTextChannelById(poll.channelId);
            if(channel != null) {
                channel.removeReactionById(poll.id, event.getEmoji(), event.getUser()).complete();
            }
            return;
        }
        poll.results.put(event.getUserIdLong(),
                poll.labels.indexOf(event.getEmoji().getFormatted()));
        poll.update();
        poll.save();
    }

    private static void handlePollReactionRemove(MessageReactionRemoveEvent event) {
        Poll poll = Poll.REGISTRY.get(event.getMessageIdLong());
        if(poll.closed) {
            return;
        }
        poll.results.remove(event.getUserIdLong(),
                poll.labels.indexOf(event.getEmoji().getFormatted()));
        poll.update();
        poll.save();
    }

    private static void handleRoleAssignerCommand(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if(guild == null) {
            event.reply("Role assignerse must be created in a server.").setEphemeral(true).queue();
            return;
        }
        Member creator = guild.getMemberById(event.getUser().getIdLong());
        if(creator == null) {
            event.reply("You are not a member of this server.").setEphemeral(true).queue();
            return;
        }
        if(creator.getIdLong() != guild.getOwnerIdLong()) {
            event.reply("You do not have permission to create role assigners.").setEphemeral(true)
                    .queue();
            return;
        }
        List<String> labels = new ArrayList<>();
        List<Long> roles = new ArrayList<>();
        for(int i = 1; i <= 10; i++) {
            OptionMapping role = event.getOption("role" + i);
            if(role == null) {
                break;
            }
            OptionMapping label = event.getOption("label" + i);
            if(label != null) {
                String emoji = label.getAsString();
                Emoji e = Utils.getGuildEmoji(emoji, event.getGuild().getIdLong());
                if(e == null) {
                    if(!Utils.isEmoji(emoji)) {
                        event.reply(
                                        "`label" + i + "` must be an emoji. Invalid value: `" + emoji + "`")
                                .setEphemeral(true).queue();
                        return;
                    }
                }
                labels.add(emoji);
            } else {
                labels.add(Config.getDefaultEmojiLabel(i - 1));
            }
            roles.add(role.getAsRole().getIdLong());
        }
        OptionMapping title = event.getOption("title");
        if(title == null) {
            event.reply("The poll's title must not be empty.").setEphemeral(true).queue();
            return;
        }
        RoleAssigner roleAssigner =
                new RoleAssigner(guild.getIdLong(), event.getGuildChannel().getIdLong(),
                        title.getAsString(), labels, roles);
        OptionMapping image = event.getOption("image");
        if(image != null) {
            roleAssigner.imageURL = image.getAsString();
        }
        OptionMapping color = event.getOption("color");
        if(color != null) {
            String accentColor = color.getAsString();
            if(accentColor.startsWith("#"))
                accentColor = accentColor.substring(1);
            try {
                roleAssigner.accentColor = Integer.parseInt(accentColor, 16);
            } catch(NumberFormatException e) {
                event.reply("Invalid color code: `" + color.getAsString() + "`").setEphemeral(true)
                        .queue();
                return;
            }
        }

        event.replyEmbeds(roleAssigner.build())
                .queue(response -> response.retrieveOriginal().queue(message -> {
                    roleAssigner.id = message.getIdLong();
                    RoleAssigner.REGISTRY.put(roleAssigner.id, roleAssigner);
                    for(final String label : roleAssigner.labels) {
                        Emoji emoji = Utils.getGuildEmoji(label, roleAssigner.guildId);
                        if(emoji == null) {
                            emoji = Emoji.fromFormatted(label);
                        }
                        message.addReaction(emoji).queue();
                    }
                    roleAssigner.save();
                }));
    }

    private static void handleRoleAssignerReactionAdd(MessageReactionAddEvent event) {
        RoleAssigner roleAssigner = RoleAssigner.REGISTRY.get(event.getMessageIdLong());
        long roleId = roleAssigner.roles.get(
                roleAssigner.labels.indexOf(event.getEmoji().getFormatted()));
        Role role = event.getGuild().getRoleById(roleId);
        if(role == null)
            return;
        Member member = event.getMember();
        if(member == null)
            return;
        if(member.getRoles().contains(role))
            return;
        event.getGuild().modifyMemberRoles(member, List.of(role), Collections.emptyList()).queue();
    }

    private static void handleRoleAssignerReactionRemove(MessageReactionRemoveEvent event) {
        RoleAssigner roleAssigner = RoleAssigner.REGISTRY.get(event.getMessageIdLong());
        long roleId = roleAssigner.roles.get(
                roleAssigner.labels.indexOf(event.getEmoji().getFormatted()));
        Role role = event.getGuild().getRoleById(roleId);
        if(role == null)
            return;
        Member member = event.getMember();
        if(member == null)
            return;
        if(!member.getRoles().contains(role))
            return;
        event.getGuild().modifyMemberRoles(member, Collections.emptyList(), List.of(role)).queue();
    }
}
