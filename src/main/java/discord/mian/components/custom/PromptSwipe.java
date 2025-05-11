package discord.mian.components.custom;

import discord.mian.components.Component;
import discord.mian.components.Components;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class PromptSwipe extends Component<ButtonInteractionEvent> {
    public PromptSwipe(String id){
        super(id+"_prompt");
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferEdit().queue();
            // characters or instructions
            String id = event.getMessage().getEmbeds().getFirst().getFooter().getText()
                    .contains("Displaying Characters") ? "characters" : "instructions";

            ViewInstalledDatas menu = ((ViewInstalledDatas) Components.components.stream().filter(component -> component.id.equals(id))
                    .findFirst().get());
            if(event.getComponentId().equals("back_prompt"))
                menu.back(event.getMessage());
            else{
                menu.next(event.getMessage());
            }

            return true;
        }

        return false;
    }
}
