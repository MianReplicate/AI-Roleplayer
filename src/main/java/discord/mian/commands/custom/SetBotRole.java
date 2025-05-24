package discord.mian.commands.custom;

import com.mongodb.MongoException;
import discord.mian.ai.AIBot;
import discord.mian.commands.SlashCommand;
import discord.mian.custom.ConfigEntry;
import discord.mian.custom.Util;
import discord.mian.data.Server;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.HashMap;

public class SetBotRole extends SlashCommand {
    public SetBotRole() {
        super("set_bot_role", "Sets the master role for the roleplayer");
        this.addOption(OptionType.ROLE, "role", "The role to set!", false, false);
        this.setContexts(InteractionContextType.GUILD);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if (super.handle(event)) {
            if (!Util.hasMasterPermission(event.getMember())) {
                event.reply("nuh uh little bro bro, you dont got permission").setEphemeral(true).queue();
                return true;
            }

            Role role = event.getOption("role", OptionMapping::getAsRole);

            Server server = AIBot.bot.getServerData(event.getGuild());
            try{
                server.updateConfig(entries -> {
                    ((ConfigEntry.LongConfig) entries.get("bot_role_id")).value = role != null ? role.getIdLong() : 0L;
                });
                event.reply("Set roleplay master role!").setEphemeral(true).queue();
            }catch(MongoException ignored){
                event.reply("Failed to set roleplay master role!").setEphemeral(true).queue();
            }
        }
        return false;
    }
}
