package discord.mian.commands.custom;

import discord.mian.ai.AIBot;
import discord.mian.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class ChangeTemp extends SlashCommand {
    public ChangeTemp() {
        super("temperature", "Change the temperature");
        this.addOption(OptionType.INTEGER, "number", "The number to use. Range: 0 to 200", true);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if (super.handle(event)) {
            int temperature = event.getOption("number", OptionMapping::getAsInt);
            if (temperature <= 0 || temperature > 200) {
                event.reply("The temperature has to be within 0-200!").setEphemeral(true).queue();
                return true;
            }

            double toSet = (double) temperature / 100;
            AIBot.bot.getChat(event.getGuild()).setTemperature(toSet);
            event.reply("Set new temperature!").setEphemeral(true).queue();
            return true;
        }
        return false;
    }
}
