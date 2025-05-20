package discord.mian.ai;

import discord.mian.api.PromptInfo;

public class FailedResponseInfo extends RuntimeException implements PromptInfo {
    private final String sent;
    private final String response;

    public FailedResponseInfo(String sent, String response, String message) {
        super(message);
        this.sent = sent;
        this.response = response;
    }

    public String getPrompt() {
        return sent;
    }

    public String getResponse() {
        return response;
    }
}
