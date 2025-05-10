package discord.mian.commands.custom;

import discord.mian.ai.AIBot;
import discord.mian.commands.SlashCommand;
import discord.mian.ai.DiscordRoleplay;
import discord.mian.custom.Cats;
import discord.mian.custom.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class Menu extends SlashCommand {


    public Menu(){
        super("menu", "Open the AI menu");
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferReply().queue();
            createMenu(event.getHook().retrieveOriginal().submit().get());
            return true;
        }
        return false;
    }

    public static void createMenu(Message message) throws IOException {
        EmbedBuilder builder = new EmbedBuilder();
        DiscordRoleplay chat = AIBot.bot.getChat(message.getGuild());

        boolean isGif = Cats.isGif();
        Random random = new Random();
        byte[] data = random.nextBoolean() ? Cats.getCat() : Util.getRandomImage();
        if(data == null)
            data = Cats.getCat();
        String fileName = isGif ? "image.gif" : "image.png";

        builder.setTitle("AI Roleplay Dashboard");
        builder.setAuthor("Created By Your Lovely Girl: @MianReplicate", "https://en.pronouns.page/@MianReplicate");
        builder.setImage("attachment://"+fileName);

        builder.setFooter("this is a very queer thing :o"); // random footer msgs
//            builder.setDescription(AIBot.bot.getFunnyMessage());
        builder.setColor(new Color(
                (int) (Math.random() * 256),
                (int) (Math.random() * 256),
                (int) (Math.random() * 256),
                (int) (Math.random() * 256))
        );
        StringBuilder display = new StringBuilder();
        if(chat.getCharacters() != null){
            List<String> keys = chat.getCharacters().keySet().stream().toList();
            for(int i = 0; i < keys.size(); i++){
                String name = keys.get(i);
                display.append(name);
                if(i != keys.size() - 1){
                    display.append(", ");
                }
            }
        } else {
            display = new StringBuilder("No ongoing roleplay!");
        }

        builder.addField("Character(s) Involved: ", display.toString(), true);
        builder.addField("Chat History: ",  chat.getHistory().size() + " messages", true);
        builder.addField("AI Model: ", chat.getModel(), true);

        MessageEditAction editAction = message
                .editMessage(MessageEditData.fromEmbeds(builder.build()))
                .setActionRow(
                        Button.success("new_roleplay", "New Roleplay")
                                .withEmoji(Emoji.fromFormatted("🪄")),
                        Button.primary("menu", "View Dashboard")
                                .withEmoji(Emoji.fromFormatted("🔝")),
                        Button.primary("characters", "View Characters")
                                .withEmoji(Emoji.fromFormatted("🧝")),
                        Button.primary("introductions", "View Introductions")
                                .withEmoji(Emoji.fromFormatted("📋")),
                        Button.link("https://openrouter.ai/models?order=pricing-low-to-high", "Free AI models")
                );
        if(data != null)
            editAction.setFiles(FileUpload.fromData(data, fileName));
        editAction.queue();
    }
}
