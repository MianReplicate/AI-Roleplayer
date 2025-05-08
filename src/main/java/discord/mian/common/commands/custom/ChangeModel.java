package discord.mian.common.commands.custom;

import discord.mian.common.AIBot;
import discord.mian.common.commands.SlashCommand;
import discord.mian.common.components.Component;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ChangeModel extends SlashCommand {
    private ArrayList<String> recentModels;

    public ChangeModel(){
        super("change_model", "Change the AI model being used");
        this.addOption(OptionType.STRING, "model", "The model link: Find free models on OpenRouter", true, true);

        recentModels = new ArrayList<>();
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            String model = event.getOption("model", OptionMapping::getAsString);
            Consumer<InteractionHook> consumer = (interactionHook) -> AIBot.bot.getChat().setModel(model);
            ReplyCallbackAction reply = event.reply("Changed model!").setEphemeral(true);
            reply.queue(consumer);

            if(!recentModels.contains(model))
                recentModels.add(model);
            if(recentModels.size() > 25)
                recentModels.removeFirst();
            return true;
        }
        return false;
    }

    @Override
    public void autoComplete(CommandAutoCompleteInteractionEvent event){
        event.replyChoices(recentModels.stream().map(word -> new Command.Choice(word, word)).collect(Collectors.toList())).queue();
    }
}
