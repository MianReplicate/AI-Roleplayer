package discord.mian.ai;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import discord.mian.ai.data.CharacterData;
import discord.mian.ai.data.InstructionData;
import discord.mian.custom.Direction;
import discord.mian.custom.Util;
import discord.mian.custom.Constants;
import discord.mian.interactions.InteractionCreator;
import discord.mian.interactions.Interactions;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.TimeUtil;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DiscordRoleplay {
    // limit roleplay to one channel lmfao

    private SimpleOpenAI llm;
    private boolean makingResponse;
    private int maxTokens;
    private String model;
    private final Guild guild;
    private TextChannel channel;
    private Webhook webhook;

    // start of history
    private OffsetDateTime historyStart;

    private Message latestAssistantMessage;
    private Message errorMsgCleanup;
    private int currentSwipe;
    private ArrayList<String> swipes;

    private final EncodingRegistry registry;

    // make possible to swipe messages
    private ArrayList<InstructionData> instructions;
    private HashMap<String, CharacterData> characters;
    private CharacterData currentCharacter;
    private boolean runningRoleplay = false;

    public DiscordRoleplay(Guild guild){
        if(Constants.LLM_KEY == null || Constants.LLM_KEY.isEmpty())
            throw new RuntimeException("No OpenRouter key set!");

        this.llm = SimpleOpenAI.builder()
                .baseUrl(Constants.BASE_URL)
                .apiKey(Constants.LLM_KEY)
                .build();

        this.maxTokens = 8192;
        this.model = Constants.DEFAULT_MODEL;
        this.registry = Encodings.newDefaultEncodingRegistry();
        this.guild = guild;
    }

    public List<ChatMessage> trimListToMeetTokens(ArrayList<ChatMessage> msgs, int startAt){
        Encoding enc = registry.getEncodingForModel(this.model.substring(this.model.lastIndexOf("/")))
                .orElse(registry.getEncoding(EncodingType.CL100K_BASE));

        StringBuilder combinedText = new StringBuilder();
        for(ChatMessage msg : msgs){
            if(msg instanceof ChatMessage.UserMessage message){
                combinedText.append(message.getContent());
            } else if(msg instanceof ChatMessage.AssistantMessage message){
                combinedText.append(message.getContent());
            } else if(msg instanceof ChatMessage.SystemMessage message){
                combinedText.append(message.getContent());
            }
        };

        int tokens = enc.countTokens(combinedText.toString());
        if(tokens >= maxTokens){
            int difference = tokens - maxTokens;

            int toRemove = 0;
            int current = 0;
            for(int i = startAt; i < msgs.size() && current < difference; i++){
                ChatMessage msg = msgs.get(i);
                String content = null;
                if(msg instanceof ChatMessage.UserMessage message){
                    content = message.getContent().toString();
                } else if(msg instanceof ChatMessage.AssistantMessage message){
                    content = message.getContent().toString();
                } else if(msg instanceof ChatMessage.SystemMessage message){
                    content = message.getContent().toString();
                }
                current += enc.countTokens(content);
                toRemove++;
            }
            List<ChatMessage> newList = msgs.subList(0, startAt);
            newList.addAll(msgs.subList(toRemove + startAt, msgs.size()));
            return newList;
        }

        return msgs;
    }

    public void creatingResponseFromDiscordMessage(){
        if(makingResponse)
            throw new RuntimeException("Already generating a response!");
        this.makingResponse = true;
        if(latestAssistantMessage != null){
            latestAssistantMessage.editMessage(latestAssistantMessage.getContentRaw())
                    .setComponents(List.of()).submit();
        }
    }

    public void finishedDiscordResponse(String finalResponse){
        this.makingResponse = false;
        if(latestAssistantMessage != null && finalResponse != null){
            ArrayList<ItemComponent> components = new ArrayList<>();
            components.add(InteractionCreator.createButton("<--", Interactions.getSwipe(Direction.BACK))
                    .withStyle(ButtonStyle.PRIMARY));
            components.add(InteractionCreator.createButton("-->", Interactions.getSwipe(Direction.NEXT))
                    .withStyle(ButtonStyle.PRIMARY));

            if(errorMsgCleanup == null){
                components.add(InteractionCreator.createPermanentButton(Button.danger("danger", Emoji.fromFormatted("🗑")),
                        Interactions.getDestroyMessage()));
                components.add(InteractionCreator.createPermanentButton(Button.success("edit", Emoji.fromFormatted("🪄")),
                        Interactions.getEditMessage()));
            }

            latestAssistantMessage.editMessage(finalResponse)
                    .setActionRow(components).queue();
        }
    }

    public ChatRequest createRequest(CharacterData character){
        return ChatRequest.builder()
                .maxCompletionTokens(this.maxTokens)
                .model(this.model)
                .messages(getHistory(character))
                .temperature(1.0)
                .build();
    }

    public String createCustomResponse(String content){
        ArrayList<ChatMessage> history = new ArrayList<>();
        history.add(ChatMessage.UserMessage.of(content, "Admin"));

        ChatRequest chatRequest = ChatRequest.builder()
                .maxCompletionTokens(this.maxTokens)
                .model(this.model)
                .messages(history)
                .build();

        CompletableFuture<Chat> futureChat = this.llm.chatCompletions()
                .create(chatRequest);
        Chat chat = futureChat.join();

        return chat.firstContent();
    }

    private String generateResponse(CharacterData character, Consumer<String> consumer){
        ChatRequest chatRequest = createRequest(character);
        CompletableFuture<Stream<Chat>> futureChat = this.llm.chatCompletions()
                .createStream(chatRequest);
        Stream<Chat> chat = futureChat.join();
        var fullResponseWrapper = new Object(){String fullResponse = "";};

        chat.filter(chatResp -> chatResp.getChoices() != null && !chatResp.getChoices().isEmpty() && chatResp.firstContent() != null)
                .map(Chat::firstContent)
                .forEach(partialResponse -> {
                    fullResponseWrapper.fullResponse += partialResponse;
                    consumer.accept(fullResponseWrapper.fullResponse);
                });
        return fullResponseWrapper.fullResponse;
    }

    private String streamOnDiscordMessage(CharacterData character, Message msgToEdit){
        AtomicBoolean queued = new AtomicBoolean(false);
        AtomicLong timeResponseMade = new AtomicLong(System.currentTimeMillis());
        double timeBetween = 1;

        String botUser = character.getName() + ":";
        String finalResponse = generateResponse(character,response -> {
            if (!queued.get() && System.currentTimeMillis() - timeResponseMade.get() >= timeBetween && !response.isBlank()) {
                queued.set(true);
                Consumer<Message> onComplete = newMsg -> {
                    queued.set(false);
                    timeResponseMade.set(System.currentTimeMillis());
                };
                String newContent = response.replaceAll(botUser, "");
                msgToEdit.editMessage(MessageEditData.fromContent(Util.botifyMessage("Message is currently generating..") + "\n" + newContent)).queue(onComplete);
            }
        });
        String newContent = finalResponse.replaceAll(botUser, "");
        if(newContent.isEmpty())
            throw new RuntimeException("Empty message! :(");
        return newContent;
    }

    public void sendRoleplayMessage(Message userMsg) {
        if(!runningRoleplay){
            userMsg.getChannel().sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since there is no ongoing chat!")
            )).queue();
            return;
        }
        if (isMakingResponse()) {
            userMsg.getChannel().sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since I am already generating one!")
            )).queue();
            return;
        }
        if(currentCharacter == null){
            userMsg.getChannel().sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since there are no characters in this chat!")
            )).queue();
            return;
        }

        if(this.errorMsgCleanup != null){
            this.errorMsgCleanup.delete().queue(RestAction.getDefaultSuccess(), toThrow -> {});
            this.errorMsgCleanup = null;
        }

        if(this.latestAssistantMessage != null){
            if(latestAssistantMessage.getContentRaw().isEmpty())
                latestAssistantMessage.delete().queue();
            else
                latestAssistantMessage.editMessageComponents(
                        ActionRow.of(Button.danger("destroy", Emoji.fromFormatted("🗑")),
                                Button.success("edit", Emoji.fromFormatted("🪄")))).queue();
            latestAssistantMessage = null;
            swipes = null;
            currentSwipe = 0;
        }
        this.creatingResponseFromDiscordMessage();

        String avatarLink = null;
        try{
            avatarLink = currentCharacter.getAvatarLink();
        } catch (Exception ignored) {

        }

        WebhookMessageCreateAction<Message> messageCreateData = webhook.sendMessage(
                Util.botifyMessage("Currently creating a response! Check back in a second.."))
                .setUsername(currentCharacter.getName());

        if(avatarLink != null){
            messageCreateData = messageCreateData.setAvatarUrl(avatarLink);
        }

        messageCreateData.queue(aiMsg -> {
            latestAssistantMessage = aiMsg;
            swipes = new ArrayList<>();
            try {
                String finalResponse = streamOnDiscordMessage(currentCharacter, aiMsg);
//                latestAssistantMessage = aiMsg;
                swipes.add(finalResponse);
                errorMsgCleanup = null;
                this.finishedDiscordResponse(finalResponse);
            } catch (Exception e) {
                this.finishedDiscordResponse(Util.botifyMessage("Failed to send a response due to an exception :< sowwy. Try using a different AI model.\nError: " + e.toString().substring(0, Math.min(e.toString().length(), 1925))));
                errorMsgCleanup = aiMsg;
                currentSwipe = 0;
                throw(e);
            }
        });
    }

    public void swipe(ButtonInteractionEvent event, Direction direction){
        if(latestAssistantMessage != null){
            if(event.getMessage().getIdLong() != latestAssistantMessage.getIdLong()){
                event.getHook().editOriginal("You cannot swipe on this message anymore :(, consider editing it instead!")
                        .queue();
                return;
            }

            String finalResponse = null;
            if(direction == Direction.BACK){
                currentSwipe--;
                if(currentSwipe == -1)
                    currentSwipe = Math.min(0, swipes.size() - 1);
            } else {
                if(currentSwipe + 1 >= swipes.size()){
                    if (isMakingResponse()) {
                        event.getHook().editOriginal("Cannot make a response since I am already generating one!")
                                .queue();
                        return;
                    }
                    CharacterData character = characters.get(latestAssistantMessage.getAuthor().getName());
                    this.creatingResponseFromDiscordMessage();

                    try{
                        finalResponse = streamOnDiscordMessage(character, latestAssistantMessage);
                        currentSwipe++;
                        swipes.add(finalResponse);
                        errorMsgCleanup = null;
                    }catch(Exception e){
                        finalResponse = "```Failed to make a new response!\nError: " + e + "```";
                    }
                    this.finishedDiscordResponse(finalResponse);
                } else {
                    currentSwipe++;
                    if(currentSwipe >= swipes.size())
                        currentSwipe = 0;
                }
            }
            latestAssistantMessage.editMessage(finalResponse != null ? MessageEditData.fromContent(finalResponse) :
                    MessageEditData.fromContent(swipes.get(currentSwipe))).queue();
        }
    }

    public boolean isMakingResponse() {
        return this.makingResponse;
    }

    public boolean isRunningRoleplay(){
        return runningRoleplay;
    }

    public List<ChatMessage> getHistory(){
        return getHistory(null);
    }

    // beginning prompts not included if no character is provided
    public List<ChatMessage> getHistory(CharacterData character){
        if(historyStart == null)
            return new ArrayList<>();

        ArrayList<ChatMessage> messages = new ArrayList<>();

        // for next msg in roleplay?
        if(character != null){
            messages.add(ChatMessage.SystemMessage.of("<INSTRUCTIONS>\nFollow the instructions below! You are the Game Master and will follow these rules!", "INSTRUCTIONS"));
            for(InstructionData instructionData : instructions){
                messages.add(instructionData.getChatMessage(character));
            }

            StringBuilder multipleCharacters = new StringBuilder("You may play multiple characters in this roleplay as the assistant. Determine which character you'll be playing based on who said a response. Not every response from you is made by the same person. You are {{char}}. Do not play other characters. The group of characters involved in this roleplay are ");
            for(String name : characters.keySet()){
                multipleCharacters.append(name).append(", ");
            };
            multipleCharacters.replace(multipleCharacters.lastIndexOf(", "), multipleCharacters.length(), ".");

            messages.add(
                    ChatMessage.SystemMessage.of(multipleCharacters.toString())
            );
            messages.add(
                    ChatMessage.SystemMessage.of("Do not include the character name in your response, this is already provided programmatically by the code.")
            );
            messages.add(ChatMessage.SystemMessage.of("<CHARACTER>\nUnderstand the character definition below! This is the character you will be playing in the roleplay.", "CHARACTER"));
            messages.add(character.getChatMessage(character));
            messages.add(ChatMessage.SystemMessage.of("<CHAT HISTORY> This is the start of the roleplay."));
        }
        int required = messages.size();

        long discordTime = TimeUtil.getDiscordTimestamp(Instant.ofEpochSecond(historyStart.toInstant().getEpochSecond()).toEpochMilli());

        ArrayList<Message> listOfMessages = new ArrayList<>();
        try {
            listOfMessages = new ArrayList<>(
                    channel.getIterableHistory().deadline(discordTime).submit().get()
            );
        } catch (InterruptedException | ExecutionException ignored) {
        }

        for(int i = listOfMessages.size() - 1; i >= 0; i--) {
            Message message = listOfMessages.get(i);
            if(message.getTimeCreated().isAfter(historyStart)){
                if(message.getAuthor() == AIBot.bot.getJDA().getSelfUser())
                    continue;
                if(message.getContentRaw().contains("Currently creating a response"))
                    continue;
                if(latestAssistantMessage != null &&
                        (latestAssistantMessage.getIdLong() == message.getIdLong() ||
                                latestAssistantMessage.getTimeCreated().isBefore(message.getTimeCreated())))
                    continue;

                String contents = message.getContentRaw();
                String username = message.getAuthor().getGlobalName();
                if (username == null)
                    username = message.getAuthor().getName();

                String formatted = contents.replaceAll("<@" + message.getAuthor().getId() + ">", "");

                if (message.isWebhookMessage()) {
                    messages.add(
                            ChatMessage.AssistantMessage.builder()
                                    .content(username + ": " + formatted)
                                    .name(username)
                                    .build()
                    );
                } else {
                    messages.add(ChatMessage.UserMessage.of(username + ": " + formatted, username));
                }
            }
        }

        // ngl we might need a max message count just in case it gets too bigggggg
        Constants.LOGGER.info("Recieved messages! Count: "+messages.size());
        Constants.LOGGER.info(String.valueOf(messages));

        return trimListToMeetTokens(messages, required);
    }

    public void startRoleplay(Message introMessage, List<InstructionData> instructionList, List<CharacterData> characterList) throws ExecutionException, InterruptedException, IOException {
        if(instructionList.size() <= 0)
            throw new RuntimeException("Need at least one set of instructions!");

        historyStart = introMessage.getTimeCreated();
        this.channel = introMessage.getChannel().asTextChannel();

        Webhook webhook;
        webhook = channel.retrieveWebhooks().submit().get().stream().filter(find -> find.getName().equals(AIBot.bot.getJDA().getSelfUser().getName()))
                .findFirst().orElse(null);
        if(webhook == null){
            InputStream inputStream = AIBot.bot.getJDA().getSelfUser().getAvatar().download().get();
            webhook = channel.createWebhook(AIBot.bot.getJDA().getSelfUser().getName())
                    .setAvatar(Icon.from(inputStream)).submit().get();
        }
        this.webhook = webhook;

        runningRoleplay = true;

        latestAssistantMessage = null;
        swipes = null;
        currentSwipe = 0;

        characters = new HashMap<>();
        characterList.forEach(this::addCharacter);
        this.instructions = new ArrayList<>();
        instructionList.forEach(this::addInstructions);
        Stream<String> stream = characters.keySet().stream();
        stream.findAny().ifPresent(this::setCurrentCharacter);
    }

    public void setMaxTokens(int maxTokens){
        this.maxTokens = maxTokens;
    }

    public void setModel(String model){
        this.model = model;
    }

    public String getModel(){
        return model;
    }

    public HashMap<String, CharacterData> getCharacters() {
        return characters;
    }

    public CharacterData getCurrentCharacter(){
        return currentCharacter;
    }

    public TextChannel getChannel(){
        return channel;
    }

    public Message getLatestAssistantMessage(){
        return latestAssistantMessage;
    }

    public void setCurrentCharacter(String name){
        currentCharacter = characters.get(name);
    }

    public void addCharacter(CharacterData character){
        characters.putIfAbsent(character.getName(), character);
        if(currentCharacter == null)
            currentCharacter = character;
    }

    public void addInstructions(InstructionData instructionData){
        this.instructions.add(instructionData);
    }
}
