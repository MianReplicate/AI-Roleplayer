package discord.mian.loxi.common.api;

import io.github.sashirestela.openai.domain.chat.ChatMessage;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public interface BotChat {
    String sendAndGetResponse(String name, String content);
    String sendAndStream(String name, String content, Consumer<String> consumer);
    ArrayList<ChatMessage> getHistory();
    boolean isMakingResponse();
    void restartHistory();
    void setMaxMessages(int messages);
    void setMaxTokens(int tokens);
    void setModel(String model);
}
