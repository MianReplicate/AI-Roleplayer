package discord.mian.modals;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public abstract class Modal {
    public final String id;

    protected Modal(String id){
        this.id = id;
    }

    public abstract void handle(ModalInteractionEvent event) throws Exception;
}
