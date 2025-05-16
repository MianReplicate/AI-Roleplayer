package discord.mian.interactions;

import discord.mian.ai.AIBot;
import discord.mian.ai.DiscordRoleplay;
import discord.mian.data.CharacterData;
import discord.mian.data.Server;
import discord.mian.api.Data;
import discord.mian.custom.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class Interactions {
//    private static final HashMap<Long, HashMap<String, List<? super Data>>> PENDING_ROLEPlAYS =
//            new SizeHashMap<>(500);


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
                Data data = server.getDatas(promptType).get(promptName);

                try {
                    data.nuke();
                } catch (IOException ignored) {

                }

                server.getDatas(promptType).remove(promptName);
            }

            createPromptViewer(event.getMessage(), promptType, null);
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
        return (event -> {
            event.deferReply().setEphemeral(true).queue();

            event.getMessage().delete().submit()
                    .thenCompose((v) -> event.getHook().editOriginal("Deleted!").submit());
         });
    }

    public static Consumer<ButtonInteractionEvent> getEditMessage(){
        return (event -> {
            DiscordRoleplay roleplay = AIBot.bot.getChat(event.getGuild());
            if(roleplay.isRunningRoleplay()){
                TextInput contentInput = TextInput.create("content", "Content", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("The content that will replace this message's original content")
                        .setValue(event.getMessage().getContentRaw())
                        .build();

                event.replyModal(
                        InteractionCreator.createPermanentModal(Modal.create("edit", "Edit Message")
                                .addComponents(ActionRow.of(contentInput)),modalEvent -> {
                            String content = modalEvent.getValue("content").getAsString();
                            modalEvent.editMessage(content).queue();
                            // replaces the content at that swipe
                            roleplay.getSwipes().add(roleplay.getCurrentSwipe(), content);
                            roleplay.getSwipes().remove(roleplay.getCurrentSwipe() + 1);
                        })
                ).queue();
            } else {
                event.reply("Roleplay isn't running currently!").setEphemeral(true).queue();
            }
        });
    }

    public static Consumer<ButtonInteractionEvent> getSwipe(Direction direction){
        return event -> {
            DiscordRoleplay roleplay = AIBot.bot.getChat(event.getGuild());
            if(roleplay.isRunningRoleplay()){
                event.deferEdit().queue();
                roleplay.swipe(event, direction);
            } else {
                event.reply("Roleplay isn't running currently!").setEphemeral(true).queue();
            }
        };
    }

    public static void replyCreatingPrompt(GenericComponentInteractionCreateEvent event, PromptType promptType){
        List<LayoutComponent> components = new ArrayList<>();
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

        List<LayoutComponent> components = new ArrayList<>();
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

            }catch(IOException ignored){

            }

            Interactions.createPromptViewer(event.getMessage(), promptType, 0);
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

    public static EmbedBuilder createConfigViewerEmbed(Message message, Direction direction){
        Server server = AIBot.bot.getServerData(message.getGuild());
        HashMap<String, ConfigEntry> configEntries = server.getConfig();
        List<String> display = configEntries.entrySet().stream().filter(entry ->
                !entry.getValue().hidden).map(Map.Entry::getKey).toList();

        EmbedBuilder builder = Util.createBotEmbed();

        builder.setTitle("Configuration");

        int show = 10;
        int maxSize = Math.max(1, (int) Math.ceil((double) display.size() / show));

        int index = 0;
        List<MessageEmbed> embeds = message.getEmbeds();
        if(!embeds.isEmpty()){
            String footerText = embeds.getFirst().getFooter().getText();
            int indexOfSlash = footerText.indexOf("/");

            if(indexOfSlash != -1)
                index = Integer.parseInt(footerText.substring(indexOfSlash - 1, indexOfSlash));
            // gets the number before the /
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
        builder.setDescription(description.toString());

        builder.setFooter("Displaying Configuration: "+(index+1)+"/"+maxSize);
        return builder;
    }

    public static void createConfigViewer(Message message, int forceIndex){
        List<MessageEmbed> embeds = message.getEmbeds();
        if(!embeds.isEmpty()){
            String footerText = embeds.getFirst().getFooter().getText();
            footerText = footerText.substring(0, footerText.indexOf("/") - 1)
                    + forceIndex + footerText.substring(footerText.indexOf("/"));

            EmbedBuilder builder = new EmbedBuilder();
            builder.copyFrom(embeds.getFirst());
            builder.setFooter(footerText);

            message.editMessageEmbeds(builder.build()).submit();
        }

        createConfigViewer((direction) -> createConfigViewerEmbed(message, null), message, null);
    }

    public static void createConfigViewer(Function<Direction, EmbedBuilder> buildEmbed, Message message, Direction direction){
        EmbedBuilder embed = buildEmbed.apply(direction);
        ArrayList<LayoutComponent> components = new ArrayList<>();

        List<SelectOption> options =
                getOptionsFromViewer(embed.getDescriptionBuilder().toString());
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
                    createConfigViewer(buildEmbed, message, Direction.BACK);

                }).withStyle(ButtonStyle.PRIMARY),
                InteractionCreator.createButton("-->", (event) -> {
                    event.deferEdit().queue();
                    createConfigViewer(buildEmbed, message, Direction.NEXT);

                }).withStyle(ButtonStyle.PRIMARY)
        ));

        MessageEditAction editAction = message
                .editMessage(MessageEditData.fromEmbeds(embed.build()))
                .setComponents(components)
                .setFiles(List.of());
        editAction.setContent(null);
        editAction.submit();
    }


    public static EmbedBuilder createPromptViewerEmbed(Message message,
                                                       String preDescription,
                                                       PromptType promptType,
                                                       Direction direction,
                                                       BiFunction<PromptType, String, String> displayItem
    ){
        Server server = AIBot.bot.getServerData(message.getGuild());
        List<String> display = server.getDatas(promptType).keySet().stream().toList();

        EmbedBuilder builder = Util.createBotEmbed();

        builder.setTitle("Available Prompts");

        int show = 10;
        int maxSize = Math.max(1, (int) Math.ceil((double) display.size() / show));

        int index = 0;
        List<MessageEmbed> embeds = message.getEmbeds();
        if(!embeds.isEmpty()){
            String footerText = embeds.getFirst().getFooter().getText();
            int indexOfSlash = footerText.indexOf("/");

            if(indexOfSlash != -1)
                index = Integer.parseInt(footerText.substring(indexOfSlash - 1, indexOfSlash));
            // gets the number before the /
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
        if(preDescription != null)
            description.insert(0, preDescription);
        display = display.subList(start, end);
        if(displayItem == null)
            displayItem = (type, string) -> string;
        for(int i = 0; i < display.size(); i++){
            String string = display.get(i);
            description.append(displayItem.apply(promptType, string));

            description.append("\n");
        }
        description.append("```");
        builder.setDescription(description.toString());

        builder.setFooter("Displaying "+promptType.displayName+": "+(index+1)+"/"+maxSize);
        return builder;
    }

    public static void createPromptViewer(Message message, PromptType promptType, int forceIndex){
        List<MessageEmbed> embeds = message.getEmbeds();
        if(!embeds.isEmpty()){
            String footerText = embeds.getFirst().getFooter().getText();
            footerText = footerText.substring(0, footerText.indexOf("/") - 1)
                    + forceIndex + footerText.substring(footerText.indexOf("/"));

            EmbedBuilder builder = new EmbedBuilder();
            builder.copyFrom(embeds.getFirst());
            builder.setFooter(footerText);

            message.editMessageEmbeds(builder.build()).submit();
        }

        createPromptViewer(message, promptType, null);
    }

    public static void createPromptViewer(Message message, PromptType promptType, Direction direction){
        createPromptViewer((direction1) -> createPromptViewerEmbed(message, null, promptType, direction1, null),
                message, promptType, true, true,
                null, direction);
    }

    public static void createPromptViewer(Function<Direction, EmbedBuilder> buildEmbed, Message message, PromptType promptType, boolean editable, boolean canGoBack,
                                          List<StringSelectMenu.Builder> onSelects, Direction direction, Button... buttons){
        EmbedBuilder embed = buildEmbed.apply(direction);
        ArrayList<LayoutComponent> components = new ArrayList<>();

        List<SelectOption> options =
                getOptionsFromViewer(embed.getDescriptionBuilder().toString());
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
                    StringSelectMenu.Builder copy = StringSelectMenu.create(builder.getId())
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
        components.add(ActionRow.of(
                InteractionCreator.createButton("<--", (event) -> {
                    event.deferEdit().queue();
                   createPromptViewer(buildEmbed, message, promptType, editable, canGoBack, onSelects, Direction.BACK, buttons);

                }).withStyle(ButtonStyle.PRIMARY),
                InteractionCreator.createButton("-->", (event) -> {
                    event.deferEdit().queue();
                    createPromptViewer(buildEmbed, message, promptType, editable, canGoBack, onSelects, Direction.NEXT, buttons);

                }).withStyle(ButtonStyle.PRIMARY)
        ));

        if(buttons.length > 0)
            components.add(ActionRow.of(buttons));

        if(canGoBack || editable){
            ArrayList<ItemComponent> itemComponents = new ArrayList<>();
            if(canGoBack)
                itemComponents.add(InteractionCreator.createButton("View Dashboard", (event) -> {
                    event.deferEdit().queue();
                    try {
                        createDashboard(event.getMessage());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).withEmoji(Emoji.fromFormatted("🔝")).withStyle(ButtonStyle.SECONDARY));
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
                .editMessage(MessageEditData.fromEmbeds(embed.build()))
                .setComponents(components)
                .setFiles(List.of());
        editAction.setContent(null);
        editAction.submit();
    }

    public static void createDashboard(Message message) throws IOException {
        DiscordRoleplay roleplay = AIBot.bot.getChat(message.getGuild());

        boolean isGif = Cats.isGif();
        Random random = new Random();
        byte[] data = random.nextBoolean() ? Cats.getCat() : Util.getRandomImage();
        if(data == null)
            data = Cats.getCat();
        String fileName = isGif ? "image.gif" : "image.png";

        EmbedBuilder builder = Util.createBotEmbed();

        builder.setTitle("AI Roleplay Dashboard");
        builder.setImage("attachment://"+fileName);

        builder.setFooter(Util.getRandomToolTip()); // random footer msgs
//            builder.setDescription(AIBot.bot.getFunnyMessage());

        HashMap<PromptType, String> displayMap = new HashMap<>();

        for(PromptType promptType : PromptType.values()){
            List<? extends Data> promptDatas = roleplay.getDatas(promptType);

            StringBuilder display = new StringBuilder();
            for(int i = 0; i < promptDatas.size(); i++){
                String name = promptDatas.get(i).getName();
                display.append(name);
                if(i != promptDatas.size() - 1){
                    display.append(", ");
                }
            }
            if(display.isEmpty())
                display.append("No ongoing roleplay!");
            displayMap.put(promptType, display.toString());
        }

        builder.addField("Instructions Involved: ", displayMap.get(PromptType.INSTRUCTION), true);
        builder.addField("World Lore Involved: ", displayMap.get(PromptType.WORLD), true);
        builder.addField("Character(s) Involved: ", displayMap.get(PromptType.CHARACTER), true);
        builder.addField("Chat History: ",  roleplay.getHistory().size() + " messages", false);
        builder.addField("AI Model: ", roleplay.getModel(), false);

        ArrayList<ItemComponent> roleplayComponents = new ArrayList<>();
        if(roleplay.isRunningRoleplay()){
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
                    .withEmoji(Emoji.fromFormatted("❎")));
            roleplayComponents.add(InteractionCreator.createButton("Restart Roleplay", (event) -> {
                if(roleplay.isMakingResponse()){
                    event.reply("Cannot restart the roleplay while a message is being generated!").setEphemeral(true).queue();
                    return;
                }
                event.reply(Util.botifyMessage("Restarted the chat! Characters are creating their initial prompts now.."))
                        .setEphemeral(true)
                        .queue(success -> {
                            try {
                                roleplay.restartRoleplay(event.getMessage());
                                roleplay.getCharacters().forEach((name, characterData) ->
                                {
                                    try {
                                        roleplay.promptCharacterToRoleplay(characterData, null, false, true);
                                    } catch (ExecutionException | InterruptedException ignored) {

                                    }
                                });

                            } catch (Exception e) {
                                event.getHook().editOriginal("Failed to start chat!").queue();
                            }
                        });
            }).withStyle(ButtonStyle.SECONDARY).withEmoji(Emoji.fromFormatted("⏪")));
        } else {
            roleplayComponents.add(InteractionCreator.createButton("New Roleplay", (event) -> {
                Server server = AIBot.bot.getServerData(event.getGuild());
                if(server.getInstructionDatas().entrySet().isEmpty() || server.getCharacterDatas().entrySet().isEmpty() || server.getWorldDatas().entrySet().isEmpty()){
                    event.reply("Must at least have one instruction, character and world created in the bot in order to start a roleplay!").setEphemeral(true).queue();
                    return;
                }

                String key = ((ConfigEntry.StringConfig) server.getConfig().get("open_router_key")).value;
                if(key == null || key.isEmpty()){
                    event.reply("No API key set! One must be set in the configuration first in order to roleplay!").queue();
                    return;
                }

                event.deferEdit().queue();

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

                        createPromptViewer((direction) -> createPromptViewerEmbed(
                                        event.getMessage(),
                                        "Select "+display+" prompts to use in the roleplay.",
                                        promptType,
                                        direction, enabledData), event.getMessage(), promptType, false, true,
                                selects, null, InteractionCreator.createButton(Emoji.fromFormatted("✅"), buttonEvent -> {
                                    if (datas.get(promptType).isEmpty()) {
                                        buttonEvent.reply("Need at least one set of "+display+"!").setEphemeral(true).queue();
                                        return;
                                    }
                                    if(nextInt + 1 >= PromptType.values().length){
                                        buttonEvent.getMessage().delete().queue();
                                        buttonEvent.reply((Util.botifyMessage("Started a new chat! Characters are creating their initial prompts now..")))
                                                .queue(success -> {
                                                    try {
                                                        roleplay.startRoleplay(success.retrieveOriginal().submit().get(),
                                                                datas.get(PromptType.INSTRUCTION).stream().map(string -> server.getInstructionDatas().get(string)).toList(),
                                                                datas.get(PromptType.WORLD).stream().map(string -> server.getWorldDatas().get(string)).toList(),
                                                                datas.get(PromptType.CHARACTER).stream().map(string -> server.getCharacterDatas().get(string)).toList()
                                                        );
                                                        roleplay.getCharacters().forEach((name, characterData) ->
                                                        {
                                                            try {
                                                                roleplay.promptCharacterToRoleplay(characterData, null, false, true);
                                                            } catch (ExecutionException | InterruptedException ignored) {

                                                            }
                                                        });

                                                    } catch (ExecutionException | InterruptedException | IOException e) {
                                                        buttonEvent.reply("Failed to start chat!").setEphemeral(true).queue();
                                                    }
                                                });
                                    } else {
                                        buttonEvent.deferEdit().queue();
                                        accept(nextInt + 1);
                                    }
                                }));
                    }
                };

                nextPromptType.accept(0);
            }).withStyle(ButtonStyle.SUCCESS).withEmoji(Emoji.fromFormatted("🪄")));
        }

        MessageEditAction editAction = message
                .editMessage(MessageEditData.fromEmbeds(builder.build()))
                .setComponents(
                        ActionRow.of(roleplayComponents),
                        ActionRow.of(
                                InteractionCreator.createButton("View Instructions", (event) -> {
                                    event.deferEdit().queue();
                                    createPromptViewer(message, PromptType.INSTRUCTION, 0);
                                }).withStyle(ButtonStyle.PRIMARY).withEmoji(Emoji.fromFormatted("📋")),
                                InteractionCreator.createButton("View Worlds", (event) -> {
                                    event.deferEdit().queue();
                                    createPromptViewer(message, PromptType.WORLD, 0);
                                }).withStyle(ButtonStyle.PRIMARY).withEmoji(Emoji.fromFormatted("🌍")),
                                InteractionCreator.createButton("View Characters", (event) -> {
                                    event.deferEdit().queue();
                                    createPromptViewer(message, PromptType.CHARACTER, 0);
                                }).withStyle(ButtonStyle.PRIMARY).withEmoji(Emoji.fromFormatted("🧝"))),
                        ActionRow.of(
                                InteractionCreator.createButton("Server Configuration", (event) -> {
                                    event.reply("Opening configuration menu").setEphemeral(true).queue(success ->
                                    {
                                        try {
                                            if(!Util.hasMasterPermission(event.getMember())){
                                                event.getHook().editOriginal("nuh uh little bro bro, you dont got permission").queue();
                                                return;
                                            }

                                            createConfigViewer(success.retrieveOriginal().submit().get(), 0);
                                        } catch (InterruptedException | ExecutionException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                }).withStyle(ButtonStyle.SECONDARY).withEmoji(Emoji.fromFormatted("⚙️")),
                                Button.link("https://openrouter.ai/models?order=pricing-low-to-high", "Free AI models")
                                        .withEmoji(Emoji.fromFormatted("🤖")))
                );
        if(data != null)
            editAction.setFiles(FileUpload.fromData(data, fileName));
        editAction.setContent(null);
        editAction.queue();
    }
}
