package discord.mian.interactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord.mian.ai.AIBot;
import discord.mian.ai.ResponseInfo;
import discord.mian.ai.Roleplay;
import discord.mian.api.PromptInfo;
import discord.mian.api.ProviderInfo;
import discord.mian.data.CharacterData;
import discord.mian.data.InstructionData;
import discord.mian.data.Server;
import discord.mian.api.Data;
import discord.mian.custom.*;
import discord.mian.data.WorldData;
import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.container.ContainerChildComponentUnion;
import net.dv8tion.jda.api.components.filedisplay.FileDisplay;
import net.dv8tion.jda.api.components.replacer.ComponentReplacer;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalTopLevelComponent;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.tika.Tika;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class Interactions {
    public static Consumer<StringSelectInteractionEvent> getDeletionMenu(PromptType promptType){
        return (event -> {
            if(!Util.hasMasterPermission(event.getMember())){
                event.reply("nuh uh little bro bro, you dont got permission").setEphemeral(true).queue();
                return;
            }

            event.deferEdit().queue();

            for(SelectOption option : event.getSelectedOptions()){
                String promptName = option.getValue();

                Server server = AIBot.bot.getServerData(event.getGuild());
                Roleplay roleplay = AIBot.bot.getChat(event.getGuild());
                Data data = server.getDatas(promptType).get(promptName);

                if(!roleplay.isRunningRoleplay() || (roleplay.isRunningRoleplay() && !roleplay.getDatas(promptType).contains(data))){
                    try {
                        data.nuke();
                    } catch (IOException e) {
                        Constants.LOGGER.error("Failed to nuke data", e);
                    }
                }

                server.getDatas(promptType).remove(promptName);
            }

            createPromptViewer(event.getHook(), promptType);
        });
    }

    public static Consumer<StringSelectInteractionEvent> getPromptEditMenu(PromptType promptType, boolean isCreating){
        return (event -> {
            if(!Util.hasMasterPermission(event.getMember())){
                event.reply("nuh uh little bro bro, you dont got permission").setEphemeral(true).queue();
                return;
            }

            String promptName = event.getSelectedOptions().getFirst().getValue();

            try {
                if (isCreating) {
                    replyCreatingPrompt(event, promptType);
                } else {
                    replyEditingPrompt(event, promptType, promptName);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static Consumer<ButtonInteractionEvent> getDestroyMessage(){
        return event -> {
            event.deferReply().setEphemeral(true).queue();

            event.getMessage().delete().queue((ignored) ->
                    event.getHook().editOriginal("Deleted!").queue());
        };
    }

    public static Consumer<ButtonInteractionEvent> getEditMessage(){
        return (event -> {
            Roleplay roleplay = AIBot.bot.getChat(event.getGuild());
            if(roleplay.isRunningRoleplay()){
                TextInput contentInput = TextInput.create("content", "Content", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("The content that will replace this message's original content")
                        .setValue(event.getMessage().getContentRaw())
                        .build();

                event.replyModal(
                        InteractionCreator.createPermanentModal(Modal.create("edit", "Edit Message")
                                .addComponents(ActionRow.of(contentInput)), modalEvent -> {
                            String content = modalEvent.getValue("content").getAsString();
                            modalEvent.editMessage(content).queue();
                            // replaces the content at that swipe
                            roleplay.getSwipes().get(roleplay.getCurrentSwipe()).editResponse(string -> content);
                        })
                ).queue();
            } else {
                event.reply("Roleplay isn't running currently!").setEphemeral(true).queue();
            }
        });
    }

    public static Consumer<ButtonInteractionEvent> getSwipe(Direction direction){
        return event -> {
            Roleplay roleplay = AIBot.bot.getChat(event.getGuild());
            if(roleplay.isRunningRoleplay()){
                event.deferEdit().queue();
                roleplay.swipe(event, direction);
            } else {
                event.reply("Roleplay isn't running currently!").setEphemeral(true).queue();
            }
        };
    }

    public static void replyCreatingPrompt(GenericComponentInteractionCreateEvent event, PromptType promptType){
        List<ModalTopLevelComponent> components = new ArrayList<>();
        components.add(ActionRow.of(
                TextInput.create("name", "Name", TextInputStyle.SHORT)
                        .setPlaceholder("Enter a name for the new prompt!")
                        .build()
        ));
        components.add(ActionRow.of(
                TextInput.create("prompt", "Prompt: {{char}} represents the character", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("Edit the prompt. {{char}} represents the character")
                        .build()
        ));

        if(promptType == PromptType.CHARACTER){
            components.add(ActionRow.of(
                    TextInput.create("talkability", "Talkability: Put a decimal from 0.0 to 1.0", TextInputStyle.SHORT)
                            .setPlaceholder("Likelihood of responding when mentioned in chat")
                            .build()
            ));
        }

        event.replyModal(
                InteractionCreator.createModal("Creating Prompt", (modalEvent) -> {
                    String promptName = modalEvent.getValue("name").getAsString();
                    getPromptEditor(promptType, promptName).accept(modalEvent);
                }).addComponents(components).build()
        ).queue();
    }

    public static void replyEditingPrompt(GenericComponentInteractionCreateEvent event, PromptType promptType, String promptName) throws IOException {
        TextInput.Builder promptInput = TextInput.create("prompt", "Prompt: {{char}} represents the character", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Edit the prompt. {{char}} represents the character");

        Server server = AIBot.bot.getServerData(event.getGuild());
        Data data = server.getDatas(promptType).get(promptName);

        if(data == null)
            throw new RuntimeException(promptName + " is not a valid prompt!");

        promptInput.setValue(data.getPrompt());

        List<ModalTopLevelComponent> components = new ArrayList<>();
        components.add(ActionRow.of(promptInput.build()));
        if(promptType == PromptType.CHARACTER){
            components.add(ActionRow.of(
                    TextInput.create("talkability", "Talkability: Put a decimal from 0.0 to 1.0", TextInputStyle.SHORT)
                            .setPlaceholder("Likelihood of responding when mentioned in chat")
                            .setValue(String.valueOf(((CharacterData) data).getTalkability()))
                            .build()
            ));
        }

        event.replyModal(
                InteractionCreator.createModal("Editing " + promptName, getPromptEditor(promptType, promptName)).addComponents(components)
                        .build()
        ).queue();
    }

    private static Consumer<ModalInteractionEvent> getPromptEditor(PromptType promptType, String promptName){
        return (event -> {
            event.deferEdit().queue();

            String name = promptName != null ? promptName : event.getValue("name").getAsString();
            String prompt = event.getValue("prompt").getAsString();

            Function<String, Double> tryParse = (string) -> {
                try{
                    return Double.parseDouble(string);
                } catch(Exception ignored){
                    return 0.5;
                }
            };

            double talkability = Math.min(1, Math.max(0, event.getValue("talkability") != null ?
                    tryParse.apply(event.getValue("talkability").getAsString()) : 0.5));
            Server server = AIBot.bot.getServerData(event.getGuild());

            Data data = server.getDatas(promptType).get(promptName);

            try{
                if(data != null){
                    data.addOrReplacePrompt(prompt);
                    if(promptType == PromptType.CHARACTER)
                        ((CharacterData) data).setTalkability(talkability);
                } else {
                    switch(promptType){
                        case CHARACTER -> server.createCharacter(name, prompt, talkability);
                        case WORLD -> server.createWorld(name, prompt);
                        case INSTRUCTION -> server.createInstruction(name, prompt);
                    }
                }

            }catch(IOException e){
                Constants.LOGGER.error("Failed to edit/add prompts", e);
            }

            createPromptViewer(event.getHook(), promptType);
        });
    }

    public static List<SelectOption> getOptionsFromViewer(String description){
        String[] toExclude = {":", "✅"};

        if(!description.equals("```\n```")){
            return List.of(description.split("\n"))
                    .stream().filter(string -> !string.contains("```"))
                    .map(string -> {
                        for(String possibleExclude : toExclude){
                            int exclude = string.indexOf(possibleExclude);
                            if(exclude != -1)
                                string = string.substring(0, exclude);
                        }
                        return SelectOption.of(string, string);
                    }).toList();
        }
        return null;
    }

    public static List<ContainerChildComponent> createConfigViewerContainer(Message message, Direction direction){
        Server server = AIBot.bot.getServerData(message.getGuild());
        HashMap<String, ConfigEntry> configEntries = server.getConfig();
        List<String> display = configEntries.entrySet().stream().filter(entry ->
                !entry.getValue().hidden).map(Map.Entry::getKey).toList();

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("# Configuration"));

        int show = 10;
        int maxSize = Math.max(1, (int) Math.ceil((double) display.size() / show));

        int index = 0;
        if(!message.getComponents().isEmpty()){
            Optional<ContainerChildComponentUnion> footerUnion = message.getComponents().getFirst().asContainer().getComponents().stream()
                    .filter(component -> component.getUniqueId() == 121)
                    .findFirst();
            if(footerUnion.isPresent()){
                String footerText = footerUnion.get().asTextDisplay().getContent();
                int indexOfSlash = footerText.indexOf("/");
                if(indexOfSlash != -1)
                    index = Integer.parseInt(footerText.substring(indexOfSlash - 1, indexOfSlash)) - 1;
                // gets the number before the /
            }
        }
        if(direction != null){
            if(direction == Direction.NEXT)
                index++;
            if(direction == Direction.BACK)
                index--;
        }
        index = Math.max(0, Math.min(index, maxSize - 1));

        int start = index * show;
        int end = Math.min(start + show, display.size());
        start = Math.min(start, end);

        StringBuilder description = new StringBuilder("```\n");

        display = display.subList(start, end);
        for(int i = 0; i < display.size(); i++){
            String string = display.get(i);
            description.append(string + ": " + configEntries.get(string).description);

            description.append("\n");
        }
        description.append("```");
        components.add(TextDisplay.of(description.toString()).withUniqueId(100));

        components.add(TextDisplay.of("-# Displaying Configuration: "+(index+1)+"/"+maxSize).withUniqueId(121));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        return components;
    }

    public static void createConfigViewer(InteractionHook hook, int forceIndex){
        Consumer<Message> consumer = message -> {
            List<ContainerChildComponentUnion> components = new ArrayList<>(message.getComponents().getFirst().asContainer().getComponents());

            components.stream().filter(component -> component.getUniqueId() == 121)
                    .findFirst().ifPresentOrElse(footer -> {
                        String footerText = footer.asTextDisplay().getContent();
                        int slash = footerText.indexOf("/");
                        if(slash != -1){
                            footerText = footerText.substring(0, slash - 1)
                                    + forceIndex + footerText.substring(slash);
                        }
                        message.editMessageComponents(message.getComponentTree()
                                .replace(ComponentReplacer.byId(121, footer.asTextDisplay().withContent(footerText)))
                                .getComponents()).queue(ignored ->
                                createConfigViewer((direction) -> createConfigViewerContainer(message, null), hook, null));
                    }, () -> createConfigViewer((direction) -> createConfigViewerContainer(message, null), hook, null));
        };

        if(hook.hasCallbackResponse()){
            consumer.accept(hook.getCallbackResponse().getMessage());
        } else {
            hook.retrieveOriginal().queue(consumer, onFail -> Constants.LOGGER.error("Failed to create config viewer", onFail));
        }
    }

    public static void createConfigViewer(Function<Direction, List<ContainerChildComponent>> builder, InteractionHook hook, Direction direction){
        Consumer<Message> consumer = message -> {
            List<ContainerChildComponent> components = builder.apply(direction);

            TextDisplay descriptionOptions =
                    (TextDisplay) components.stream().filter(child -> child.getUniqueId() == 100)
                            .findFirst().orElseThrow();
            List<SelectOption> options =
                    getOptionsFromViewer(descriptionOptions.getContent());
            components.add(
                    ActionRow.of(InteractionCreator.createStringMenu((event -> {
                                String configOption = event.getSelectedOptions().getFirst().getValue();
                                ConfigEntry entry = AIBot.bot.getServerData(event.getGuild()).getConfig().get(configOption);

                                TextInput.Builder textInput = TextInput.create("value", "Value", TextInputStyle.SHORT)
                                        .setPlaceholder("Enter a valid value: For booleans, type \"true\" or \"false\".");
                                if(entry instanceof ConfigEntry.StringConfig stringConfig && !stringConfig.value.isEmpty())
                                    textInput.setValue(stringConfig.value);
                                else if(entry instanceof ConfigEntry.BoolConfig boolConfig)
                                    textInput.setValue(String.valueOf(boolConfig.value));

                                event.replyModal(InteractionCreator.createModal("Editing " + configOption.toUpperCase(), (modalEvent) -> {
                                    String value = modalEvent.getValue("value").getAsString();
                                    HashMap<String, ConfigEntry> entries = AIBot.bot.getServerData(modalEvent.getGuild()).getConfig();

                                    if(entry instanceof ConfigEntry.StringConfig stringConfig)
                                        stringConfig.value = value;
                                    else if(entry instanceof ConfigEntry.BoolConfig boolConfig){
                                        Function<String, Boolean> tryParseBoolean = (string) -> {
                                            try{
                                                return Boolean.valueOf(string);
                                            } catch (Exception ignored){
                                                return ((ConfigEntry.BoolConfig) entry).value;
                                            }
                                        };

                                        boolConfig.value = tryParseBoolean.apply(value);
                                    }

                                    entries.put(configOption, entry);

                                    if(AIBot.bot.getServerData(modalEvent.getGuild()).saveToConfig(entries))
                                        modalEvent.reply("Saved config!").setEphemeral(true).queue();
                                    else
                                        modalEvent.reply("Failed to update config!").setEphemeral(true).queue();
                                }).addComponents(
                                        ActionRow.of(textInput.build())).build()).queue();
                            }))
                            .setRequiredRange(1, 1)
                            .setPlaceholder("Configure Option")
                            .addOptions(options)
                            .build())
            );

            components.add(ActionRow.of(
                    InteractionCreator.createButton("<--", (event) -> {
                        event.deferEdit().queue();
                        createConfigViewer(builder, hook, Direction.BACK);

                    }).withStyle(ButtonStyle.SECONDARY),
                    InteractionCreator.createButton("-->", (event) -> {
                        event.deferEdit().queue();
                        createConfigViewer(builder, hook, Direction.NEXT);

                    }).withStyle(ButtonStyle.SECONDARY)
            ));

            components.add(Separator.createDivider(Separator.Spacing.SMALL));

            ArrayList<Button> itemComponents = new ArrayList<>();
            itemComponents.add(InteractionCreator.createButton("View Dashboard", (event) -> {
                event.deferEdit().queue();
                try {
                    createDashboard(event.getMessage());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).withEmoji(Emoji.fromFormatted("🔝")).withStyle(ButtonStyle.PRIMARY));

            components.add(ActionRow.of(itemComponents));


            MessageEditAction editAction = message
                    .editMessageComponents(Util.createBotContainer(components))
                    .useComponentsV2()
                    .setFiles(List.of());
            editAction.queue(InteractionCreator.queueTimeoutComponents(null));
        };

        if(hook.hasCallbackResponse()){
            consumer.accept(hook.getCallbackResponse().getMessage());
        } else {
            hook.retrieveOriginal().queue(consumer, onFail -> Constants.LOGGER.error("Failed to create config viewer", onFail));
        }
    }


    public static List<ContainerChildComponent> createPromptViewerContainer(Message message,
                                                                            String preDescription,
                                                                            PromptType promptType,
                                                                            Direction direction,
                                                                            BiFunction<PromptType, String, String> displayItem
    ){
        Server server = AIBot.bot.getServerData(message.getGuild());
        List<String> display = server.getDatas(promptType).keySet().stream().toList();

        List<ContainerChildComponent> components = new ArrayList<>();
        components.add(TextDisplay.of("# Available Prompts"));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));

        if(preDescription != null){
            components.add(TextDisplay.of(preDescription));
            components.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        int show = 10;
        int maxSize = Math.max(1, (int) Math.ceil((double) display.size() / show));

        int index = 0;

        if(!message.getComponents().isEmpty()){
            Optional<ContainerChildComponentUnion> footerUnion = message.getComponents().getFirst().asContainer().getComponents().stream()
                    .filter(component -> component.getUniqueId() == 121)
                    .findFirst();
            if(footerUnion.isPresent()){
                String footerText = footerUnion.get().asTextDisplay().getContent();
                int indexOfSlash = footerText.indexOf("/");
                if(indexOfSlash != -1)
                    index = Integer.parseInt(footerText.substring(indexOfSlash - 1, indexOfSlash)) - 1;
                // gets the number before the /
            }
        }
        if(direction != null){
            if(direction == Direction.NEXT)
                index++;
            if(direction == Direction.BACK)
                index--;
        }
        index = Math.max(0, Math.min(index, maxSize - 1));

        int start = index * show;
        int end = Math.min(start + show, display.size());
        start = Math.min(start, end);

        StringBuilder description = new StringBuilder("```\n");

        display = display.subList(start, end);
        if(displayItem == null)
            displayItem = (type, string) -> string;
        for(int i = 0; i < display.size(); i++){
            String string = display.get(i);
            description.append(displayItem.apply(promptType, string));

            description.append("\n");
        }
        description.append("```");
        components.add(TextDisplay.of(description.toString()).withUniqueId(100));

        components.add(TextDisplay.of("-# Displaying "+promptType.displayName+": "+(index+1)+"/"+maxSize).withUniqueId(121));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        return components;
    }

    public static void createPromptViewer(InteractionHook hook, PromptType promptType){
        Consumer<Message> consumer = message -> createPromptViewer(message, promptType, null);

        if(hook.hasCallbackResponse()){
            consumer.accept(hook.getCallbackResponse().getMessage());
        } else {
            hook.retrieveOriginal().queue(consumer, e -> Constants.LOGGER.error("Failed to get message for prompt viewer", e));
        }
    }

    public static void createPromptViewer(InteractionHook hook, PromptType promptType, int forceIndex){
        Consumer<Message> consumer = message -> {

            List<ContainerChildComponentUnion> components = new ArrayList<>(message.getComponents().getFirst().asContainer().getComponents());

            components.stream().filter(component -> component.getUniqueId() == 121)
                    .findFirst().ifPresentOrElse(footer -> {
                        String footerText = footer.asTextDisplay().getContent();
                        int slash = footerText.indexOf("/");
                        if(slash != -1){
                            footerText = footerText.substring(0, slash - 1)
                                    + forceIndex + footerText.substring(slash);
                        }
                        message.editMessageComponents(message.getComponentTree()
                                .replace(ComponentReplacer.byId(121, footer.asTextDisplay().withContent(footerText)))
                                .getComponents()).queue((ignored) ->
                                createPromptViewer(message, promptType, null), t -> Constants.LOGGER.error("Failed to create prompt viewer", t));
                    }, () -> createPromptViewer(message, promptType, null));

        };

        if(hook.hasCallbackResponse()){
            consumer.accept(hook.getCallbackResponse().getMessage());
        } else {
            hook.retrieveOriginal().queue(consumer, e -> Constants.LOGGER.error("Failed to get message for prompt viewer", e));
        }
    }

    public static void createPromptViewer(Message message, PromptType promptType, Direction direction){
        createPromptViewer((direction1) -> createPromptViewerContainer(message, null, promptType, direction1, null),
                message, promptType, true, true,
                null, direction);
    }

    public static void createPromptViewer(Function<Direction, List<ContainerChildComponent>> buildContainer, Message message, PromptType promptType, boolean editable, boolean canGoBack,
                                          List<StringSelectMenu.Builder> onSelects, Direction direction, Button... buttons){
        List<ContainerChildComponent> components = buildContainer.apply(direction);

        TextDisplay descriptionOptions =
                (TextDisplay) components.stream().filter(child -> child.getUniqueId() == 100)
                        .findFirst().orElseThrow();
        List<SelectOption> options =
                getOptionsFromViewer(descriptionOptions.getContent());
        if(options != null){
            if(editable){
                components.add(
                        ActionRow.of(InteractionCreator.createStringMenu(Interactions.getPromptEditMenu(promptType, false))
                                .setRequiredRange(1, 1)
                                .setPlaceholder("Edit Prompt")
                                .addOptions(options)
                                .build())
                );
                components.add(
                        ActionRow.of(InteractionCreator.createStringMenu(Interactions.getDeletionMenu(promptType))
                                .setMaxValues(25)
                                .setPlaceholder("Delete Prompts")
                                .addOptions(options)
                                .build())
                );
            }
            if(onSelects != null){
                for(StringSelectMenu.Builder builder : onSelects){
                    StringSelectMenu.Builder copy = StringSelectMenu.create(builder.getCustomId())
                            .addOptions(builder.getOptions())
                            .setMinValues(builder.getMinValues())
                            .setMaxValues(builder.getMaxValues())
                            .setPlaceholder(builder.getPlaceholder());

                    components.add(ActionRow.of(
                            copy.addOptions(options)
                                    .build()));
                }
            }
        }
        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(ActionRow.of(
                InteractionCreator.createButton("<--", (event) -> {
                    event.deferEdit().queue();
                    createPromptViewer(buildContainer, message, promptType, editable, canGoBack, onSelects, Direction.BACK, buttons);

                }).withStyle(ButtonStyle.SECONDARY),
                InteractionCreator.createButton("-->", (event) -> {
                    event.deferEdit().queue();
                    createPromptViewer(buildContainer, message, promptType, editable, canGoBack, onSelects, Direction.NEXT, buttons);

                }).withStyle(ButtonStyle.SECONDARY)
        ));

        if(buttons.length > 0){
            components.add(Separator.createDivider(Separator.Spacing.SMALL));
            components.add(ActionRow.of(buttons));
        }

        components.add(Separator.createDivider(Separator.Spacing.SMALL));

        if(canGoBack || editable){
            ArrayList<Button> itemComponents = new ArrayList<>();
            if(canGoBack)
                itemComponents.add(InteractionCreator.createButton("View Dashboard", (event) -> {
                    event.deferEdit().queue();
                    try {
                        createDashboard(event.getMessage());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).withEmoji(Emoji.fromFormatted("🔝")).withStyle(ButtonStyle.PRIMARY));
            if(editable)
                itemComponents.add(InteractionCreator.createButton("Create Prompt", (event) -> {
                    if(!Util.hasMasterPermission(event.getMember())){
                        event.reply("nuh uh little bro bro, you dont got permission").setEphemeral(true).queue();
                        return;
                    }

                    replyCreatingPrompt(event, promptType);
                }).withEmoji(Emoji.fromFormatted("🪄")));

            components.add(ActionRow.of(itemComponents));
        }

        MessageEditAction editAction = message
                .editMessageComponents(Util.createBotContainer(components))
                .useComponentsV2()
                .setFiles(List.of());
        editAction.queue(InteractionCreator.queueTimeoutComponents(null), e -> Constants.LOGGER.error("Failed to get message for prompt viewer", e));
    }

    public static void createDashboard(Message message) throws IOException {
        Roleplay roleplay = AIBot.bot.getChat(message.getGuild());

        boolean isGif = Cats.isGif();
        Random random = new Random();
        byte[] data = random.nextBoolean() ? Cats.getCat() : Util.getRandomImage();
        if(data == null)
            data = Cats.getCat();
        String fileName = isGif ? "image.gif" : "image.png";

        Server server = AIBot.bot.getServerData(message.getGuild());
        String key = server.getKey();

        ArrayList<Button> roleplayComponents = new ArrayList<>();

        roleplayComponents.add(InteractionCreator.createButton("Create Roleplay", (event) -> {
            if(server.getInstructionDatas().isEmpty() || server.getCharacterDatas().isEmpty() || server.getWorldDatas().isEmpty()){
                event.reply("Must at least have one instruction, character and world created in the bot in order to start a roleplay!").setEphemeral(true).queue();
                return;
            }

            if(key == null || key.isEmpty()){
                event.reply("No API key set! One must be set in the configuration first in order to roleplay!").setEphemeral(true).queue();
                return;
            }

            event.replyModal(InteractionCreator.createModal("Name Roleplay", modal -> {
                modal.deferEdit().queue();

                String name = modal.getValue("name").getAsString();

                HashMap<PromptType, ArrayList<String>> datas = new HashMap<>();
                datas.put(PromptType.INSTRUCTION, new ArrayList<>());
                datas.put(PromptType.WORLD, new ArrayList<>());
                datas.put(PromptType.CHARACTER, new ArrayList<>());

                BiConsumer<StringSelectInteractionEvent, HashMap<String, ? extends Data>> viewPromptConsumer =
                        (onSelect, dataList) -> {
                            onSelect.deferReply(true).queue();
                            try {
                                Data prompt = dataList.get(onSelect.getSelectedOptions().getFirst().getValue());
                                onSelect.getHook()
                                        .sendFiles(FileUpload.fromData(prompt.getPromptFile()))
                                        .setEphemeral(true)
                                        .queue();
                            } catch (Exception e) {
                                onSelect.getHook().editOriginal("Failed to retrieve the prompt!").queue();
                            }
                        };

                BiFunction<PromptType, String, String> enabledData = (promptType, string) -> datas.get(promptType).contains(string) ? string + "✅" : string;

                Consumer<Integer> nextPromptType = new Consumer<>() {
                    @Override
                    public void accept(Integer nextInt) {
                        PromptType promptType = PromptType.values()[nextInt];
                        String display = promptType.displayName.toLowerCase();

                        ArrayList<StringSelectMenu.Builder> selects = new ArrayList<>();

                        selects.add(InteractionCreator.createStringMenu(onSelect -> {
                            onSelect.deferEdit().queue();
                            onSelect.getSelectedOptions().forEach(selectOption -> {
                                ArrayList<String> prompts = datas.get(promptType);
                                String option = selectOption.getValue();
                                if (!prompts.contains(option))
                                    prompts.add(option);
                                else
                                    prompts.remove(option);
                            });
                            accept(nextInt);
                        }).setMaxValues(25).setPlaceholder("Add/Remove Prompts"));

                        selects.add(
                                InteractionCreator.createStringMenu(onSelect ->
                                                viewPromptConsumer.accept(onSelect, server.getDatas(promptType)))
                                        .setRequiredRange(1, 1)
                                        .setPlaceholder("View Prompts")
                        );

                        Consumer<Message> onMessage = givenMsg -> {
                            createPromptViewer((direction) -> createPromptViewerContainer(
                                            givenMsg,
                                            "Select "+display+" prompts to use in the roleplay.",
                                            promptType,
                                            direction, enabledData), givenMsg, promptType, false, true,
                                    selects, null, InteractionCreator.createButton(Emoji.fromFormatted("✅"), buttonEvent -> {
                                        if (datas.get(promptType).isEmpty()) {
                                            buttonEvent.reply("Need at least one set of "+display+"!").setEphemeral(true).queue();
                                            return;
                                        }
                                        if(nextInt + 1 >= PromptType.values().length){
                                            buttonEvent.getMessage().delete().queue();

                                            try {
                                                roleplay.startRoleplay(
                                                        buttonEvent,
                                                        name,
                                                        datas.get(PromptType.INSTRUCTION).stream().map(string -> server.getInstructionDatas().get(string)).toList(),
                                                        datas.get(PromptType.WORLD).stream().map(string -> server.getWorldDatas().get(string)).toList(),
                                                        datas.get(PromptType.CHARACTER).stream().map(string -> server.getCharacterDatas().get(string)).toList(),
                                                        hook ->
                                                                roleplay.getDatas(PromptType.CHARACTER).forEach((chrData) ->
                                                                        roleplay.promptCharacterToRoleplay((CharacterData) chrData, null, false))
                                                );
                                            } catch (ExecutionException | InterruptedException | IOException e) {
                                                buttonEvent.reply("Failed to start chat!").setEphemeral(true).queue();
                                            }
                                        } else {
                                            buttonEvent.deferEdit().queue();
                                            accept(nextInt + 1);
                                        }
                                    }));
                        };

                        InteractionHook hook = event.getHook();
                        if(hook.hasCallbackResponse()){
                            onMessage.accept(hook.getCallbackResponse().getMessage());
                        } else {
                            hook.retrieveOriginal().queue(onMessage, onFail -> Constants.LOGGER.error("Failed to continue creating roleplay", onFail));
                        }
                    }
                };

                nextPromptType.accept(0);
            }).addComponents(ActionRow.of(
                    TextInput.create("name", "Name", TextInputStyle.SHORT)
                            .setRequired(true)
                            .setPlaceholder("A very sussy roleplay")
                            .build()
            )).build()).queue();
        }).withEmoji(Emoji.fromFormatted("🪄"))
                .withStyle(ButtonStyle.PRIMARY).withDisabled(message.getChannelType().isThread()));

        roleplayComponents.add(InteractionCreator.createButton("Stop Roleplay", (event) ->
                {
                    if(roleplay.isMakingResponse()){
                        event.reply("Cannot stop the roleplay while a message is being generated!").setEphemeral(true).queue();
                        return;
                    }
                    event.deferEdit().queue();
                    roleplay.stopRoleplay();
                    try {
                        createDashboard(event.getMessage());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .withStyle(ButtonStyle.DANGER)
                .withEmoji(Emoji.fromFormatted("🛑"))
                .withDisabled(!roleplay.isRunningRoleplay()));

        roleplayComponents.add(InteractionCreator.createButton("Restart Roleplay", (event) -> {
            if(roleplay.isMakingResponse()){
                event.reply("Cannot restart the roleplay while a message is being generated!").setEphemeral(true).queue();
                return;
            }
                    event.replyModal(InteractionCreator.createModal("Name Roleplay", modal -> {
                        String name = modal.getValue("name").getAsString();

                        try {
                            roleplay.startRoleplay(
                                    modal,
                                    name,
                                    roleplay.getDatas(PromptType.INSTRUCTION).stream().map(dat -> (InstructionData) dat).toList(),
                                    roleplay.getDatas(PromptType.WORLD).stream().map(dat -> (WorldData) dat).toList(),
                                    roleplay.getDatas(PromptType.CHARACTER).stream().map(dat -> (CharacterData) dat).toList(),
                                    hook ->
                                            roleplay.getDatas(PromptType.CHARACTER).forEach((characterData) ->
                                                    roleplay.promptCharacterToRoleplay((CharacterData) characterData, null, false)));
                        } catch (Exception e) {
                            event.getHook().editOriginal("Failed to restart chat!").queue();
                        }
                    }).addComponents(ActionRow.of(
                            TextInput.create("name", "Name", TextInputStyle.SHORT)
                                    .setRequired(true)
                                    .setPlaceholder("A very sussy roleplay")
                                    .build()
                    )).build()).queue();
        }).withStyle(ButtonStyle.SECONDARY).withEmoji(Emoji.fromFormatted("⏪"))
                .withDisabled(!roleplay.isRunningRoleplay() || message.getChannelType().isThread()));

        List<ContainerChildComponent> components = new ArrayList<>();
        components.add(Section.of(
                Thumbnail.fromFile(FileUpload.fromData(data != null ? data : Util.getRandomImage(), fileName)),
                TextDisplay.of("# Dashboard"),
                TextDisplay.of("*\""+Util.getRandomToolTip()+"\"*")
        ));

        components.add(Separator.createDivider(Separator.Spacing.SMALL));

        double remaining = 0;

        if(key != null && !key.isEmpty()){
            OkHttpClient client = new OkHttpClient.Builder().build();

            Request request = new Request.Builder()
                    .url("https://openrouter.ai/api/v1/credits")
                    .header("Authorization", "Bearer " + key)
                    .get()
                    .build();

            Call call = client.newCall(request);
            try (Response response = call.execute()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(response.body().string());
                JsonNode dataNode = node.get("data");
                if(dataNode != null){
                    double totalCredits = dataNode.get("total_credits").asDouble();
                    double totalUsage = dataNode.get("total_usage").asDouble();
                    remaining = totalCredits - totalUsage;
                }
            } catch(Exception e){
                Constants.LOGGER.error("Failed to get usage amount for key", e);
            }
        }

        String provider = roleplay.getProvider();

        components.add(TextDisplay.of("**AI Model:** " + roleplay.getModel().getDisplay()));
        components.add(TextDisplay.of("**Forced Provider:** " + (provider != null && !provider.isEmpty() ? provider : "None")));
        components.add(TextDisplay.of("**Credits Remaining:** " + "$"+String.format("%.2f", remaining)));

        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("**Temperature:** "+ roleplay.getTemperature()));
        components.add(TextDisplay.of("**Max Tokens:** "+ roleplay.getMaxTokens()));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));

//        if(roleplay.isRunningRoleplay()){
//            components.add(Separator.createDivider(Separator.Spacing.SMALL));
//            for(PromptType promptType : PromptType.values()){
//                List<? extends Data> promptDatas = roleplay.getDatas(promptType);
//
//                StringBuilder display = new StringBuilder();
//                display.append("-# ");
//                components.add(TextDisplay.of("**"+promptType.displayName+" Involved"+"**"));
//
//                for(int i = 0; i < promptDatas.size(); i++){
//                    String name = promptDatas.get(i).getName();
//                    display.append(name);
//                    if(i != promptDatas.size() - 1){
//                        display.append(", ");
//                    }
//                }
//                components.add(TextDisplay.of(display.toString()));
//            }
//        }

        components.add(TextDisplay.of("### Roleplay Status: " + (roleplay.isRunningRoleplay() ? "Ongoing" : "Stopped")));
        if(roleplay.isRunningRoleplay()){
            components.add(TextDisplay.of("[More Information]("+roleplay.getChannel().getJumpUrl()+")"));
        }
        components.add(ActionRow.of(roleplayComponents));

        components.add(Separator.createDivider(Separator.Spacing.LARGE));

        components.add(TextDisplay.of("### View Prompts"));

        components.add(ActionRow.of(
                                InteractionCreator.createButton("View Instructions", (event) -> {
                                    event.deferEdit().queue();
                                    createPromptViewer(event.getHook(), PromptType.INSTRUCTION, 0);
                                }).withEmoji(Emoji.fromFormatted("📋")).withStyle(ButtonStyle.SECONDARY),
                                InteractionCreator.createButton("View Worlds", (event) -> {
                                    event.deferEdit().queue();
                                    createPromptViewer(event.getHook(), PromptType.WORLD, 0);
                                }).withEmoji(Emoji.fromFormatted("🌍")).withStyle(ButtonStyle.SECONDARY),
                                InteractionCreator.createButton("View Characters", (event) -> {
                                    event.deferEdit().queue();
                                    createPromptViewer(event.getHook(), PromptType.CHARACTER, 0);
                                }).withEmoji(Emoji.fromFormatted("🧝")).withStyle(ButtonStyle.SECONDARY)));

        components.add(Separator.createDivider(Separator.Spacing.LARGE));

        components.add(TextDisplay.of("### Configuration"));

        components.add(ActionRow.of(
                InteractionCreator.createButton("Server Configuration", (event) -> {
                    event.deferEdit().queue();

                    if(!Util.hasMasterPermission(event.getMember())){
                        event.getHook().editOriginal("nuh uh little bro bro, you dont got permission").queue();
                        return;
                    }
                    createConfigViewer(event.getHook(), 0);
                }).withEmoji(Emoji.fromFormatted("⚙️")).withStyle(ButtonStyle.SECONDARY),
                Button.link("https://openrouter.ai/models?order=pricing-low-to-high", "Free AI models")
                        .withEmoji(Emoji.fromFormatted("🤖"))));

        MessageEditAction editAction = message
                .editMessageComponents(Util.createBotContainer(components))
                .useComponentsV2();
        editAction.queue(InteractionCreator.queueTimeoutComponents(null));
    }

    public static Button createCancellableResponse(){
        return InteractionCreator.createPermanentButton(Button.danger("cancel_response",
                Emoji.fromFormatted("🛑")), event -> {
            event.deferEdit().queue();
            AIBot.bot.getChat(event.getGuild()).cancelGeneration();
        });
    }

    public static Consumer<ButtonInteractionEvent> getResponseInfo() {
        return event -> {
            try{
                Roleplay chat = AIBot.bot.getChat(event.getGuild());
                PromptInfo responseInfo = chat.getFailedResponseInfo();
                if(responseInfo == null){
                    List<ResponseInfo> responseSwipes = chat.getSwipes();
                    if(responseSwipes == null || responseSwipes.isEmpty()){
                        throw new RuntimeException("No swipes!");
                    } else {
                        responseInfo = responseSwipes.get(chat.getCurrentSwipe());
                    }
                }

                List<ContainerChildComponent> containerComponents = new ArrayList<>();

                byte[] data = null;
                try{
                    data = Files.readAllBytes(chat.getCurrentCharacter().getAvatar().toPath());
                }catch(Exception e){
                    Constants.LOGGER.error("Failed to get avatar, using backup", e);
                }
                data = data != null ? data : Objects.requireNonNull(Util.getRandomImage());
                Tika tika = new Tika();
                String type = tika.detect(data);
                type = type.substring(type.indexOf("/")+1);

                containerComponents.add(Section.of(
                        Thumbnail.fromFile(FileUpload.fromData(data, "avatar."+type)),
                        TextDisplay.of("# Response Information"),
                        TextDisplay.of("Metadata about the generated response")
                ));
                containerComponents.add(Separator.createDivider(Separator.Spacing.SMALL));
                containerComponents.add(TextDisplay.of("-# The json file sent to the LLM for a response"));
                containerComponents.add(FileDisplay.fromFile(FileUpload.fromData(responseInfo.getPrompt().getBytes(), "prompt.json")));
                containerComponents.add(Separator.createDivider(Separator.Spacing.SMALL));

                if(responseInfo instanceof ProviderInfo providerInfo){
                    String reply = "**Model:** " + providerInfo.getModel() +
                            "\n**Provider:** " + providerInfo.getProvider() +
                            "\n**Prompt Tokens:** " + (providerInfo.getPromptTokens().isPresent() ? providerInfo.getPromptTokens().get() : "Unknown") +
                            "\n**Completion Tokens:** " + (providerInfo.getCompletionTokens().isPresent() ? providerInfo.getCompletionTokens().get() : "Unknown")+
                            "\n**Total Tokens:** " + providerInfo.getTotalTokens()+
                            "\n**Price:** " + (providerInfo.getPrice() != null ? "$" + String.format("%.4f", providerInfo.getPrice()) : "Unknown");

                    containerComponents.add(TextDisplay.of(reply));
                } else {
                    // is failed response
                    containerComponents.add(TextDisplay.of("-# The response returned by OpenRouter"));
                    containerComponents.add(FileDisplay.fromFile(FileUpload.fromData(responseInfo.getResponse().getBytes(), "response.json")));
                }

                event.replyComponents(Util.createBotContainer(containerComponents))
                        .setEphemeral(true)
                        .useComponentsV2()
                        .queue();
            }catch(Exception e){
                event.reply("Failed to retrieve response information!").setEphemeral(true).queue();
                Constants.LOGGER.info(e.toString());
            }
        };
    }

    public static Button getContinue(){
        return InteractionCreator.createPermanentButton(Button.primary("start_here", "Continue"),
                        button -> {
                            button.deferEdit().queue();
                            try{
                                AIBot.bot.getChat(button.getGuild()).startRoleplay(
                                        button.getMessage(), button.getHook(), null
                                );
                            }catch(Exception e){
                                button.getHook().editOriginal("Failed to continue roleplay!").queue();
                                Constants.LOGGER.error("Failed to continue roleplay", e);
//                                return;
                            }
//                            button.getHook().editOriginal("Continuing this thread's roleplay!").queue();
                        })
                .withEmoji(Emoji.fromFormatted("🔁"));
    }
}
