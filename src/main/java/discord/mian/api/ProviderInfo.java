package discord.mian.api;

import java.util.Optional;

public interface ProviderInfo {
    Optional<Integer> getCompletionTokens();
    int getTotalTokens();
    Optional<Integer> getPromptTokens();
    String getModel();
    String getProvider();
    String getResponse();
    Double getPrice();
}
