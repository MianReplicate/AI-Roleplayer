package discord.mian.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord.mian.custom.Constants;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

public class ResponseInfo {
    private final String provider;
    private final String model;
    private String response;
    private final String sent;
    private final Optional<Integer> promptTokens;
    private final Optional<Integer> completionTokens;

    public ResponseInfo(String model, String provider, String response, Integer promptTokens, Integer completionTokens, String sent){
        this.provider = provider;
        this.model = model;
        this.response = response;
        this.promptTokens = Optional.ofNullable(promptTokens);
        this.completionTokens = Optional.ofNullable(completionTokens);
        this.sent = sent;
    }

    public Optional<Integer> getCompletionTokens() {
        return completionTokens;
    }

    public Optional<Integer> getPromptTokens() {
        return promptTokens;
    }

    public int getTotalTokens(){
        return completionTokens.orElse(0) + promptTokens.orElse(0);
    }

    public String getModel() {
        return model != null && !model.isEmpty() ? model : "Unknown!";
    }

    public String getProvider() {
        return provider != null && !provider.isEmpty() ? provider : "Unknown!";
    }

    public String getResponse(){
        return response != null && !response.isEmpty() ? response : "No response!";
    }

    public String editResponse(Function<String, String> edit){
        this.response = edit.apply(response);
        return response;
    }

    public String getPrompt(){
        return sent;
    }

    public Double getPrice(){
        try{
            String author = model.substring(0, model.indexOf("/"));
            String slug = model.substring(model.indexOf("/") + 1);

            OkHttpClient client = new OkHttpClient.Builder().build();

            Request request = new Request.Builder()
                    .url("https://openrouter.ai/api/v1/models/"+author+"/"+slug+"/endpoints")
                    .get()
                    .build();

            Call call = client.newCall(request);
            try (Response response = call.execute()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(response.body().string());
                JsonNode dataNode = node.get("data").get("endpoints");

                for(Iterator<JsonNode> it = dataNode.values(); dataNode.values().hasNext();){
                    JsonNode endpoint = it.next();
                    String name = endpoint.get("name").asText();
                    name = name.substring(0, name.indexOf("|")-1);

                    if(name.equals(provider)){
                        double total = 0;
                        total += endpoint.get("pricing").get("prompt").asDouble() * getPromptTokens();
                        total += endpoint.get("pricing").get("completion").asDouble() * getCompletionTokens();
                        return total;
                    }
                }
            }
        }catch(Exception e){
            Constants.LOGGER.error("Failed to get price for response info", e);
        }
        return null;
    }
}
