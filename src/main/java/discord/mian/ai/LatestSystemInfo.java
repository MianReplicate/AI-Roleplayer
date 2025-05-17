package discord.mian.ai;

public class LatestSystemInfo {
    private String provider;
    private String model;
    private int promptTokens;
    private int completionTokens;

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getTotalTokens(){
        return completionTokens + promptTokens;
    }

    public String getModel() {
        return model != null && !model.isEmpty() ? model : "Unknown!";
    }

    public String getProvider() {
        return provider != null && !provider.isEmpty() ? provider : "Unknown!";
    }
}
