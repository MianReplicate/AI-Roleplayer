package discord.mian.loxi.common;

import discord.mian.loxi.BotRunner;
import discord.mian.loxi.common.util.Constants;
import discord.mian.loxi.common.util.Util;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
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
        BotRunner.bot.getBotCommands().getCommands().stream().filter(command -> event.getName().equals(command.getName())).findAny().ifPresentOrElse(
                abstractCommand -> abstractCommand.execute(event), () -> {
                    throw new RuntimeException("Unknown command ran!");
                }
        );
    }

    @SubscribeEvent
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event){
        BotRunner.bot.getBotCommands().getCommands().stream().filter(command -> event.getName().equals(command.getName())).findAny().ifPresentOrElse(
                abstractCommand -> abstractCommand.onAutoComplete(event), () -> {
                    throw new RuntimeException("Unknown command ran!");
                }
        );
    }

    @SubscribeEvent
    public void onMessageReceived(MessageReceivedEvent event) {
        if(Constants.ALLOWED_USER_IDS.contains(event.getAuthor().getId()) || Constants.ALLOWED_SERVERS.contains(event.getGuild().getId()) || Constants.PUBLIC){
            Message msg = event.getMessage();
            Mentions mentions = msg.getMentions();
            if(mentions.getUsers().stream().anyMatch(user -> user == BotRunner.bot.getUser())
                    || (msg.getReferencedMessage() != null && msg.getReferencedMessage().getAuthor() == BotRunner.bot.getUser())){
                if(BotRunner.bot.getChat().isMakingResponse()) {
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
//                        String response = BotRunner.bot.getChat().sendAndGetResponse(msg.getAuthor().getEffectiveName(), noMentionsContent);
//                        message.editMessage(MessageEditData.fromContent(response)).queue();

                        // Bottom is streaming code. Add an option between streaming & non streaming later on.
                        AtomicBoolean queued = new AtomicBoolean(false);
                        AtomicLong timeResponseMade = new AtomicLong(System.currentTimeMillis());
                        double timeBetween = 1;

                        String fullResponse = BotRunner.bot.getChat().sendAndStream(msg.getAuthor().getEffectiveName(), noMentionsContent,
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

                        message.editMessage(MessageEditData.fromContent(fullResponse)).queue();
                    } catch (Exception e) {
                        message.editMessage(MessageEditData.fromContent(Util.botifyMessage("Failed to send a response due to an exception :< sowwy.\nError: "+e)))
                                .queue();
                        BotRunner.bot.getChat().responseFailed();
                    }
                };
                messageCreateData.queue(messageConsumer);
            }
        }
    }
}
