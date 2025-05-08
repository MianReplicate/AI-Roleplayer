package discord.mian.common.commands.api;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface CommandHandler<T extends GenericInteractionCreateEvent & IReplyCallback> extends CommandData {
    boolean handle(T event) throws Exception;
}
