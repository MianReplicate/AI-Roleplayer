package discord.mian.loxi.common.commands.custom;

import discord.mian.loxi.common.commands.AbstractCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class RestartHistory extends AbstractCommand {
    public RestartHistory() {
        super("restart_history", "Restarts chat history if there is an ongoing chat");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.reply("Restarting history!").queue();
    }
}
