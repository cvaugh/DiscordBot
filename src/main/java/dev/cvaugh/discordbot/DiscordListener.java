package dev.cvaugh.discordbot;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DiscordListener extends ListenerAdapter {
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        Logger.info("Ready event received");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot())
            return;
        if(!event.isFromType(ChannelType.PRIVATE)) {
            if(event.getMessage().getContentRaw().startsWith(Main.commandPrefix())) {
                parseCommand(event);
            }
        }
    }

    private static void parseCommand(MessageReceivedEvent event) {
        String[] splitCommand =
                event.getMessage().getContentRaw().substring(Main.commandPrefix().length())
                        .split(" ", 2);
        String command = splitCommand[0].toLowerCase();
        String args = splitCommand.length > 1 ? splitCommand[1] : "";

        switch(command) {
        case "ping": {
            long time = System.currentTimeMillis();
            event.getChannel().sendMessage("Waiting for response...").queue(response -> {
                response.editMessageFormat("Response time: %d ms",
                        System.currentTimeMillis() - time).queue();
            });
            break;
        }
        case "poll": {

            break;
        }
        default: {}
        }
    }
}
