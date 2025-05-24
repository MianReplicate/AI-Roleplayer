package discord.mian.interactions;

import discord.mian.custom.Constants;
import discord.mian.custom.SizeHashMap;
import net.dv8tion.jda.api.components.ActionComponent;
import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.replacer.ComponentReplacer;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.concurrent.DelayedCompletableFuture;
import net.dv8tion.jda.internal.components.selections.StringSelectMenuImpl;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class InteractionCreator {
    private static final int MAX_INTERACTIONS = 1000;

    private static final HashMap<String, Consumer<? super GenericInteractionCreateEvent>> PERM_INTERACTIONS = new HashMap<>();
    private static final HashMap<String, Consumer<? super GenericInteractionCreateEvent>> INTERACTIONS = new HashMap<>();
    private static final HashMap<String, Consumer<ModalInteractionEvent>> MODALS = new SizeHashMap<>(MAX_INTERACTIONS);
    private static final HashMap<Long, CompletableFuture<Void>> TIMEOUTS = new HashMap<>();

    static {
        Interactions.getContinue();
    }

    public static Consumer<Message> queueTimeoutComponents(Long length) {
        Consumer<Message> removeConsumer = msg -> {
            TIMEOUTS.remove(msg.getIdLong());
            msg.getComponentTree().findAll(ActionComponent.class, ActionComponent::isDisabled)
                    .forEach(component -> INTERACTIONS.remove(component.getCustomId()));
        };

        return message -> {
            if(TIMEOUTS.containsKey(message.getIdLong())){
                Constants.LOGGER.info("found previous timeout, cancelling!");
                TIMEOUTS.get(message.getIdLong()).cancel(true);
                TIMEOUTS.remove(message.getIdLong());
            }

            TIMEOUTS.put(message.getIdLong(), message.editMessageComponents(
                            message.getComponentTree().replace(ComponentReplacer.of(
                                    ActionComponent.class,
                                    component -> !PERM_INTERACTIONS.containsKey(component.getCustomId()),
                                    component -> component.withDisabled(true)))
                    ).useComponentsV2()
                    .onErrorMap(throwable -> message)
                    .submitAfter(length != null ? length : 5, TimeUnit.SECONDS)
                    .thenAcceptAsync(removeConsumer)
                    .exceptionallyAsync(throwable -> {
                        removeConsumer.accept(message);
                        return null;
                    }));
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

        MODALS.put(id, (event) -> {
            eventConsumer.accept(event);
            MODALS.remove(id);
        });
        return modal;
    }
}
