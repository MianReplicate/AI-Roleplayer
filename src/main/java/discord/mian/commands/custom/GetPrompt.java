package discord.mian.commands.custom;

import discord.mian.commands.SlashCommand;
import discord.mian.ai.prompt.PromptType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class GetPrompt extends SlashCommand {
    public GetPrompt(){
        super("get_prompt", "Retrieve a prompt based on type and name");
        this.addOptions(
                new OptionData(
                        OptionType.STRING,
                        "type_of_prompt",
                        "The type of prompt you want",
                        true
                ).addChoices(List.of(
                        new Command.Choice(PromptType.INSTRUCTION.getName(), PromptType.INSTRUCTION.getName()),
                        new Command.Choice(PromptType.CHARACTER.getName(), PromptType.CHARACTER.getName()),
                        new Command.Choice(PromptType.WORLD.getName(), PromptType.WORLD.getName())
                )));
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            return true;
        }
        return false;
    }
}
