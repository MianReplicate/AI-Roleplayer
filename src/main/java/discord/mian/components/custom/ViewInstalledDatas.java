package discord.mian.components.custom;

import discord.mian.ai.AIBot;
import discord.mian.ai.DiscordRoleplay;
import discord.mian.ai.data.Server;
import discord.mian.components.Component;
import discord.mian.custom.Cats;
import discord.mian.custom.Constants;
import discord.mian.custom.Util;
import io.github.sashirestela.openai.support.Constant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.awt.*;
import java.io.IOException;
import java.sql.Connection;
import java.time.chrono.IsoEra;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ViewInstalledDatas extends Component<ButtonInteractionEvent> {
    private final HashMap<Long, Integer> indexes;

    public ViewInstalledDatas(String id) {
        super(id);
        indexes = new HashMap<>();
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferEdit().queue();

            createInstalledDatasMenu(event.getMessage());
            return true;
        }

        return false;
    }

    public void back(Message message){
        indexes.put(message.getIdLong(), Math.max(0, indexes.getOrDefault(message.getIdLong(), 0) - 1));
        createInstalledDatasMenu(message);
    }

    public void next(Message message){
        indexes.put(message.getIdLong(), indexes.getOrDefault(message.getIdLong(), 0) + 1);
        createInstalledDatasMenu(message);
    }

    public void createInstalledDatasMenu(Message message, int index){
        indexes.put(message.getIdLong(), index);
        createInstalledDatasMenu(message);
    }

    public void createInstalledDatasMenu(Message message) {
        Server server = AIBot.bot.getServerData(message.getGuild());
        List<String> display = id.equals("characters") ? server.getCharacterDatas().keySet().stream().toList() :
                server.getInstructionDatas().keySet().stream().toList();

        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle("Installed Prompts");
        builder.setAuthor("Created By Your Lovely Girl: @MianReplicate", "https://en.pronouns.page/@MianReplicate");

        builder.setColor(new Color(
                (int) (Math.random() * 256),
                (int) (Math.random() * 256),
                (int) (Math.random() * 256),
                (int) (Math.random() * 256))
        );

        int show = 10;
        int maxSize = Math.max(1, (int) Math.ceil((double) display.size() / show));
        int index = Math.min(indexes.getOrDefault(message.getIdLong(), 0), maxSize - 1);
        int start = index * show;
        int end = Math.min(start + show, display.size());
        start = Math.min(start, end);

        indexes.put(message.getIdLong(), index);

        StringBuilder description = new StringBuilder("```\n");
        display = display.subList(start, end);
        ArrayList<SelectOption> options = new ArrayList<>();
        for(int i = 0; i < display.size(); i++){
            String string = display.get(i);
            description.append(string);

            options.add(SelectOption.of(string, string));

            if(i + 1 != display.size())
                description.append("\n");
        }
        description.append("```");
        builder.setDescription(description.toString());

        builder.setFooter("Displaying "+id.substring(0, 1).toUpperCase()+id.substring(1)+": "+(index+1)+"/"+maxSize);

        ArrayList<LayoutComponent> components = new ArrayList<>();

        if(!options.isEmpty()){
            components.add(
                    ActionRow.of(StringSelectMenu.create("edit_prompt")
                            .setRequiredRange(1, 1)
                            .setPlaceholder("Edit Prompt")
                            .addOptions(options)
                            .build())
            );
            components.add(
                    ActionRow.of(StringSelectMenu.create("delete_prompt")
                                    .setMaxValues(25)
                            .setPlaceholder("Delete Prompts")
                            .addOptions(options)
                            .build())
            );
        }
        components.add(ActionRow.of(
                        Button.primary("back_prompt", "<--"),
                        Button.primary("next_prompt", "-->")
                ));
        components.add(ActionRow.of(
                Button.secondary("menu", "View Dashboard")
                .withEmoji(Emoji.fromFormatted("🔝")),
                Button.success("create_prompt", "Create Prompt")
                        .withEmoji(Emoji.fromFormatted("🪄"))));

        MessageEditAction editAction = message
                .editMessage(MessageEditData.fromEmbeds(builder.build()))
                .setComponents(components)
                .setFiles(List.of());
        editAction.queue();
    }
}
