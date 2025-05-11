package discord.mian.components.custom;

import discord.mian.components.Component;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class EditMessage extends Component<ButtonInteractionEvent> {
    public EditMessage() {
        super("edit");
    }

    @Override
    public boolean handle(ButtonInteractionEvent event) throws Exception {
        if(super.handle(event)){
            TextInput subject = TextInput.create("content", "Content", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("The content that will replace this message's original content")
                    .build();

            event.replyModal(
                    Modal.create("edit", "Edit Message")
                            .addComponents(ActionRow.of(subject)).build()
            ).queue();
            return true;
        }

        return false;
    }
}
