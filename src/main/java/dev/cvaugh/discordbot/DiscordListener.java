package dev.cvaugh.discordbot;

import com.vdurmont.emoji.EmojiManager;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
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
        Main.schedulePollUpdates();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch(event.getName()) {
        case "help" -> {
            // TODO
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
                    // TODO fix emoji validation
                    if(!EmojiManager.isEmoji(emoji) &&
                            Utils.getGuildEmoji(emoji, event.getGuild().getIdLong()) == null) {
                        event.reply("Label `" + emoji + "` must be an emoji.").setEphemeral(true)
                                .queue();
                        return;
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

            event.replyEmbeds(poll.build()).queue(response -> {
                response.retrieveOriginal().queue(message -> {
                    poll.id = message.getIdLong();
                    Poll.POLLS.put(poll.id, poll);
                    poll.save();
                    for(String label : poll.labels) {
                        if(EmojiManager.isEmoji(label)) {
                            message.addReaction(Emoji.fromUnicode(label)).queue();
                        } else {
                            RichCustomEmoji emoji = Utils.getGuildEmoji(label, poll.guildId);
                            if(emoji == null) {
                                Logger.error("Emoji not found: %s", label);
                                return;
                            }
                            message.addReaction(emoji).queue();
                        }
                    }
                });
            });
        }
        default -> {}
        }
    }

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
}
