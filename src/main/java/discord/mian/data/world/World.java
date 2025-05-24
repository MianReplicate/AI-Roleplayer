package discord.mian.data.world;

import discord.mian.api.Chattable;
import discord.mian.data.Data;
import discord.mian.data.character.Character;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

public class World extends Data<WorldDocument> implements Chattable {
    public World(WorldDocument document) {
        super(WorldDocument.class, document);
    }

    public ChatMessage.SystemMessage getChatMessage(Character data) {
        String definition = getPrompt();
        definition = definition.replaceAll("\\{\\{char}}", data.getName());

        return ChatMessage.SystemMessage.of(definition);
    }
}
