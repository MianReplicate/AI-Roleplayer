package discord.mian.interactions;

import discord.mian.ai.AIBot;
import discord.mian.ai.DiscordRoleplay;
import discord.mian.ai.data.CharacterData;
import discord.mian.ai.data.InstructionData;
import discord.mian.ai.data.Server;
import discord.mian.api.Data;
import discord.mian.custom.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Interactions {
//    private static final HashMap<Long, HashMap<String, List<? super Data>>> PENDING_ROLEPlAYS =
//            new SizeHashMap<>(500);


    public static Consumer<StringSelectInteractionEvent> getDeletionMenu(boolean isCharacter){
        return (event -> {
            event.deferEdit().queue();

            for(SelectOption option : event.getSelectedOptions()){
                String promptName = option.getValue();

                Server server = AIBot.bot.getServerData(event.getGuild());
                Data data = isCharacter ? server.getCharacterDatas().get(promptName)
                        : server.getInstructionDatas().get(promptName);

                try {
                    data.nuke();
                } catch (IOException ignored) {

                }

                if(isCharacter)
                    server.getCharacterDatas().remove(promptName);
                else
                    server.getInstructionDatas().remove(promptName);
            }

            createPromptViewer(event.getMessage(), isCharacter, null);
        });
    }

    public static Consumer<StringSelectInteractionEvent> getPromptEditMenu(boolean isCharacter, boolean isCreating){
        return (event -> {
            String promptName = event.getSelectedOptions().getFirst().getValue();

            try {
                if (isCreating) {
                    replyCreatingPrompt(event, isCharacter);
                } else {
                    replyEditingPrompt(event, isCharacter, promptName);
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
                                .addComponents(ActionRow.of(contentInput)).build(),modalEvent -> {
                            String content = modalEvent.getValue("content").getAsString();
                            modalEvent.editMessage(content).queue();
                        })
                ).queue();
            } else {
                event.reply("Roleplay isn't running currently!").setEphemeral(true).queue();
            }
        });
    }

    public static Consumer<ButtonInteractionEvent> getSwipe(Direction direction){
        return event -> {
            event.deferEdit().queue();

            DiscordRoleplay roleplay = AIBot.bot.getChat(event.getGuild());
            if(roleplay.isRunningRoleplay()){
                roleplay.swipe(event, direction);
            } else {
                event.getHook().editOriginal("Roleplay isn't running currently!").queue();
            }
        };
    }

    public static void replyCreatingPrompt(GenericComponentInteractionCreateEvent event, boolean isCharacter){
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

        if(isCharacter){
            components.add(ActionRow.of(
                    TextInput.create("talkability", "Talkability", TextInputStyle.SHORT)
                            .setPlaceholder("The chances of this character responding when their name is mentioned OR when auto-mode is on\n\nPut one decimal value from 0 to 1 in here. Default: 0.5")
                            .build()
            ));
        }

        event.replyModal(
                InteractionCreator.createModal("Creating Prompt", (modalEvent) -> {
                    String promptName = modalEvent.getValue("name").getAsString();
                    getPromptEditor(isCharacter, promptName).accept(modalEvent);
                }).addComponents(components).build()
        ).queue();
    }

    public static void replyEditingPrompt(GenericComponentInteractionCreateEvent event, boolean isCharacter, String promptName) throws IOException {
        TextInput.Builder promptInput = TextInput.create("prompt", "Prompt: {{char}} represents the character", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Edit the prompt. {{char}} represents the character");

        Server server = AIBot.bot.getServerData(event.getGuild());
        Data data = isCharacter ? server.getCharacterDatas().get(promptName)
                : server.getInstructionDatas().get(promptName);

        if(data == null)
            throw new RuntimeException(promptName + " is not a valid prompt!");

        promptInput.setValue(data.getPrompt());

        List<LayoutComponent> components = new ArrayList<>();
        components.add(ActionRow.of(promptInput.build()));
        if(isCharacter){
            components.add(ActionRow.of(
                    TextInput.create("talkability", "Talkability: Put a decimal from 0.0 to 1.0", TextInputStyle.SHORT)
                            .setPlaceholder("Likelihood of responding when mentioned in chat")
                            .setValue(String.valueOf(((CharacterData) data).getTalkability()))
                            .build()
            ));
        }

        event.replyModal(
                InteractionCreator.createModal("Editing " + promptName, getPromptEditor(isCharacter, promptName)).addComponents(components)
                        .build()
        ).queue();
    }

    private static Consumer<ModalInteractionEvent> getPromptEditor(boolean isCharacter, String promptName){
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

            Data data = isCharacter ? server.getCharacterDatas().get(name)
                    : server.getInstructionDatas().get(name);

            try{
                if(data != null){
                    data.addOrReplacePrompt(prompt);
                    if(isCharacter)
                        ((CharacterData) data).setTalkability(talkability);
                } else {
                    if(isCharacter)
                        server.createCharacter(name, prompt, talkability);
                    else
                        server.createInstruction(name, prompt);
                }

            }catch(IOException ignored){

            }

            Interactions.createPromptViewer(event.getMessage(), isCharacter, 0);
        });
    }

    public static List<SelectOption> getOptionsFromViewer(String description, Server server, boolean isCharacter){
        if(!description.equals("```\n```")){
            return List.of(description.split("\n"))
                    .stream().filter(string -> {
                        if(isCharacter)
                            return server.getCharacterDatas().containsKey(string);
                        else
                            return server.getInstructionDatas().containsKey(string);
                    }).map(string -> SelectOption.of(string, string)).toList();
        }
        return null;
    }

    public static EmbedBuilder createPromptViewerEmbed(Message message, String preDescription, boolean isCharacter, Direction direction){
        Server server = AIBot.bot.getServerData(message.getGuild());
        List<String> display = isCharacter ? server.getCharacterDatas().keySet().stream().toList() :
                server.getInstructionDatas().keySet().stream().toList();
        String promptType = isCharacter ? "Characters" : "Instructions";

        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle("Available Prompts");
        builder.setAuthor("Created By Your Lovely Girl: @MianReplicate", "https://en.pronouns.page/@MianReplicate");

        builder.setColor(new Color(
                (int) (Math.random() * 256),
                (int) (Math.random() * 256),
                (int) (Math.random() * 256),
                (int) (Math.random() * 256))
        );

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
        for(int i = 0; i < display.size(); i++){
            String string = display.get(i);
            description.append(string);

            description.append("\n");
        }
        description.append("```");
        builder.setDescription(description.toString());

        builder.setFooter("Displaying "+promptType+": "+(index+1)+"/"+maxSize);
        return builder;
    }

    public static void createPromptViewer(Message message, boolean isCharacter, int forceIndex){
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

        createPromptViewer(message, isCharacter, null);
    }

    public static void createPromptViewer(Message message, boolean isCharacter, Direction direction){
        createPromptViewer((direction1) -> createPromptViewerEmbed(message, null, isCharacter, direction1),
                message, isCharacter, true, true,
                null, direction);
    }

    public static void createPromptViewer(Function<Direction, EmbedBuilder> buildEmbed, Message message, boolean isCharacter, boolean editable, boolean canGoBack,
                                          Consumer<StringSelectInteractionEvent> onSelect, Direction direction, Button... buttons){
        EmbedBuilder embed = buildEmbed.apply(direction);
        ArrayList<LayoutComponent> components = new ArrayList<>();

        List<SelectOption> options =
                getOptionsFromViewer(embed.getDescriptionBuilder().toString(), AIBot.bot.getServerData(message.getGuild()), isCharacter);
        if(options != null){
            if(editable){
                components.add(
                        ActionRow.of(InteractionCreator.createStringMenu(Interactions.getPromptEditMenu(isCharacter, false))
                                .setRequiredRange(1, 1)
                                .setPlaceholder("Edit Prompt")
                                .addOptions(options)
                                .build())
                );
                components.add(
                        ActionRow.of(InteractionCreator.createStringMenu(Interactions.getDeletionMenu(isCharacter))
                                .setMaxValues(25)
                                .setPlaceholder("Delete Prompts")
                                .addOptions(options)
                                .build())
                );
            }
            if(onSelect != null){
                components.add(ActionRow.of(InteractionCreator.createStringMenu(onSelect)
                        .setMaxValues(25)
                        .setPlaceholder("Select Prompts")
                        .addOptions(options)
                        .build()));
            }
        }

        components.add(ActionRow.of(
                InteractionCreator.createButton("<--", (event) -> {
                    event.deferEdit().queue();
                   createPromptViewer(buildEmbed, message, isCharacter, editable, canGoBack, onSelect, Direction.BACK, buttons);

                }).withStyle(ButtonStyle.PRIMARY),
                InteractionCreator.createButton("-->", (event) -> {
                    event.deferEdit().queue();
                    createPromptViewer(buildEmbed, message, isCharacter, editable, canGoBack, onSelect, Direction.NEXT, buttons);

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
                    replyCreatingPrompt(event, isCharacter);
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
        EmbedBuilder builder = new EmbedBuilder();
        DiscordRoleplay chat = AIBot.bot.getChat(message.getGuild());

        boolean isGif = Cats.isGif();
        Random random = new Random();
        byte[] data = random.nextBoolean() ? Cats.getCat() : Util.getRandomImage();
        if(data == null)
            data = Cats.getCat();
        String fileName = isGif ? "image.gif" : "image.png";

        builder.setTitle("AI Roleplay Dashboard");
        builder.setAuthor("Created By Your Lovely Girl: @MianReplicate", "https://en.pronouns.page/@MianReplicate");
        builder.setImage("attachment://"+fileName);

        builder.setFooter("this is a very queer thing :o"); // random footer msgs
//            builder.setDescription(AIBot.bot.getFunnyMessage());
        builder.setColor(new Color(
                (int) (Math.random() * 256),
                (int) (Math.random() * 256),
                (int) (Math.random() * 256),
                (int) (Math.random() * 256))
        );
        StringBuilder display = new StringBuilder();
        if(chat.getCharacters() != null){
            java.util.List<String> keys = chat.getCharacters().keySet().stream().toList();
            for(int i = 0; i < keys.size(); i++){
                String name = keys.get(i);
                display.append(name);
                if(i != keys.size() - 1){
                    display.append(", ");
                }
            }
        } else {
            display = new StringBuilder("No ongoing roleplay!");
        }

        builder.addField("Character(s) Involved: ", display.toString(), true);
        builder.addField("Chat History: ",  chat.getHistory().size() + " messages", true);
        builder.addField("AI Model: ", chat.getModel(), true);

        MessageEditAction editAction = message
                .editMessage(MessageEditData.fromEmbeds(builder.build()))
                .setActionRow(
                        InteractionCreator.createButton("New Roleplay", (event) -> {
                            Server server = AIBot.bot.getServerData(event.getGuild());
                            if(server.getInstructionDatas().entrySet().isEmpty() || server.getCharacterDatas().entrySet().isEmpty()){
                                event.reply("Must at least have one instruction and one character created in the bot in order to start a roleplay!").queue();
                                return;
                            }

                            event.deferEdit().queue();

                            HashMap<String, ArrayList<String>> datas = new HashMap<>();
                            datas.put("instructions", new ArrayList<>());
                            datas.put("characters", new ArrayList<>());
//                            PENDING_ROLEPlAYS.put(event.getMessageIdLong(), datas);

                            createPromptViewer((direction) -> createPromptViewerEmbed(
                                            event.getMessage(),
                                            "Select instruction prompts to use in the roleplay.",
                                            false,
                                            direction), event.getMessage(), false, false, true,
                                    (onSelectInstructions) -> {
                                        onSelectInstructions.deferReply(true).queue();
                                        onSelectInstructions.getSelectedOptions().forEach(selectOption -> {
                                            ArrayList<String> instructions = datas.get("instructions");
                                            if(!instructions.contains(selectOption.getValue()))
                                                instructions.add(selectOption.getValue());
                                        });
                                        onSelectInstructions.getHook().editOriginal("Added selected instructions!").queue();
                                    }, null, InteractionCreator.createButton(Emoji.fromFormatted("✅"), buttonEvent -> {
                                        if(datas.get("instructions").isEmpty()){
                                            buttonEvent.reply("Need at least one set of instructions!").setEphemeral(true).queue();
                                            return;
                                        }

                                        buttonEvent.deferEdit().queue();
                                        createPromptViewer((direction) -> createPromptViewerEmbed(
                                                        buttonEvent.getMessage(),
                                                        "Select character prompts to use in the roleplay.",
                                                        true,
                                                        direction), buttonEvent.getMessage(), true, false, true,
                                                (onSelectInstructions) -> {

                                                    onSelectInstructions.deferReply(true).queue();
                                                    onSelectInstructions.getSelectedOptions().forEach(selectOption -> {
                                                        ArrayList<String> instructions = datas.get("characters");
                                                        if(!instructions.contains(selectOption.getValue()))
                                                            instructions.add(selectOption.getValue());
                                                    });
                                                    onSelectInstructions.getHook().editOriginal("Added selected characters!").queue();

                                                }, null, InteractionCreator.createButton(Emoji.fromFormatted("✅"), buttonEvent2 -> {
                                                    if(datas.get("characters").isEmpty()){
                                                        buttonEvent.reply("Need at least one character!").setEphemeral(true).queue();
                                                        return;
                                                    }

                                                    buttonEvent2.deferEdit().queue();
                                                    buttonEvent2.getHook().editOriginal(Util.botifyMessage("Started the new chat! Characters are creating their initial prompts now.."))
                                                            .setEmbeds(new MessageEmbed[0])
                                                            .setComponents()
                                                            .queue(success -> {
                                                        try {
                                                            DiscordRoleplay roleplay = AIBot.bot.getChat(buttonEvent2.getGuild());

                                                            roleplay.startRoleplay(success,
                                                                    datas.get("instructions").stream().map(string -> server.getInstructionDatas().get(string)).toList(),
                                                                    datas.get("characters").stream().map(string -> server.getCharacterDatas().get(string)).toList()
                                                                    );
                                                            roleplay.getCharacters().forEach((name, characterData) ->
                                                            {
                                                                try {
                                                                    roleplay.promptCharacterToRoleplay(characterData, null, false, true);
                                                                } catch (ExecutionException | InterruptedException ignored) {

                                                                }
                                                            });

                                                        } catch (ExecutionException | InterruptedException | IOException e) {
                                                            buttonEvent2.getHook().editOriginal("Failed to start chat!").queue();
                                                        }
                                                    });
                                                }));

                            }));
                        }).withStyle(ButtonStyle.SUCCESS).withEmoji(Emoji.fromFormatted("🪄")),

                        InteractionCreator.createButton("View Characters", (event) -> {
                            event.deferEdit().queue();
                            createPromptViewer(message, true, 0);
                        }).withStyle(ButtonStyle.PRIMARY).withEmoji(Emoji.fromFormatted("🧝")),
                        InteractionCreator.createButton("View Introductions", (event) -> {
                            event.deferEdit().queue();
                            createPromptViewer(message, false, 0);
                        }).withStyle(ButtonStyle.PRIMARY).withEmoji(Emoji.fromFormatted("📋")),

                        Button.link("https://openrouter.ai/models?order=pricing-low-to-high", "Free AI models")
                );
        if(data != null)
            editAction.setFiles(FileUpload.fromData(data, fileName));
        editAction.setContent(null);
        editAction.queue();
    }
}
