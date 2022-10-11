package dev.cvaugh.discordbot;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
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
            if(flips < 1 || flips > 100) {
                event.reply("`count` must be between 1 and 50, inclusive.").setEphemeral(true)
                        .queue();
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
                    // TODO validate emoji
                    labels.add(emoji);
                } else {
                    labels.add(Poll.DEFAULT_LABELS[i - 1]);
                }
                options.add(option.getAsString());
            }
            if(event.getGuild() == null) {
                event.reply("Polls must be created in a server.").setEphemeral(true).queue();
                return;
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
                    for(String label : poll.labels) {
                        message.addReaction(Emoji.fromUnicode(label)).queue();
                    }
                });
            });
        }
        default -> {}
        }
    }
}
