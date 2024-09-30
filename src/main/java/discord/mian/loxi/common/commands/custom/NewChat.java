package discord.mian.loxi.common.commands.custom;

import discord.mian.loxi.common.commands.AbstractCommand;
import discord.mian.loxi.common.util.Constants;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class NewChat extends AbstractCommand {

    public NewChat(){
        this(
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
                ).addChoices());
    }

    private NewChat(OptionData systemPrompt, OptionData characterPrompt, OptionData worldPrompt){
        super("new_chat", "Starts a new chat",
                systemPrompt,
                characterPrompt,
                worldPrompt);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.reply("Chat is currently unavailable!").queue();
    }
}
