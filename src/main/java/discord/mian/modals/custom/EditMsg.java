package discord.mian.modals.custom;

import discord.mian.modals.Modal;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public class EditMsg extends Modal {
    public EditMsg(){
        super("edit");
    }

    @Override
    public void handle(ModalInteractionEvent event) throws Exception {
        String content = event.getValue("content").getAsString();
        event.editMessage(content).queue();
    }
}
