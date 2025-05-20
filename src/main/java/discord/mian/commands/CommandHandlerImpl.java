package discord.mian.commands;

import discord.mian.api.CommandHandler;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public abstract class CommandHandlerImpl<T extends GenericInteractionCreateEvent & IReplyCallback> extends CommandDataImpl implements CommandHandler<T> {
    protected CommandHandlerImpl(String name, String description) {
        super(name, description);
    }

    protected CommandHandlerImpl(Command.Type type, String name) {
        super(type, name);
    }

    public abstract boolean handle(T event) throws Exception;
}
