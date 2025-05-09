package discord.mian.server.api;

import discord.mian.server.ai.DiscordRoleplay;

public interface AI {
    DiscordRoleplay getChat();
    void createChat();

}
