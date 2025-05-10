package discord.mian.components.custom;

import discord.mian.ai.AIBot;
import discord.mian.ai.DiscordRoleplay;
import discord.mian.ai.data.Server;
import discord.mian.components.Component;
import discord.mian.custom.Cats;
import discord.mian.custom.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class ViewInstalledDatas extends Component<ButtonInteractionEvent> {
    public ViewInstalledDatas(String id) {
        super(id);
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferEdit().queue();
            Server server = AIBot.bot.getServerData(event.getGuild());
            if(event.getComponentId().equals("characters")){
                createInstalledDatasMenu(event.getMessage(), server.getCharacterDatas().keySet().stream().toList());
            } else {
                createInstalledDatasMenu(event.getMessage(), server.getInstructionDatas().keySet().stream().toList());
            }

            return true;
        }

        return false;
    }

    public static void createInstalledDatasMenu(Message message, List<String> display) throws IOException {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle("Installed Prompts");
        builder.setAuthor("Created By Your Lovely Girl: @MianReplicate", "https://en.pronouns.page/@MianReplicate");

        builder.setColor(new Color(
                (int) (Math.random() * 256),
                (int) (Math.random() * 256),
                (int) (Math.random() * 256),
                (int) (Math.random() * 256))
        );
        StringBuilder description = new StringBuilder("```");
        for(int i = 0; i < display.size(); i++){
            String string = display.get(i);
            description.append(string);

            if(i + 1 != display.size())
                description.append("\n");
        }
        description.append("```");
        builder.setDescription(description.toString());

        MessageEditAction editAction = message
                .editMessage(MessageEditData.fromEmbeds(builder.build()))
                .setFiles(List.of());
        editAction.queue();
    }
}
