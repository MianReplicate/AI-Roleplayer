package discord.mian.common.commands.custom;

import discord.mian.common.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Menu extends SlashCommand {
    public Menu(){
        super("menu", "Open the AI menu");
    }

    @Override
    public boolean handle(SlashCommandInteractionEvent event) throws Exception {
        if(super.handle(event)){
            EmbedBuilder builder = new EmbedBuilder();

            builder.setTitle("AI Roleplay Menu");
            builder.setAuthor("Mian", "https://en.pronouns.page/@MianReplicate");
            builder.setImage("attachment://cat.png");

            InputStream file;

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest
                    .newBuilder(URI.create("https://cataas.com/cat/says/Meow%20"+event.getUser().getGlobalName()))
                    .GET()
                    .build();

            try {
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                file = response.body();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            builder.setFooter("this is very gay :o");
            builder.setDescription("The panel to fuck around on :)");
            builder.setColor(new Color(
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256))
            );

//            event.getChannel().sendFiles()
            event.reply(MessageCreateData.fromEmbeds(builder.build()))
                    .setFiles(FileUpload.fromData(file, "cat.png"))
//                    .setEphemeral(true)
                    .addActionRow(
                            Button.primary("restart_history", "Restart History")) // Restart history
//                            Button.success("emoji", Emoji.fromFormatted("<:minn:245267426227388416>"))) // Button with only an emoji
                    .queue();
            return true;
        }
        return false;
    }
}
