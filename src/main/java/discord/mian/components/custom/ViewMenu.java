package discord.mian.components.custom;

import discord.mian.commands.custom.Menu;
import discord.mian.components.Component;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class ViewMenu extends Component<ButtonInteractionEvent> {
    public ViewMenu() {
        super("menu");
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            event.deferEdit().queue();
            Menu.createMenu(event.getMessage());
            return true;
        }

        return false;
    }
}
