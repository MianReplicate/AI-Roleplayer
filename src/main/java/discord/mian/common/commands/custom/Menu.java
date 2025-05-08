package discord.mian.common.commands.custom;

import discord.mian.common.AIBot;
import discord.mian.common.Cat;
import discord.mian.common.commands.SlashCommand;
import discord.mian.server.ai.RoleplayChat;
import discord.mian.server.ai.prompt.Character;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

public class Menu extends SlashCommand {


    public Menu(){
        super("menu", "Open the AI menu");
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferReply().queue();

            EmbedBuilder builder = new EmbedBuilder();

            boolean isGif = Cat.isGif();
            byte[] data = Cat.getCat();
            String cat = isGif ? "cat.gif" : "cat.png";

            builder.setTitle("AI Roleplay Menu");
            builder.setAuthor("Created By Your Lovely Girl: @MianReplicate", "https://en.pronouns.page/@MianReplicate");
            builder.setImage("attachment://"+cat);

            builder.setFooter("this is a very queer thing :o"); // random footer msgs

            String responseMsg = AIBot.bot.getChat().createResponse(
                            "[System Command: Respond to the following message in 10 or less words]: \"Fuck you\""
            );
            builder.setDescription("\""+responseMsg+"\""); // put random AI msg in here
            builder.setColor(new Color(
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256))
            );
            RoleplayChat chat = AIBot.bot.getChat();
            builder.addField("Character Card(s): ", chat.getCharacter().getType(Character.CharacteristicType.ALIASES).get(0), true);
            builder.addField("Chat History: ",  Math.max(0, chat.getHistory().size() - 2) + " messages", true);
            builder.addField("AI Model: ", chat.getModel(), true);

           event.getHook().sendMessage(MessageCreateData.fromEmbeds(builder.build()))
                    .setFiles(FileUpload.fromData(data, cat))
                    .addActionRow(
                            Button.primary("restart_history", "Restart History"),
                            Button.link("https://openrouter.ai/models?order=pricing-low-to-high", "Free AI models")
                            ) // Restart history
//                            Button.success("emoji", Emoji.fromFormatted("<:minn:245267426227388416>"))) // Button with only an emoji
                    .queue();
            return true;
        }
        return false;
    }
}
