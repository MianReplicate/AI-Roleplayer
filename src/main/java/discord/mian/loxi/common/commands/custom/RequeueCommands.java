package discord.mian.loxi.common.commands.custom;

import discord.mian.loxi.BotRunner;
import discord.mian.loxi.common.commands.AbstractCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class RequeueCommands extends AbstractCommand {

    // we should porbably remove this idt it is a good idea lol, we'll figure some way out to reregister autocompletion thingies
    public RequeueCommands(){
        super("requeue_commands", "Reregisters commands to Discord!");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).setContent("Restarting commands!").and(
                BotRunner.bot.getBotCommands().addCommands()
        ).queue();
    }
}
