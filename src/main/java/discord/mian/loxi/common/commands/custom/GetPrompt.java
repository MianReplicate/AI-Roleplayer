package discord.mian.loxi.common.commands.custom;

import discord.mian.loxi.common.ai.prompt.PromptType;
import discord.mian.loxi.common.commands.AbstractCommand;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class GetPrompt extends AbstractCommand {
    public GetPrompt(){
        this(
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

    public GetPrompt(OptionData typeOfPrompt){
        super("get_prompt", "Retrieve a prompt based on type and name", typeOfPrompt);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {

    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event){

    }
}
