package discord.mian.components.custom;

import discord.mian.components.Component;
import discord.mian.modals.custom.PromptEditor;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class CreatePrompt extends Component<ButtonInteractionEvent> {
    public CreatePrompt(){
        super("create_prompt");
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            boolean isCharacter = event.getMessage().getEmbeds().getFirst().getFooter().getText().contains("Displaying Characters");
            PromptEditor.replyPromptEditor(event, isCharacter ? "character" : "instruction", null, null);
            return true;
        }
        return false;
    }
}
