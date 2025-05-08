package discord.mian.common.commands;

import discord.mian.common.PermissionHandler;
import discord.mian.common.commands.api.CommandHandler;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public abstract class SlashCommand
        extends CommandHandlerImpl<SlashCommandInteractionEvent>
        implements CommandHandler<SlashCommandInteractionEvent> {
    protected final PermissionHandler<SlashCommandInteractionEvent> permissionHandler;

    public SlashCommand(String name, String description){
        super(name.toLowerCase(), description);
        this.permissionHandler = new PermissionHandler<>();
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        return permissionHandler.handle(event);
    }

    public void autoComplete(CommandAutoCompleteInteractionEvent event) throws Exception {};
}
