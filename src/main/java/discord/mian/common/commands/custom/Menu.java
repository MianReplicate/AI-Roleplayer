package discord.mian.common.commands.custom;

import discord.mian.common.AIBot;
import discord.mian.common.Cats;
import discord.mian.common.commands.SlashCommand;
import discord.mian.server.ai.DiscordRoleplay;
import discord.mian.server.ai.prompt.Character;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;

public class Menu extends SlashCommand {


    public Menu(){
        super("menu", "Open the AI menu");
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferReply().queue();

            EmbedBuilder builder = new EmbedBuilder();
            DiscordRoleplay chat = AIBot.bot.getChat(event.getGuild());

            boolean isGif = Cats.isGif();
            byte[] data = Cats.getCat();
            String cat = isGif ? "cat.gif" : "cat.png";

            builder.setTitle("AI Roleplay Menu");
            builder.setAuthor("Created By Your Lovely Girl: @MianReplicate", "https://en.pronouns.page/@MianReplicate");
            builder.setImage("attachment://"+cat);

            builder.setFooter("this is a very queer thing :o"); // random footer msgs
//            builder.setDescription(AIBot.bot.getFunnyMessage());
            builder.setColor(new Color(
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256))
            );
            builder.addField("Character Card(s): ", chat.getCharacter().getType(Character.CharacteristicType.ALIASES).get(0), true);
            builder.addField("Chat History: ",  Math.max(0, chat.getHistory().size() - 2) + " messages", true);
            builder.addField("AI Model: ", chat.getModel(), true);

            WebhookMessageCreateAction<Message> webhookMessageCreateAction = event.getHook().sendMessage(MessageCreateData.fromEmbeds(builder.build()))
                    .addActionRow(
                            Button.primary("restart_history", "Restart History"),
                            Button.link("https://openrouter.ai/models?order=pricing-low-to-high", "Free AI models")
                            );
            if(data != null)
                webhookMessageCreateAction.setFiles(FileUpload.fromData(data, cat));
            webhookMessageCreateAction.queue();
            return true;
        }
        return false;
    }
}
