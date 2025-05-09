package discord.mian.commands.custom;

import discord.mian.AIBot;
import discord.mian.custom.Cats;
import discord.mian.commands.SlashCommand;
import discord.mian.ai.DiscordRoleplay;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.awt.*;
import java.util.List;

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

    public static void createMenu(Message message){
        EmbedBuilder builder = new EmbedBuilder();
        DiscordRoleplay chat = AIBot.bot.getChat(message.getGuild());

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
                        Button.link("https://openrouter.ai/models?order=pricing-low-to-high", "Free AI models")
                );
        if(data != null)
            editAction.setFiles(FileUpload.fromData(data, cat));
        editAction.queue();
    }
}
