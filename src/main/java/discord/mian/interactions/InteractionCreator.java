package discord.mian.interactions;

import discord.mian.custom.SizeHashMap;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public class InteractionCreator {
    private static final int MAX_INTERACTIONS = 1000;

    private static final HashMap<String, Consumer<? super GenericInteractionCreateEvent>> PERM_INTERACTIONS = new HashMap<>();
    private static final HashMap<String, Consumer<? super GenericInteractionCreateEvent>> INTERACTIONS = new SizeHashMap<>(MAX_INTERACTIONS);
    private static final HashMap<String, Consumer<ModalInteractionEvent>> MODALS = new SizeHashMap<>(300);

    static {
        Interactions.getContinue();
    }

    public static Consumer<Message> queueTimeoutComponents(Long length) {
        return message -> {
//            message.editMessageComponents(
//                    message.getComponentTree().replace(ComponentReplacer.of(
//                            Button.class,
//                            button -> !PERM_INTERACTIONS.containsKey(button.getCustomId()),
//                            button -> button.withDisabled(true)
//                    )).replace(ComponentReplacer.of(
//                            StringSelectMenuImpl.class,
//                            menu -> !PERM_INTERACTIONS.containsKey(menu.getCustomId()),
//                            menu -> menu.withDisabled(true)
//                    ))
//            ).useComponentsV2()
//                    .delay(length != null ? length : 5, TimeUnit.SECONDS)
//                    .queue(success -> Constants.LOGGER.info("GRRR"),
//                    grr -> Constants.LOGGER.info("bark?"));
        };
    }

    public static Consumer<? super GenericInteractionCreateEvent> getComponentConsumer(String id) {
        return PERM_INTERACTIONS.getOrDefault(id, INTERACTIONS.getOrDefault(id, null));
    }

    public static Consumer<ModalInteractionEvent> getModalConsumer(String id) {
        return MODALS.get(id);
    }

    public static Button createButton(Emoji emoji, Consumer<ButtonInteractionEvent> eventConsumer) {
        return createButton(Button.success("brb", emoji), eventConsumer);
    }

    public static Button createButton(String label, Consumer<ButtonInteractionEvent> eventConsumer) {
        return createButton(Button.success("brb", label), eventConsumer);
    }

    public static Button createButton(Button button, Consumer<ButtonInteractionEvent> eventConsumer) {
        String id = System.currentTimeMillis() + "-" + UUID.randomUUID();
        button = button.withCustomId(id);

        INTERACTIONS.put(id, (event) -> {
            eventConsumer.accept((ButtonInteractionEvent) event);
        });

        return button;
    }

    // permanent
    public static Button createPermanentButton(Button button, Consumer<ButtonInteractionEvent> eventConsumer) {
        PERM_INTERACTIONS.putIfAbsent(button.getCustomId() + "_button", (event) -> {
            eventConsumer.accept((ButtonInteractionEvent) event);
        });
        button = button.withCustomId(button.getCustomId() + "_button");
        return button;
    }

    public static StringSelectMenu.Builder createStringMenu(Consumer<StringSelectInteractionEvent> eventConsumer) {
        String id = System.currentTimeMillis() + "-" + UUID.randomUUID();
        StringSelectMenu.Builder selectMenu = StringSelectMenu.create(id);

        INTERACTIONS.put(id, (event) -> {
            eventConsumer.accept((StringSelectInteractionEvent) event);
        });

        return selectMenu;
    }

    public static Modal createPermanentModal(Modal.Builder modal, Consumer<ModalInteractionEvent> eventConsumer) {
        PERM_INTERACTIONS.putIfAbsent(modal.getId() + "_modal", (event) -> {
            eventConsumer.accept((ModalInteractionEvent) event);
        });
        modal = modal.setId(modal.getId() + "_modal");
        return modal.build();
    }

    public static Modal.Builder createModal(String label, Consumer<ModalInteractionEvent> eventConsumer) {
        String id = System.currentTimeMillis() + "-" + UUID.randomUUID();
        Modal.Builder modal = Modal.create(id, label);

        MODALS.put(id, eventConsumer);
        return modal;
    }
}
