package discord.mian.server.api;

import discord.mian.server.ai.RoleplayChat;

public interface AI {
    RoleplayChat getChat();
    void createChat();

}
