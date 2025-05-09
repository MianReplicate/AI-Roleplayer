package discord.mian.components.custom;

import discord.mian.components.Component;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class InspectHistory extends Component<ButtonInteractionEvent> {
    public InspectHistory(){
        super("inspect_history");
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferReply().setEphemeral(true).queue();
//            event.getHook().editOriginalComponents()
            return true;
        }

        return false;
    }
}
