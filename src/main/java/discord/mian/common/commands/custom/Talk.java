package discord.mian.common.commands.custom;

import discord.mian.common.AIBot;
import discord.mian.common.commands.SlashCommand;
import discord.mian.common.util.Constants;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.TimeUtil;

import java.util.Date;
import java.util.Optional;

public class Talk extends SlashCommand {
    // later when we add group chats, add option to reply to someone else?
    public Talk(){
        super("talk", "Engage within the ongoing roleplay. Your latest message (within a day) goes in as a reply.");
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            if(Constants.ALLOWED_USER_IDS.contains(event.getUser().getId()) || Constants.ALLOWED_SERVERS.contains(event.getGuild().getId()) || Constants.PUBLIC){
                event.deferReply().queue();
                if(event.getChannel().getType() == ChannelType.TEXT){
                    TextChannel channel = event.getChannel().asTextChannel();

                    Date date = new Date();
                    long dayAgo = date.getTime() - 86400000L;
                    long discordDayAgo = TimeUtil.getDiscordTimestamp(dayAgo);

                    Optional<Message> message = channel.getIterableHistory().deadline(discordDayAgo).stream().filter(msg -> msg.getAuthor() == event.getUser())
                            .findFirst();
                    if(message.isPresent()){
                        AIBot.bot.userChatted(message.get());
                        String link = message.get().getJumpUrl();
                        event.getHook().editOriginal("Got your most recent [message]("+link+") and sent it!").queue();
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
