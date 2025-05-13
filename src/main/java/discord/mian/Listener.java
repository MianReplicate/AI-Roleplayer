package discord.mian;

import discord.mian.ai.AIBot;
import discord.mian.ai.data.CharacterData;
import discord.mian.components.Component;
import discord.mian.components.Components;
import discord.mian.custom.Constants;
import discord.mian.modals.Modal;
import discord.mian.modals.Modals;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.util.Optional;

public class Listener {
//    @SubscribeEvent
//    public void onModalInteraction(ModalInteractionEvent event) throws Exception{
//        Optional<Modal> optional = Modals.modals.stream().filter(modal -> modal.id.equals(event.getModalId()))
//                .findFirst();
//
//        if(optional.isPresent()){
//            Modal modal = optional.get();
//            try{
//                modal.handle(event);
//            } catch (Exception e) {
//                event.getHook().retrieveOriginal().queue(
//                        message -> message.editMessage("An unexpected error occurred :<").queue(),
//                        failure -> event.reply("An unexpected error occurred :<").setEphemeral(true).queue()
//                );
//                throw(e);
//            };
//        }
//    }

//    @SubscribeEvent
//    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) throws Exception {
////        if(AIBot.bot.getChat(event.getGuild()) == null){
////            event.reply("Bot is not initialized for this guild yet! Please wait a moment..").queue();
////            AIBot.bot.createChat(event.getGuild());
////            return;
////        }
//        BotCommands.handleCommand(event);
//    }
//
//    @SubscribeEvent
//    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
//        if(AIBot.bot.getChat(event.getGuild()) == null){
//            return;
//        }
//        BotCommands.handleAutoComplete(event);
//    }
//
//    @SubscribeEvent
//    public void onButtonInteraction(ButtonInteractionEvent event) throws Exception {
//        Optional<Component> optional = Components.components.stream().filter(component -> component.id.equals(event.getComponentId()))
//                .findFirst();
//
//        if(optional.isPresent()){
//            Component component = optional.get();
//            try{
//                component.handle(event);
//            } catch (Exception e) {
//                event.getHook().retrieveOriginal().queue(
//                        message -> message.editMessage("Failed to activate button :<").queue(),
//                        failure -> event.reply("Failed to activate button :<").setEphemeral(true).queue()
//                );
//                throw(e);
//            };
//        }
//    }
//
//    @SubscribeEvent
//    public void onStringSelectInteraction(StringSelectInteractionEvent event) throws Exception {
//        Optional<Component> optional = Components.components.stream().filter(component -> component.id.equals(event.getComponentId()))
//                .findFirst();
//
//        if(optional.isPresent()){
//            Component component = optional.get();
//            try{
//                component.handle(event);
//            } catch (Exception e) {
//                event.getHook().retrieveOriginal().queue(
//                        message -> message.editMessage("Failed to select :<").queue(),
//                        failure -> event.reply("Failed to select :<").setEphemeral(true).queue()
//                );
//                throw(e);
//            };
//        }
//    }

    @SubscribeEvent
    public void onMessageReceived(MessageReceivedEvent event) {
        if(Constants.ALLOWED_USER_IDS.contains(event.getAuthor().getId()) || Constants.ALLOWED_SERVERS.contains(event.getGuild().getId()) || Constants.PUBLIC){
            Message msg = event.getMessage();
            if(msg.getReferencedMessage() != null && msg.getReferencedMessage().isWebhookMessage()){
                CharacterData character =
                        AIBot.bot.getServerData(event.getGuild()).getCharacterDatas().get(msg.getReferencedMessage().getAuthor().getName());

                if(character == null || !AIBot.bot.getChat(event.getGuild()).getCharacters().containsKey(character.getName())){
                    return;
                }
                AIBot.bot.userChattedTo(character, event.getMessage());
            }
        }
    }
}
