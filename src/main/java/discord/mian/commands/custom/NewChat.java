package discord.mian.commands.custom;

import discord.mian.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class NewChat extends SlashCommand {

    public NewChat(){
        super("new_chat", "Starts a new chat");
        this.addOptions(
                new OptionData(
                        OptionType.STRING,
                        "system_prompt",
                        "The system prompt you want to use with the AI. Note: AI may not follow the system prompt always",
                        true
                ).addChoices(),
                new OptionData(
                        OptionType.STRING,
                        "character_prompt",
                        "The character you want to role play with",
                        true
                ).addChoices(),
                new OptionData(
                        OptionType.STRING,
                        "world_prompt",
                        "The world lore you want to use for the role play",
                        true
                ).addChoices()
        );
        this.setContexts(InteractionContextType.GUILD);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.reply("Chat is currently unavailable!").queue();
            return true;
        }
        return false;
    }

    @Override
    public void autoComplete(CommandAutoCompleteInteractionEvent event){

    }
}
