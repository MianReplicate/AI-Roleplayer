package discord.mian.commands;

import discord.mian.commands.api.CommandHandler;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;

public abstract class MessageCommand
        extends CommandHandlerImpl<MessageContextInteractionEvent>
        implements CommandHandler<MessageContextInteractionEvent> {
    public MessageCommand(String name) {
        super(Command.Type.MESSAGE, name);
    }
}
