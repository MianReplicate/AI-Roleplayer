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
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event){
        BotCommands.handleCommand(event);
    }

    @SubscribeEvent
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event){
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
                if(AIBot.bot.getChat().isMakingResponse()) {
                    msg.getChannel().sendMessage(MessageCreateData.fromContent(
                            Util.botifyMessage("Cannot make a response since I am already generating one!")
                    )).queue();
                    return;
                }
                MessageCreateAction messageCreateData = msg.getChannel().sendMessage(
                        MessageCreateData.fromContent(
                                Util.botifyMessage("Currently creating a response! Check back in a second..")
                        )
                );
                Consumer<Message> messageConsumer = message -> {
                    try{
                        String noMentionsContent = msg.getContentRaw().replaceAll("<@"+msg.getAuthor().getId()+">", "");
//                        String response = AIBot.bot.getChat().sendAndGetResponse(msg.getAuthor().getEffectiveName(), noMentionsContent);
//                        message.editMessage(MessageEditData.fromContent(response)).queue();

                        // Bottom is streaming code. Add an option between streaming & non streaming later on.
                        AtomicBoolean queued = new AtomicBoolean(false);
                        AtomicLong timeResponseMade = new AtomicLong(System.currentTimeMillis());
                        double timeBetween = 1;

                        String fullResponse = AIBot.bot.getChat().sendAndStream(msg.getAuthor().getEffectiveName(), noMentionsContent,
                                currentResponse -> {
                                    if(!queued.get() && System.currentTimeMillis() - timeResponseMade.get() >= timeBetween && !currentResponse.isBlank()){
                                        queued.set(true);
                                        Consumer<Message> onComplete = newMsg -> {
                                            queued.set(false);
                                            timeResponseMade.set(System.currentTimeMillis());
                                        };
                                        message.editMessage(MessageEditData.fromContent(Util.botifyMessage("Message is being streamed: Once the response is complete, this will be gone to let you know the message is done streaming")+"\n"+ currentResponse)).queue(onComplete);
                                    }
                                });
                        //

//                        String fullResponse = "gay";
                        message.editMessage(MessageEditData.fromContent(fullResponse)).queue();
                    } catch (Exception e) {
                        message.editMessage(MessageEditData.fromContent(Util.botifyMessage("Failed to send a response due to an exception :< sowwy.\nError: "+e)))
                                .queue();
                        AIBot.bot.getChat().responseFailed();
                    }
                };
                messageCreateData.queue(messageConsumer);
            }
        }
    }
}
