package discord.mian.modals.custom;

import discord.mian.ai.AIBot;
import discord.mian.ai.data.Server;
import discord.mian.api.Data;
import discord.mian.components.Components;
import discord.mian.components.custom.ViewInstalledDatas;
import discord.mian.modals.Modal;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class PromptEditor extends Modal {

    public PromptEditor(){
        super("edit_prompt");
    }

    @Override
    public void handle(ModalInteractionEvent event) throws Exception {
        event.deferEdit().queue();
        boolean isCharacter = event.getValue("type").getAsString().equals("character");

        String promptName = event.getValue("name").getAsString();
        String prompt = event.getValue("prompt").getAsString();
        Server server = AIBot.bot.getServerData(event.getGuild());

        Data data = isCharacter ? server.getCharacterDatas().get(promptName)
                : server.getInstructionDatas().get(promptName);

        if(data != null){
            data.addOrReplacePrompt(prompt);
        } else {
            if(isCharacter)
                server.createCharacter(promptName, prompt);
            else
                server.createInstruction(promptName, prompt);
        }

        String id = event.getMessage().getEmbeds().getFirst().getFooter().getText()
                .contains("Displaying Characters") ? "characters" : "instructions";
        ViewInstalledDatas menu = ((ViewInstalledDatas) Components.components.stream().filter(component -> component.id.equals(id))
                .findFirst().get());
        menu.createInstalledDatasMenu(event.getMessage(), 0);
    }

    public static void replyPromptEditor(GenericComponentInteractionCreateEvent event, String type, String promptName, String prompt){
        event.replyModal(
                net.dv8tion.jda.api.interactions.modals.Modal.create("edit_prompt", promptName != null ? "Editing " + promptName : "Creating Prompt").addComponents(
                        ActionRow.of(
                                TextInput.create("type", "Type (DO NOT CHANGE THIS)", TextInputStyle.SHORT)
                                        .setValue(type)
                                        .build()
                        ),
                        ActionRow.of(
                                TextInput.create("name", "Name (DON'T CHANGE THIS IF EDITING A PROMPT!)", TextInputStyle.SHORT)
                                        .setPlaceholder("If there is no name here, enter one!")
                                        .setValue(promptName)
                                        .build()
                        ),
                        ActionRow.of(
                                TextInput.create("prompt", "Prompt: {{char}} represents the character", TextInputStyle.PARAGRAPH)
                                        .setPlaceholder("Edit the prompt. {{char}} represents the character")
                                        .setValue(prompt)
                                        .build()
                        )).build()
        ).queue();
    }
}
