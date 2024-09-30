package discord.mian.loxi.common.api;

import io.github.sashirestela.openai.domain.chat.ChatMessage;

import java.util.ArrayList;

public interface BotChat {
    String sendAndGetResponse(String name, String content);
    ArrayList<ChatMessage> getHistory();
    boolean isMakingResponse();
    void restartHistory();
}
