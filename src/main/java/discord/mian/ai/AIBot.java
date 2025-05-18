package discord.mian.ai;

import discord.mian.custom.Constants;
import discord.mian.data.Server;
import discord.mian.commands.BotCommands;
import discord.mian.custom.Util;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AIBot {
    public static AIBot bot;

    private final JDA jda;

    private Map<Guild, Roleplay> chats;
    private Map<Guild, Server> servers;

    public AIBot(JDA jda) throws Exception {
        if (bot != null)
            throw new RuntimeException("Can't create multiple AI-Bots!");
        bot = this;
        jda.awaitReady();
        this.servers = new HashMap<>();
        this.chats = new HashMap<>();
        this.jda = jda;
        Constants.ALLOWED_USER_IDS.add(this.jda.retrieveApplicationInfo().submit().get().getOwner().getIdLong());

        BotCommands.addCommands().queue();

        for(Guild guild : jda.getGuildCache()){
            onServerJoin(guild);
        }

        for(File serverFolder : Objects.requireNonNull(Util.createFileRelativeToData("servers").listFiles())){
            long id = Long.valueOf(serverFolder.getName());
            if(jda.getGuildById(id) == null)
                serverFolder.delete(); // removes data of servers we are no longer in
        }
    }

    public JDA getJDA() {
        return this.jda;
    }

    public Roleplay getChat(Guild guild) {
        if(!this.chats.containsKey(guild))
            AIBot.bot.createChat(guild);

        return this.chats.get(guild);
    }

    public Server getServerData(Guild guild){
        return servers.get(guild);
    }

    public void createChat(Guild guild) {
        Roleplay chat = new Roleplay(guild);
        Server serverData = getServerData(guild);

        this.chats.put(guild, chat);
    }

    public void onServerJoin(Guild guild){
        servers.put(guild, new Server(guild));
    }
}
