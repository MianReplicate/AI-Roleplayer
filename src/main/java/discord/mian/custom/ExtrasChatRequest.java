package discord.mian.custom;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.github.sashirestela.openai.common.ResponseFormat;
import io.github.sashirestela.openai.common.StreamOptions;
import io.github.sashirestela.openai.common.tool.Tool;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtrasChatRequest extends ChatRequest {
    private final HashMap<String, Object> fields = new HashMap<>();

    private ExtrasChatRequest(List<ChatMessage> messages, String model, Boolean store, ReasoningEffort reasoningEffort, Map<String, String> metadata, Double frequencyPenalty, Map<String, Integer> logitBias, Boolean logprobs, Integer topLogprobs, Integer maxTokens, Integer maxCompletionTokens, Integer n, List<Modality> modalities, Audio audio, Double presencePenalty, ResponseFormat responseFormat, Integer seed, ServiceTier serviceTier, Object stop, Boolean stream, StreamOptions streamOptions, Double temperature, Double topP, List<Tool> tools, Object toolChoice, Boolean parallelToolCalls, String user) {
        super(messages, model, store, reasoningEffort, metadata, frequencyPenalty, logitBias, logprobs, topLogprobs, maxTokens, maxCompletionTokens, n, modalities, audio, presencePenalty, responseFormat, seed, serviceTier, stop, stream, streamOptions, temperature, topP, tools, toolChoice, parallelToolCalls, user);
    }

    @JsonAnySetter
    public void setField(String string, Object object) {
        fields.put(string, object);
    }

    @JsonAnyGetter
    public HashMap<String, Object> getFields() {
        return fields;
    }

    public static ExtrasChatRequestBuilder extrasBuilder() {
        return new ExtrasChatRequestBuilder();
    }

    public static ExtrasChatRequest from(ChatRequest request) {
        return new ExtrasChatRequest(request.getMessages(), request.getModel(), request.getStore(), request.getReasoningEffort(), request.getMetadata(), request.getFrequencyPenalty(), request.getLogitBias(), request.getLogprobs(), request.getTopLogprobs(), request.getMaxTokens(), request.getMaxCompletionTokens(), request.getN(), request.getModalities(), request.getAudio(), request.getPresencePenalty(), request.getResponseFormat(), request.getSeed(), request.getServiceTier(), request.getStop(), request.getStream(), request.getStreamOptions(), request.getTemperature(), request.getTopP(), request.getTools(), request.getToolChoice(), request.getParallelToolCalls(), request.getUser());
    }

    public static class ExtrasChatRequestBuilder {
        public final ChatRequestBuilder builder;

        public List<String> providers;
        private boolean fallback;

        public ExtrasChatRequestBuilder() {
            builder = ExtrasChatRequest.builder();
        }

        public ExtrasChatRequestBuilder setProviders(String... strings) {
            providers = List.of(strings);
            return this;
        }

        public ExtrasChatRequestBuilder setProviderFallback(boolean allowed) {
            fallback = allowed;
            return this;
        }

        public ExtrasChatRequest build() {
            ExtrasChatRequest request = ExtrasChatRequest.from(builder.build());
            if (providers != null) {
                HashMap<String, Object> providerMap = new HashMap<>();
                providerMap.put("order", providers);
                providerMap.put("allow_fallbacks", fallback);

                request.setField("provider", providerMap);
            }
            return request;
        }
    }
}
