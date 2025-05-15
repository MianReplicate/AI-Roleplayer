package discord.mian;

import discord.mian.ai.AIBot;
import discord.mian.ai.DiscordRoleplay;
import discord.mian.custom.ConfigEntry;
import discord.mian.data.CharacterData;
import discord.mian.commands.BotCommands;
import discord.mian.custom.Constants;
import discord.mian.interactions.InteractionCreator;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class Listener {
    @SubscribeEvent
    public void onModalInteraction(ModalInteractionEvent event) {
        Consumer<? super GenericInteractionCreateEvent> component =
                InteractionCreator.getComponentConsumer(event.getModalId());
        if(component == null){
            event.reply("This interaction has expired! Please redo the same steps you used to get here").setEphemeral(true).queue();
            return;
        }

        try{
            component.accept(event);
        } catch (Exception e) {
            event.getHook().retrieveOriginal().queue(
                    message -> message.editMessage("An unexpected error occurred :<").queue(),
                    failure -> event.reply("An unexpected error occurred :<").setEphemeral(true).queue()
            );
            throw(e);
        }
    }

    @SubscribeEvent
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) throws Exception {
//        if(AIBot.bot.getChat(event.getGuild()) == null){
//            event.reply("Bot is not initialized for this guild yet! Please wait a moment..").queue();
//            AIBot.bot.createChat(event.getGuild());
//            return;
//        }
        BotCommands.handleCommand(event);
    }

    @SubscribeEvent
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if(AIBot.bot.getChat(event.getGuild()) == null){
            return;
        }
        BotCommands.handleAutoComplete(event);
    }

    @SubscribeEvent
    public void onButtonInteraction(ButtonInteractionEvent event) {
        Consumer<? super GenericInteractionCreateEvent> component =
                InteractionCreator.getComponentConsumer(event.getComponentId());
        if(component == null){
            event.reply("This interaction has expired! Please redo the same steps you used to get here").setEphemeral(true).queue();
            return;
        }

        try{
            component.accept(event);
        } catch (Exception e) {
            event.getHook().retrieveOriginal().queue(
                    message -> message.editMessage("Failed to activate button :<").queue(),
                    failure -> event.reply("Failed to activate button :<").setEphemeral(true).queue()
            );
            throw(e);
        }
    }

    @SubscribeEvent
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        Consumer<? super GenericInteractionCreateEvent> component =
                InteractionCreator.getComponentConsumer(event.getComponentId());
        if(component == null){
            event.reply("This interaction has expired! Please redo the same steps you used to get here").setEphemeral(true).queue();
            return;
        }

        try{
            component.accept(event);
        } catch (Exception e) {
            event.getHook().retrieveOriginal().queue(
                    message -> message.editMessage("Failed to select options :<").queue(),
                    failure -> event.reply("Failed to select options :<").setEphemeral(true).queue()
            );
            throw(e);
        }
    }

    @SubscribeEvent
    public void onMessageReceived(MessageReceivedEvent event) throws ExecutionException, InterruptedException {
        if(Constants.ALLOWED_USER_IDS.contains(event.getAuthor().getId()) || Constants.ALLOWED_SERVERS.contains(event.getGuild().getId()) || Constants.PUBLIC){
            Message msg = event.getMessage();
            DiscordRoleplay roleplay = AIBot.bot.getChat(event.getGuild());

            if(msg.getAuthor() == AIBot.bot.getJDA().getSelfUser())
                return;
            if(msg.isWebhookMessage())
                return;
            if(roleplay.isMakingResponse())
                return;
            if(!roleplay.isRunningRoleplay())
                return;

            if(event.getChannel().getIdLong() == roleplay.getChannel().getIdLong() && roleplay.isRunningRoleplay()){
                Random random = new Random();

                CharacterData fromContent = roleplay.findRespondingCharacterFromContent(msg.getContentRaw());
                if(fromContent != null && random.nextBoolean() && !fromContent.getName().equals(event.getAuthor().getName()))
                    roleplay.promptCharacterToRoleplay(fromContent, msg, true, false);
                else{
                    CharacterData data = roleplay.findRespondingCharacterFromMessage(msg);
                    if(data != null && !data.getName().equals(event.getAuthor().getName())){
                        roleplay.promptCharacterToRoleplay(data, msg, true, false);
                    } else if(!((ConfigEntry.BoolConfig) AIBot.bot.getServerData(event.getGuild()).getConfig()
                            .get("only_chat_on_mention")).value){

                        if(random.nextBoolean()){
                            final double total = roleplay.getCharacters().values().stream()
                                    .filter(data1 -> !data1.getName().equals(event.getAuthor().getName()))
                                    .mapToDouble(CharacterData::getTalkability).sum();

                            double percentage = Math.random();
                            List<CharacterData> meetsCriteria = roleplay.getCharacters().values().stream().filter(
                                    characterData -> (characterData.getTalkability() / total) >= percentage &&
                                            !characterData.getName().equals(event.getAuthor().getName())
                            ).toList();

                            if(!meetsCriteria.isEmpty())
                                roleplay.promptCharacterToRoleplay(meetsCriteria.get((int)(Math.random() * meetsCriteria.size())),
                                        msg, true, false);
                        }
                    }
                }
            }
        }
    }
}
