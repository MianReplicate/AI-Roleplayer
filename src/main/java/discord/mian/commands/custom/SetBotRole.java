package discord.mian.commands.custom;

import discord.mian.ai.AIBot;
import discord.mian.commands.SlashCommand;
import discord.mian.custom.ConfigEntry;
import discord.mian.data.Server;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.HashMap;

public class SetBotRole extends SlashCommand {
    public SetBotRole(){
        super("set_bot_role", "Sets the master role for the roleplayer");
        this.addOption(OptionType.ROLE, "role", "The role to set!", false, false);
        this.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR));
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            Role role = event.getOption("role", OptionMapping::getAsRole);

            Server server = AIBot.bot.getServerData(event.getGuild());
            HashMap<String, ConfigEntry> entries = server.getConfig();
            ((ConfigEntry.LongConfig) entries.get("bot_role_id")).value = role != null ? role.getIdLong() : 0L;
            server.saveToConfig(entries);

            event.reply("Set roleplay master role!").setEphemeral(true).queue();
        }
        return false;
    }
}
