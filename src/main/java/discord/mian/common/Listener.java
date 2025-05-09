package discord.mian.common;

import discord.mian.BotRunner;
import discord.mian.common.commands.BotCommands;
import discord.mian.common.components.Components;
import discord.mian.common.util.Constants;
import discord.mian.common.util.Util;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class Listener {
    @SubscribeEvent
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) throws Exception {
        BotCommands.handleCommand(event);
    }

    @SubscribeEvent
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) throws Exception{
        BotCommands.handleAutoComplete(event);
    }

    @SubscribeEvent
    public void onButtonInteraction(ButtonInteractionEvent event){
        Components.components.stream().filter(component -> component.id.equals(event.getComponentId()))
                .findFirst()
                .ifPresent(component -> {
                    try{
                        component.handle(event);
                    } catch (Exception e) {
                        event.reply("Failed to run command :<").setEphemeral(true).queue();
                    }
                });
    }

    @SubscribeEvent
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        Components.components.stream().filter(component -> component.id.equals(event.getComponentId()))
                .findFirst()
                .ifPresent(component -> {
                    try{
                        component.handle(event);
                    } catch (Exception e) {
                        event.reply("Failed to run command :<").setEphemeral(true).queue();
                    }
                });
    }

    @SubscribeEvent
    public void onMessageReceived(MessageReceivedEvent event) {
        if(Constants.ALLOWED_USER_IDS.contains(event.getAuthor().getId()) || Constants.ALLOWED_SERVERS.contains(event.getGuild().getId()) || Constants.PUBLIC){
            Message msg = event.getMessage();
            Mentions mentions = msg.getMentions();
            if(mentions.getUsers().stream().anyMatch(user -> user == AIBot.bot.getJDA().getSelfUser())
                    || (msg.getReferencedMessage() != null && msg.getReferencedMessage().getAuthor() == AIBot.bot.getJDA().getSelfUser())){
                AIBot.bot.userChatted(event.getMessage());
            }
        }
    }
}
