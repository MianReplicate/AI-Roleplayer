package discord.mian.loxi.common.commands.custom;

import discord.mian.loxi.BotRunner;
import discord.mian.loxi.common.commands.AbstractCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.function.Consumer;

public class RestartHistory extends AbstractCommand {
    public RestartHistory() {
        super("restart_history", "Restarts chat history if there is an ongoing chat");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        ReplyCallbackAction reply = event.reply("Restarting history!");
        Consumer<InteractionHook> consumer = (interactionHook) -> BotRunner.bot.getChat().restartHistory();
        reply.queue(consumer);
    }
}
