package discord.mian.ai;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import discord.mian.api.Data;
import discord.mian.custom.*;
import discord.mian.data.CharacterData;
import discord.mian.data.InstructionData;
import discord.mian.data.Server;
import discord.mian.data.WorldData;
import discord.mian.interactions.InteractionCreator;
import discord.mian.interactions.Interactions;
import io.github.sashirestela.cleverclient.support.CleverClientException;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import io.github.sashirestela.openai.domain.completion.Completion;
import io.github.sashirestela.openai.domain.completion.CompletionRequest;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DiscordRoleplay {
    // limit roleplay to one channel lmfao

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
    private ArrayList<WorldData> worldLore;
    private HashMap<String, CharacterData> characters;
    private CharacterData currentCharacter;
    private boolean runningRoleplay = false;

    public DiscordRoleplay(Guild guild){
//        this.llm = SimpleOpenAI.builder()
//                .baseUrl(Constants.BASE_URL)
//                .apiKey(Constants.LLM_KEY)
//                .build();

        this.maxTokens = 8192;
        this.model = Constants.DEFAULT_MODEL;
        this.registry = Encodings.newDefaultEncodingRegistry();
        this.guild = guild;

        instructions = new ArrayList<>();
        worldLore = new ArrayList<>();
        characters = new HashMap<>();
    }

    public String combinePrompts(List<ChatMessage> msgs){
        String prevType = "";
        StringBuilder combinedText = new StringBuilder();
        for(ChatMessage msg : msgs){
            if(msg instanceof ChatMessage.UserMessage message){
//                if(!prevType.equals("user"))
//                    combinedText.append("USER:");
                combinedText.append("\n").append(message.getContent());
                prevType="user";
            } else if(msg instanceof ChatMessage.AssistantMessage message){
//                if(!prevType.equals("assistant"))
//                    combinedText.append("ASSISTANT:");
                combinedText.append("\n").append(message.getContent());
                prevType="assistant";
            } else if(msg instanceof ChatMessage.SystemMessage message){
                if(!prevType.equals("system"))
                    combinedText.append("SYSTEM:");
                combinedText.append("\n").append(message.getContent());
                prevType="system";
            }
        };
        return combinedText.toString();
    }

    public List<ChatMessage> trimListToMeetTokens(List<ChatMessage> msgs, int startAt){
        Encoding enc = registry.getEncodingForModel(this.model.substring(this.model.lastIndexOf("/")))
                .orElse(registry.getEncoding(EncodingType.CL100K_BASE));

        String combinedText = combinePrompts(msgs);

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
                components.add(InteractionCreator.createPermanentButton(Button.danger("destroy", Emoji.fromFormatted("🗑")),
                        Interactions.getDestroyMessage()));
                components.add(InteractionCreator.createPermanentButton(Button.success("edit", Emoji.fromFormatted("🪄")),
                        Interactions.getEditMessage()));
            }

            latestAssistantMessage.editMessage(finalResponse)
                    .setActionRow(components).queue();
        }
    }

    public CharacterData findRespondingCharacterFromContent(String content){
        content = content.toLowerCase();
        final String finalContent = content;
        Server server = AIBot.bot.getServerData(guild);

        Optional<CharacterData> foundCharacter = server.getCharacterDatas().entrySet().stream().filter(entry ->
                        finalContent.contains(entry.getValue().getFirstName().toLowerCase())).map(entry -> entry.getValue())
                .findFirst();

        if(foundCharacter.isPresent() && getCharacters().containsKey(foundCharacter.get().getName())){
            return foundCharacter.get();
        }
        return null;
    }

    public CharacterData findRespondingCharacterFromMessage(Message msg){
         if(msg.getReferencedMessage() != null && msg.getReferencedMessage().isWebhookMessage()){
            CharacterData character =
                    AIBot.bot.getServerData(guild).getCharacterDatas().get(msg.getReferencedMessage().getAuthor().getName());

            if(character == null || !getCharacters().containsKey(character.getName())){
                return null;
            }
            return character;
        }
        return null;
    }

    public CompletionRequest createTextRequest(CharacterData character){
        return CompletionRequest.builder()
                .maxTokens(this.maxTokens)
                .model(this.model)
                .prompt(combinePrompts(getHistory(character)))
                .temperature(1.0)
                .build();
    }

    public ChatRequest createChatRequest(CharacterData character){
        return ChatRequest.builder()
                .maxCompletionTokens(this.maxTokens)
                .model(this.model)
                .messages(getHistory(character))
                .temperature(1.0)
                .build();
    }

    public SimpleOpenAI createLLM(){
        try {
            return SimpleOpenAI.builder()
                    .baseUrl(Constants.BASE_URL)
                    .apiKey(((ConfigEntry.StringConfig) AIBot.bot.getServerData(guild).getConfig().get("open_router_key")).value)
                    .build();
        } catch(Exception e){
            return null;
        }
    }

    public String createCustomResponse(String content){
        ArrayList<ChatMessage> history = new ArrayList<>();
        history.add(ChatMessage.UserMessage.of(content, "Admin"));

        ChatRequest chatRequest = ChatRequest.builder()
                .maxCompletionTokens(this.maxTokens)
                .model(this.model)
                .messages(history)
                .build();

        SimpleOpenAI llm = createLLM();

        if(llm == null){
            return null;
        }

        CompletableFuture<Chat> futureChat = llm.chatCompletions()
                .create(chatRequest);
        Chat chat = futureChat.join();

        return chat.firstContent();
    }

    private String generateResponse(CharacterData character, boolean useChatCompletion, Consumer<String> consumer){
        ChatRequest chatRequest = useChatCompletion ? createChatRequest(character) : null;
        CompletionRequest completionRequest = !useChatCompletion ? createTextRequest(character) : null;

        SimpleOpenAI llm = createLLM();

        if(llm == null){
            return null;
        }

        try{
            CompletableFuture<Stream<Chat>> futureChat = useChatCompletion ? llm.chatCompletions()
                    .createStream(chatRequest) : null;
            CompletableFuture<Stream<Completion>> futureCompletion = !useChatCompletion ? llm.completions()
                    .createStream(completionRequest) : null;

            Stream<Chat> chat = futureChat != null ? futureChat.join() : null;
            Stream<Completion> completion = futureCompletion != null ? futureCompletion.join() : null;

            var fullResponseWrapper = new Object(){String fullResponse = "";};

            if(chat != null){
                chat.filter(chatResp -> chatResp.getChoices() != null && !chatResp.getChoices().isEmpty() && chatResp.firstContent() != null)
                        .map(Chat::firstContent)
                        .forEach(partialResponse -> {
                            fullResponseWrapper.fullResponse += partialResponse;
                            consumer.accept(fullResponseWrapper.fullResponse);
                        });
            } else {
                completion.filter(chatResp -> chatResp.getChoices() != null && !chatResp.getChoices().isEmpty() && chatResp.firstText() != null)
                        .map(Completion::firstText)
                        .forEach(partialResponse -> {
                            fullResponseWrapper.fullResponse += partialResponse;
                            consumer.accept(fullResponseWrapper.fullResponse);
                        });
            }

            return fullResponseWrapper.fullResponse;
        }catch(CleverClientException e){
            return null;
        }
    }

    public boolean usingChatCompletion(){
        return ((ConfigEntry.BoolConfig)AIBot.bot.getServerData(guild).getConfig().get("use_chat_completions")).value;
    }

    private String streamOnDiscordMessage(CharacterData character, Message msgToEdit){
        AtomicBoolean queued = new AtomicBoolean(false);
        AtomicLong timeResponseMade = new AtomicLong(System.currentTimeMillis());
        double timeBetween = 1;

        String botUser = character.getName() + ":";

        String finalResponse = generateResponse(character, usingChatCompletion(), response -> {
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
        if(finalResponse == null)
            throw new RuntimeException("Failed to create the LLM! Ensure that the API key is valid!");
        String newContent = finalResponse.replaceAll(botUser, "");
        if(newContent.isEmpty())
            throw new RuntimeException("Empty message! :(");
        return newContent;
    }

    public void promptCharacterToRoleplay(CharacterData character, Message replyTo, boolean triggerAutoResponse, boolean waitForFinish) throws ExecutionException, InterruptedException {
        if(isRunningRoleplay()){
            addCharacter(character);
            setCurrentCharacter(character.getName());
            sendRoleplayMessage(triggerAutoResponse, waitForFinish);
        }
    }

    // selects a random character based on their talking likeliness
    public void selectRandomCharacter(){

    }

    public void sendRoleplayMessage(boolean triggerAutoResponse, boolean waitForFinish) throws ExecutionException, InterruptedException {
        if(!runningRoleplay){
            channel.sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since there is no ongoing chat!")
            )).queue();
            return;
        }
        if (isMakingResponse()) {
            channel.sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since I am already generating one!")
            )).queue();
            return;
        }
        if(currentCharacter == null){
            channel.sendMessage(MessageCreateData.fromContent(
                    Util.botifyMessage("Cannot make a response since there are no characters in this chat!")
            )).queue();
            return;
        }

        if(this.errorMsgCleanup != null){
            this.errorMsgCleanup.delete().queue(RestAction.getDefaultSuccess(), toThrow -> {});
            if(this.latestAssistantMessage == errorMsgCleanup)
                latestAssistantMessage = null;
            this.errorMsgCleanup = null;
        }

        if(this.latestAssistantMessage != null){
            if(latestAssistantMessage.getContentRaw().isEmpty())
                latestAssistantMessage.delete().queue();
            else
                latestAssistantMessage.editMessageComponents(
                        ActionRow.of(Button.danger("destroy_button", Emoji.fromFormatted("🗑")),
                                Button.success("edit_button", Emoji.fromFormatted("🪄")))).queue();
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

        Consumer<Message> consumer = (aiMsg) -> {
            latestAssistantMessage = aiMsg;
            swipes = new ArrayList<>();
            try {
                String finalResponse = streamOnDiscordMessage(currentCharacter, aiMsg);
//                latestAssistantMessage = aiMsg;
                swipes.add(finalResponse);
                errorMsgCleanup = null;
                this.finishedDiscordResponse(finalResponse);

                // if able to reply to other bots..
                // add chaining
                if(triggerAutoResponse){
                    CharacterData data = findRespondingCharacterFromContent(finalResponse);
                    if(data != null && data != currentCharacter && data.getTalkability() >= Math.random()) {
                        try{
                            promptCharacterToRoleplay(data, latestAssistantMessage, true, waitForFinish);
                        } catch (Exception ignored){

                        }
                    }
                }
            } catch (Exception e) {
                String overrideError = null;
                if(e instanceof IllegalArgumentException ignored){
                    overrideError = "Response is too long!";
                }

                this.finishedDiscordResponse(Util.botifyMessage("Failed to send a response due to an exception :< sowwy. Try using a different AI model.\nError: " + (overrideError != null ? overrideError : e.toString().substring(0, Math.min(e.toString().length(), 1750)))));
                errorMsgCleanup = aiMsg;
                currentSwipe = 0;
                throw(e);
            }
        };

        if(waitForFinish){
            consumer.accept(messageCreateData.submit().get());
        } else {
            messageCreateData.queue(consumer);
        }
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
                        String msg = e.toString().contains("Content may not be longer") ? "Content is too long!" : e.toString();
                        finalResponse = "```Failed to make a new response!\nError: " + msg + "```";
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

        if(character != null){
            messages.add(ChatMessage.SystemMessage.of("<INSTRUCTIONS>\nFollow the instructions below! You are participating in a roleplay with other users!", "INSTRUCTIONS"));
            messages.add(ChatMessage.SystemMessage.of("This is a chatbot roleplay. You are roleplaying with other users, your responses should only be a few sentences long, should incorporate humor and shouldn't be too serious. The only time this can be overridden is if later instructions conflict with these. Keep responses within a few sentences!"));
            for(InstructionData instructionData : instructions){
                messages.add(instructionData.getChatMessage(character));
            }

            messages.add(ChatMessage.SystemMessage.of("<WORLD LORE>\n The following is lore and information about the world that this roleplay takes place in!"));
            for(WorldData worldData : worldLore){
                messages.add(worldData.getChatMessage(character));
            }

//            StringBuilder multipleCharacters = new StringBuilder("For your response, you will be replying as {{char}}. Do not respond as any of the other characters in this group except {{char}}: ");
//            for(String name : characters.keySet()){
//                multipleCharacters.append(name).append(", ");
//            };
//            multipleCharacters.replace(multipleCharacters.lastIndexOf(", "), multipleCharacters.length(), ".");
//
//            messages.add(
//                    ChatMessage.SystemMessage.of(multipleCharacters.toString())
//            );
            messages.add(
                    ChatMessage.SystemMessage.of("Do not include the character name in your response, this is already provided programmatically by the code.")
            );
            messages.add(ChatMessage.SystemMessage.of("<CHARACTER PERSONA>\nUnderstand the character definition below! This is the character you will be playing in the roleplay.", "CHARACTER"));
            messages.add(character.getChatMessage(character));
            messages.add(ChatMessage.SystemMessage.of("<CHAT HISTORY>"));
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

        boolean isChatCompletion = usingChatCompletion();
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

                String formatted = contents
                        .replaceAll("<@" + message.getAuthor().getId() + ">", "")
                        .replaceAll("<|im_end|>", "");
                String finalContent = "";
                if(!isChatCompletion){
                    finalContent += "<|im_end|>\n";
                }
                finalContent += username + ": " + formatted;

                if (message.isWebhookMessage()) {
                    messages.add(
                            ChatMessage.AssistantMessage.builder()
                                    .content(finalContent)
                                    .name(username)
                                    .build()
                    );
                } else {
                    messages.add(ChatMessage.UserMessage.of(finalContent, username));
                }
            }
        }
        if(isChatCompletion && character != null)
            messages.add(ChatMessage.SystemMessage.of("Write as " + character.getName() + " for your next response!"));
        return trimListToMeetTokens(messages, required);
    }

    public void startRoleplay(Message introMessage, List<InstructionData> instructionList, List<WorldData> worldDatas, List<CharacterData> characterList) throws ExecutionException, InterruptedException, IOException {
        if(instructionList.size() <= 0)
            throw new RuntimeException("Need at least one set of instructions!");
        if(worldDatas.size() <= 0)
            throw new RuntimeException("Need at least one set of world lore!");

        historyStart = introMessage.getTimeCreated();
        channel = introMessage.getChannel().asTextChannel();

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

        characterList.forEach(this::addCharacter);
        instructionList.forEach(this::addInstructions);
        worldDatas.forEach(this::addWorldLore);

        Stream<String> stream = characters.keySet().stream();
        stream.findAny().ifPresent(this::setCurrentCharacter);
    }

    public void restartRoleplay(Message introMessage){
        latestAssistantMessage = null;
        swipes = null;
        currentSwipe = 0;

        historyStart = introMessage.getTimeCreated();
        channel = introMessage.getChannel().asTextChannel();
    }

    public void stopRoleplay(){
        if(this.isRunningRoleplay()){
            runningRoleplay = false;
            historyStart = null;
            channel = null;
            webhook = null;
            latestAssistantMessage = null;
            swipes = null;
            currentSwipe = 0;
            characters.clear();
            instructions.clear();
            worldLore.clear();
        }
    }

    public int getCurrentSwipe(){
        return currentSwipe;
    }

    public ArrayList<String> getSwipes(){
        return swipes;
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

    public List<WorldData> getWorlds(){
        return worldLore;
    }

    public List<InstructionData> getInstructions() {
        return instructions;
    }

    public HashMap<String, CharacterData> getCharacters() {
        return characters;
    }

    public List<? extends Data> getDatas(PromptType promptType){
        return switch(promptType){
            case CHARACTER -> getCharacters().values().stream().toList();
            case WORLD -> getWorlds();
            case INSTRUCTION -> getInstructions();
        };
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

    public void addWorldLore(WorldData worldLore){
        this.worldLore.add(worldLore);
    }
}
