package discord.mian.commands;

import discord.mian.custom.PermissionHandler;
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PermissionCommand extends ApplicationCommand {
    protected final PermissionHandler<GuildSlashEvent> permissionHandler;

    public PermissionCommand(){
        this.permissionHandler = new PermissionHandler<>();
    }
}
