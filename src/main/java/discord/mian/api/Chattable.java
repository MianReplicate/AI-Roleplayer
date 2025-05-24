package discord.mian.api;

import discord.mian.data.character.Character;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

public interface Chattable {
    ChatMessage getChatMessage(Character data);
}
