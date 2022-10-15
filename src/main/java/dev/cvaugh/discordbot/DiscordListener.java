package dev.cvaugh.discordbot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
            event.getUser().openPrivateChannel().queue(channel -> {
                channel.sendMessage(Main.helpText).queue();
                event.reply("The bot's help information has been sent to you in a DM.")
                        .setEphemeral(true).queue();
            });
        }
        case "flipacoin" -> {
            OptionMapping count = event.getOption("count");
            int flips = count == null ? 1 : count.getAsInt();
            if(flips < 1 || flips > Config.getMaxCoinFlips()) {
                event.reply("`count` must be between 1 and " + Config.getMaxCoinFlips() +
                        ", inclusive.").setEphemeral(true).queue();
            } else if(flips == 1) {
                event.reply(ThreadLocalRandom.current().nextFloat() > 0.5f ? "Heads!" : "Tails!")
                        .queue();
            } else {
                StringBuilder sb = new StringBuilder();
                for(int i = 0; i < flips; i++) {
                    sb.append(ThreadLocalRandom.current().nextFloat() > 0.5f ? "`H` " : "`T` ");
                }
                event.reply(sb.toString().trim()).queue();
            }
        }
        case "ping" -> {
            long time = System.currentTimeMillis();
            event.reply("Waiting for response...").setEphemeral(true)
                    .queue(response -> response.editOriginal(String.format("Response time: %d ms",
                            System.currentTimeMillis() - time)).queue());
        }
        case "poll" -> {
            if(event.getGuild() == null) {
                event.reply("Polls must be created in a server.").setEphemeral(true).queue();
                return;
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
                                    "`label" + i + "` must be an emoji. Invalid value: `" + emoji +
                                            "`").setEphemeral(true).queue();
                            return;
                        }
                    }
                    labels.add(emoji);
                } else {
                    labels.add(Config.getDefaultPollLabel(i - 1));
                }
                options.add(option.getAsString());
            }
            OptionMapping title = event.getOption("title");
            if(title == null) {
                event.reply("The poll's title must not be empty.").setEphemeral(true).queue();
                return;
            }
            Poll poll = new Poll(event.getGuild().getIdLong(), event.getGuildChannel().getIdLong(),
                    event.getUser().getIdLong(), title.getAsString(), labels, options);
            OptionMapping duration = event.getOption("duration");
            if(duration != null) {
                long end = Utils.durationToTimestamp(duration.getAsString());
                if(end == 0L) {
                    event.reply("Invalid duration: `" + duration.getAsString() + "`")
                            .setEphemeral(true).queue();
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
                    event.reply("Invalid color code: `" + color.getAsString() + "`")
                            .setEphemeral(true).queue();
                    return;
                }
            }

            event.replyEmbeds(poll.build())
                    .queue(response -> response.retrieveOriginal().queue(message -> {
                        poll.id = message.getIdLong();
                        Poll.POLLS.put(poll.id, poll);
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
        case "settings" -> {
            // TODO settings command
        }
        default -> {}
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if(event.getUser() == null || event.getUser().isBot())
            return;
        if(Poll.POLLS.containsKey(event.getMessageIdLong())) {
            Poll poll = Poll.POLLS.get(event.getMessageIdLong());
            if(poll.closed || poll.results.containsKey(event.getUserIdLong())) {
                TextChannel channel = Main.jda.getTextChannelById(poll.channelId);
                if(channel != null) {
                    channel.removeReactionById(poll.id, event.getEmoji(), event.getUser())
                            .complete();
                }
                return;
            }
            poll.results.put(event.getUserIdLong(),
                    poll.labels.indexOf(event.getEmoji().getFormatted()));
            poll.update();
            poll.save();
        }
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        if(event.getUser() == null || event.getUser().isBot()) {return;}
        if(Poll.POLLS.containsKey(event.getMessageIdLong())) {
            Poll poll = Poll.POLLS.get(event.getMessageIdLong());
            if(poll.closed) {
                return;
            }
            poll.results.remove(event.getUserIdLong(),
                    poll.labels.indexOf(event.getEmoji().getFormatted()));
            poll.update();
            poll.save();
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if(Poll.POLLS.containsKey(event.getMessageIdLong())) {
            Poll.deletePoll(event.getMessageIdLong());
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Logger.info("Joined guild: %s (ID %d)", event.getGuild().getName(),
                event.getGuild().getIdLong());
        Guilds.put(event.getGuild().getIdLong());
    }
}
