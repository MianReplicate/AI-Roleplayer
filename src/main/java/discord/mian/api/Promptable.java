package discord.mian.api;

import io.github.sashirestela.openai.domain.chat.ChatMessage;

public interface Promptable {
    ChatMessage getPrompt();
}
