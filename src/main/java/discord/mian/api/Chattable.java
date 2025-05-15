package discord.mian.api;

import discord.mian.data.CharacterData;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

public interface Chattable {
    ChatMessage getChatMessage(CharacterData data);
}
