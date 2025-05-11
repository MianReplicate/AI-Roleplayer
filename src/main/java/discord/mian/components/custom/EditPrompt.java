package discord.mian.components.custom;

import discord.mian.ai.AIBot;
import discord.mian.ai.data.Server;
import discord.mian.components.Component;
import discord.mian.modals.custom.PromptEditor;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public class EditPrompt extends Component<StringSelectInteractionEvent> {
    public EditPrompt(){
        super("edit_prompt");
    }

    @Override
    public boolean handle(StringSelectInteractionEvent event) throws Exception {
        if(super.handle(event)){
            boolean isCharacter = event.getMessage().getEmbeds().getFirst().getFooter().getText().contains("Displaying Characters");
            String promptName = event.getSelectedOptions().getFirst().getValue();

            Server server = AIBot.bot.getServerData(event.getGuild());
            String prompt = isCharacter ? server.getCharacterDatas().get(promptName).getPrompt()
                    : server.getInstructionDatas().get(promptName).getPrompt();

            PromptEditor.replyPromptEditor(event, isCharacter ? "character" : "instruction", promptName, prompt);
            return true;
        }
        return false;
    }
}
