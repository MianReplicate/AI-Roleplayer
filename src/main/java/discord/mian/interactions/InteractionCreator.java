package discord.mian.interactions;

import discord.mian.custom.Constants;
import discord.mian.custom.SizeHashMap;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public class InteractionCreator {
    private static final int MAX_INTERACTIONS = 1000;

    private static final HashMap<String, Consumer<? super GenericInteractionCreateEvent>> permInteractions = new HashMap<>();
    private static final HashMap<String, Consumer<? super GenericInteractionCreateEvent>> interactions = new SizeHashMap<>(MAX_INTERACTIONS);

    public static Consumer<? super GenericInteractionCreateEvent> getComponentConsumer(String id){
        return permInteractions.getOrDefault(id, interactions.getOrDefault(id, null));
    }

    public static Button createButton(Emoji emoji, Consumer<ButtonInteractionEvent> eventConsumer){
        return createButton(Button.success("brb", emoji), eventConsumer);
    }

    public static Button createButton(String label, Consumer<ButtonInteractionEvent> eventConsumer){
        return createButton(Button.success("brb", label), eventConsumer);
    }

    public static Button createButton(Button button, Consumer<ButtonInteractionEvent> eventConsumer){
        String id = System.currentTimeMillis() + "-" + UUID.randomUUID();
        button = button.withId(id);

        interactions.put(id, (event) -> {
            eventConsumer.accept((ButtonInteractionEvent) event);
        });

        return button;
    }

    // permanent
    public static Button createPermanentButton(Button button, Consumer<ButtonInteractionEvent> eventConsumer){
        permInteractions.putIfAbsent(button.getId(), (event) -> {
            eventConsumer.accept((ButtonInteractionEvent) event);
        });
        return button;
    }

    public static StringSelectMenu.Builder createStringMenu(Consumer<StringSelectInteractionEvent> eventConsumer){
        String id = System.currentTimeMillis() + "-" + UUID.randomUUID();
        StringSelectMenu.Builder selectMenu = StringSelectMenu.create(id);

        interactions.put(id, (event) -> {
            eventConsumer.accept((StringSelectInteractionEvent) event);
        });

        return selectMenu;
    }

    public static Modal createPermanentModal(Modal modal, Consumer<ModalInteractionEvent> eventConsumer){
        permInteractions.putIfAbsent(modal.getId(), (event) -> {
            eventConsumer.accept((ModalInteractionEvent) event);
        });
        return modal;
    }

    public static Modal.Builder createModal(String label, Consumer<ModalInteractionEvent> eventConsumer){
        String id = System.currentTimeMillis() + "-" + UUID.randomUUID();
        Modal.Builder modal = Modal.create(id, label);

        interactions.put(id, (event) -> {
            eventConsumer.accept((ModalInteractionEvent) event);
        });
        return modal;
    }
}
