package discord.mian.components.custom;

import discord.mian.components.Component;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class Destroy extends Component<ButtonInteractionEvent> {
    public Destroy(){
        super("destroy");
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){

        }

        return false;
    }
}
