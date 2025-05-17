package discord.mian.commands.custom;

import discord.mian.ai.AIBot;
import discord.mian.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class ChangeMaxTokens extends SlashCommand {
    public ChangeMaxTokens() {
        super("tokens", "Change the max tokens");
        this.addOption(OptionType.INTEGER, "number", "The number to use, must be greater than 0", true);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            int tokens = event.getOption("number", OptionMapping::getAsInt);
            if(tokens <= 0){
                event.reply("The tokens have to be greater than 0").setEphemeral(true).queue();
                return true;
            }

            AIBot.bot.getChat(event.getGuild()).setMaxTokens(tokens);
            event.reply("Set max tokens!").setEphemeral(true).queue();
            return true;
        }
        return false;
    }
}
