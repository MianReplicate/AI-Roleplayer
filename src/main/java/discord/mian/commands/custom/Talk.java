package discord.mian.commands.custom;

import discord.mian.AIBot;
import discord.mian.ai.DiscordRoleplay;
import discord.mian.commands.SlashCommand;
import discord.mian.custom.Constants;
import discord.mian.ai.prompt.Character;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.TimeUtil;

import java.util.Date;
import java.util.Optional;

public class Talk extends SlashCommand {
    // later when we add group chats, add option to reply to someone else?
    public Talk(){
        super("talk", "Engage within the ongoing roleplay. Your latest message (within a day) goes in as a reply.");
        this.addOption(OptionType.STRING, "character", "The character you want to talk to", true);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            if(Constants.ALLOWED_USER_IDS.contains(event.getUser().getId()) || Constants.ALLOWED_SERVERS.contains(event.getGuild().getId()) || Constants.PUBLIC){
                event.deferReply().setEphemeral(true).queue();
                DiscordRoleplay roleplay = AIBot.bot.getChat(event.getGuild());
                if(event.getChannel().getType() == ChannelType.TEXT){
                    TextChannel channel = roleplay.getChannel();
                    if(event.getChannel().getIdLong() == channel.getIdLong()){
                        Date date = new Date();
                        long dayAgo = date.getTime() - 86400000L;
                        long discordDayAgo = TimeUtil.getDiscordTimestamp(dayAgo);

                        Optional<Message> message = channel.getIterableHistory().deadline(discordDayAgo).stream().filter(msg -> msg.getAuthor() == event.getUser())
                                .findFirst();
                        if(message.isPresent()){
                            Character character = (Character)
                                    AIBot.bot.getPrompts().getPromptData(event.getOption("character", OptionMapping::getAsString));
                            if(character == null){
                                event.getHook().editOriginal("Invalid character!").queue();
                                return true;
                            }

                            boolean success = AIBot.bot.userChattedTo(character, message.get());
                            if(success){
                                String link = message.get().getJumpUrl();
                                event.getHook().editOriginal("Got your most recent [message]("+link+") and sent it!").queue();
                            } else {
                                event.getHook().editOriginal("Start a roleplay first!").queue();
                            }
                            return true;
                        }
                    } else {
                        event.getHook().editOriginal("Can't roleplay here as roleplay was started in another [channel!]("+channel.getJumpUrl()+")").queue();
                        return true;
                    }

                }
            }
        }
        return false;
    }
}
