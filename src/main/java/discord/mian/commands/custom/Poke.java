package discord.mian.commands.custom;

import discord.mian.ai.AIBot;
import discord.mian.ai.Roleplay;
import discord.mian.data.CharacterData;
import discord.mian.commands.SlashCommand;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.ArrayList;
import java.util.List;

public class Poke extends SlashCommand {
    public Poke() {
        super("poke", "Prompt a character to speak. Can be used to add new characters into the roleplay as well!");
        this.addOption(OptionType.STRING, "character", "The character to be prompted!", true, true);
        this.setContexts(InteractionContextType.GUILD);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferReply().setEphemeral(true).queue();
            Roleplay roleplay = AIBot.bot.getChat(event.getGuild());
            if(event.getChannel().getType() == ChannelType.TEXT){
                if(!roleplay.isRunningRoleplay()){
                    event.getHook().editOriginal("Start a roleplay first!").queue();
                    return true;
                }

                TextChannel channel = roleplay.getChannel();
                if(event.getChannel().getIdLong() == channel.getIdLong()){
                    CharacterData character = AIBot.bot.getServerData(event.getGuild())
                            .getCharacterDatas().get(event.getOption("character", OptionMapping::getAsString));
                    if(character == null){
                        event.getHook().editOriginal("Invalid character!").queue();
                        return true;
                    }

                    roleplay.promptCharacterToRoleplay(character, null, true, false);
                    event.getHook().editOriginal("Prompted character!").queue();
                    return true;
                } else {
                    event.getHook().editOriginal("Can't roleplay here as roleplay was started in another [channel!]("+channel.getJumpUrl()+")").queue();
                    return true;
                }

            }
        }
        return false;
    }

    @Override
    public void autoComplete(CommandAutoCompleteInteractionEvent event){
        List<String> characterNames = AIBot.bot.getServerData(event.getGuild()).getCharacterDatas().keySet().stream()
                .filter(string -> string.toLowerCase().startsWith(event.getFocusedOption().getValue().toLowerCase())).toList();
        if(characterNames.size() >= 25)
            characterNames = characterNames.subList(0, 25);
        List<Command.Choice> choices = new ArrayList<>();
        characterNames.forEach(name -> choices.add(new Command.Choice(name, name)));

        event.replyChoices(choices).queue();
    }
}
