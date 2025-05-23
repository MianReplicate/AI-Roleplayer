package discord.mian.commands.custom;

import discord.mian.commands.SlashCommand;
import discord.mian.custom.Constants;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.requests.restaction.MessageCreateActionImpl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Talk extends SlashCommand {
    public Talk() {
        super("talk", "You want me to talk?");
        this.permissionHandler.addUsers(Constants.ALLOWED_USER_IDS);
        this.addOptions(
                new OptionData(OptionType.STRING, "text", "Put any text here!", false),
                new OptionData(OptionType.ATTACHMENT, "attachment", "Put any attachments here!", false),
                new OptionData(OptionType.CHANNEL, "channel", "Want a specific channel? Here it is :O", false),
                new OptionData(OptionType.STRING, "replyto", "Put message id here to reply to!", false),
                new OptionData(OptionType.INTEGER, "delay", "Wanna delay me?! Fuck yeahhh", false)
        );
        this.setContexts(InteractionContextType.GUILD);
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if (super.handle(event)) {
            String text = event.getOption("text", OptionMapping::getAsString);
            String msgId = event.getOption("replyto", OptionMapping::getAsString);
            Message.Attachment attachment = event.getOption("attachment", OptionMapping::getAsAttachment);
            Channel channel = event.getOption("channel", OptionMapping::getAsChannel);
            Integer delay = event.getOption("delay", OptionMapping::getAsInt);

            if (channel == null) {
                channel = event.getChannel();
            } else {
                channel = ((GuildChannelUnion) channel).asTextChannel();
            }

            MessageChannel msgChannel = (MessageChannel) channel;
            MessageCreateBuilder builder = new MessageCreateBuilder();

            if (text != null) {
                builder.setContent(text);
            }

            if (attachment != null) {
                builder.addFiles(FileUpload.fromStreamSupplier(attachment.getFileName(), () -> {
                    try {
                        return attachment.getProxy().download().get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }

            MessageCreateData msgData = builder.build();

            Consumer<Message> onMessage = (message) -> {
                MessageCreateAction messageCreateAction = message != null ? message.reply(msgData)
                        : new MessageCreateActionImpl(msgChannel).applyData(msgData);

                ReplyCallbackAction replyAction = event.reply("The message is on its way!").setEphemeral(true);

                if (delay != null) {
                    msgChannel.sendTyping().and(replyAction).queue(success ->
                            messageCreateAction.queueAfter(delay, TimeUnit.SECONDS));
                } else {
                    messageCreateAction.and(replyAction).queue();
                }
            };

            if (msgId != null) {
                msgChannel.retrieveMessageById(msgId).queue(onMessage);
            } else {
                onMessage.accept(null);
            }
            return true;
        }
        return false;
    }
}