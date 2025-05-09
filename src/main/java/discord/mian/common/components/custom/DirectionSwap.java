package discord.mian.common.components.custom;

import discord.mian.common.components.Component;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class DirectionSwap extends Component<ButtonInteractionEvent> {
    public DirectionSwap(String direction){
        super(direction);
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            if(id.equals("back")){

            } else if(id.equals("next")){

            }

            return true;
        }
        return false;
    }
}
