package discord.mian.commands.custom;

import discord.mian.ai.AIBot;
import discord.mian.commands.SlashCommand;
import discord.mian.custom.Util;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
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
        this.setContexts(InteractionContextType.GUILD);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            if(!Util.hasMasterPermission(event.getMember())){
                event.reply("nuh uh little bro bro, you dont got permission").setEphemeral(true).queue();
                return true;
            }

            String model = event.getOption("model", OptionMapping::getAsString);
            Consumer<InteractionHook> consumer = (interactionHook) -> AIBot.bot.getChat(event.getGuild()).setModel(model);
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
