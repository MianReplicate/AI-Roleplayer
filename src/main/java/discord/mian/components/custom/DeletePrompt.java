package discord.mian.components.custom;

import discord.mian.ai.AIBot;
import discord.mian.ai.data.Server;
import discord.mian.api.Data;
import discord.mian.components.Component;
import discord.mian.components.Components;
import discord.mian.modals.custom.PromptEditor;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

public class DeletePrompt extends Component<StringSelectInteractionEvent> {
    public DeletePrompt(){
        super("delete_prompt");
    }

    @Override
    public boolean handle(StringSelectInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferEdit().queue();

            boolean isCharacter = event.getMessage().getEmbeds().getFirst().getFooter().getText().contains("Displaying Characters");

            for(SelectOption option : event.getSelectedOptions()){
                String promptName = option.getValue();

                Server server = AIBot.bot.getServerData(event.getGuild());
                Data data = isCharacter ? server.getCharacterDatas().get(promptName)
                        : server.getInstructionDatas().get(promptName);

                data.nuke();
                if(isCharacter)
                    server.getCharacterDatas().remove(promptName);
                else
                    server.getInstructionDatas().remove(promptName);
            };

            String id = event.getMessage().getEmbeds().getFirst().getFooter().getText()
                    .contains("Displaying Characters") ? "characters" : "instructions";
            ViewInstalledDatas menu = ((ViewInstalledDatas) Components.components.stream().filter(component -> component.id.equals(id))
                    .findFirst().get());
            menu.createInstalledDatasMenu(event.getMessage());
            return true;
        }
        return false;
    }
}
