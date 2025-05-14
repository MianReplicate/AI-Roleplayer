package discord.mian.interactions;

import discord.mian.ai.AIBot;
import discord.mian.ai.DiscordRoleplay;
import discord.mian.ai.data.Server;
import discord.mian.api.Data;
import discord.mian.custom.Cats;
import discord.mian.custom.Constants;
import discord.mian.custom.Direction;
import discord.mian.custom.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
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
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class Interactions {
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
        event.replyModal(
                InteractionCreator.createModal("Creating Prompt", (modalEvent) -> {
                    String promptName = modalEvent.getValue("name").getAsString();
                    getPromptEditor(isCharacter, promptName).accept(modalEvent);
                }).addComponents(
                        ActionRow.of(
                                TextInput.create("name", "Name", TextInputStyle.SHORT)
                                        .setPlaceholder("Enter a name for the new prompt!")
                                        .build()
                        ),
                        ActionRow.of(
                                        TextInput.create("prompt", "Prompt: {{char}} represents the character", TextInputStyle.PARAGRAPH)
                                                .setPlaceholder("Edit the prompt. {{char}} represents the character")
                                                .build()
                                )).build()
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

        event.replyModal(
                InteractionCreator.createModal("Editing " + promptName, getPromptEditor(isCharacter, promptName)).addComponents(
                        ActionRow.of(promptInput.build())).build()
        ).queue();
    }

    private static Consumer<ModalInteractionEvent> getPromptEditor(boolean isCharacter, String promptName){
        return (event -> {
            event.deferEdit().queue();

            String name = promptName != null ? promptName : event.getValue("name").getAsString();
            String prompt = event.getValue("prompt").getAsString();
            Server server = AIBot.bot.getServerData(event.getGuild());

            Data data = isCharacter ? server.getCharacterDatas().get(name)
                    : server.getInstructionDatas().get(name);

            try{
                if(data != null){
                    data.addOrReplacePrompt(prompt);
                } else {
                    if(isCharacter)
                        server.createCharacter(name, prompt);
                    else
                        server.createInstruction(name, prompt);
                }

            }catch(IOException ignored){

            }

            Interactions.createPromptViewer(event.getMessage(), isCharacter, 0);
        });
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
            index = Integer.parseInt(footerText.substring(footerText.indexOf("/") - 1, footerText.indexOf("/")));
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
        ArrayList<SelectOption> options = new ArrayList<>();
        for(int i = 0; i < display.size(); i++){
            String string = display.get(i);
            description.append(string);

            options.add(SelectOption.of(string, string));

            if(i + 1 != display.size())
                description.append("\n");
        }
        description.append("```");
        builder.setDescription(description.toString());

        builder.setFooter("Displaying "+promptType+": "+(index+1)+"/"+maxSize);

        ArrayList<LayoutComponent> components = new ArrayList<>();

        if(!options.isEmpty()){
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
        components.add(ActionRow.of(
                InteractionCreator.createButton("<--", (event) -> {
                    event.deferEdit().queue();
                   createPromptViewer(message, isCharacter, Direction.BACK);
                }).withStyle(ButtonStyle.PRIMARY),
                InteractionCreator.createButton("-->", (event) -> {
                    event.deferEdit().queue();
                    createPromptViewer(message, isCharacter, Direction.NEXT);
                }).withStyle(ButtonStyle.PRIMARY)
        ));


        components.add(ActionRow.of(
                InteractionCreator.createButton("View Dashboard", (event) -> {
                    event.deferEdit().queue();
                    try {
                        createDashboard(event.getMessage());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).withEmoji(Emoji.fromFormatted("🔝")).withStyle(ButtonStyle.SECONDARY),

                InteractionCreator.createButton("Create Prompt", (event) -> {
                    replyCreatingPrompt(event, isCharacter);
                        }).withEmoji(Emoji.fromFormatted("🪄"))));

        MessageEditAction editAction = message
                .editMessage(MessageEditData.fromEmbeds(builder.build()))
                .setComponents(components)
                .setFiles(List.of());
        editAction.queue();
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
                            event.reply("Started new chat! From here on out, characters will be listening :o").queue(success -> {
                                try {
                                    AIBot.bot.getChat(event.getGuild())
                                            .startRoleplay(success.retrieveOriginal().submit().get(),
                                                    java.util.List.of(AIBot.bot.getServerData(event.getGuild()).getInstructionDatas().get("non-nsfw")), List.of());
                                } catch (ExecutionException | InterruptedException | IOException e) {
                                    event.getHook().editOriginal("Failed to start chat!").queue();
                                }
                            });
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
        editAction.queue();
    }
}
