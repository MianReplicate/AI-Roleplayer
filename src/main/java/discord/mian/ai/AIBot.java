package discord.mian.ai;

import discord.mian.ai.data.CharacterData;
import discord.mian.ai.data.Server;
import discord.mian.commands.BotCommands;
import discord.mian.custom.Util;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AIBot {
    public static AIBot bot;

    private final JDA jda;

    private Map<Guild, DiscordRoleplay> chats;
    private Map<Guild, Server> servers;

    public AIBot(JDA jda) throws Exception {
        if (bot != null)
            throw new RuntimeException("Can't create multiple AI-Bots!");
        bot = this;
        jda.awaitReady();
        this.servers = new HashMap<>();
        this.chats = new HashMap<>();
        this.jda = jda;

        BotCommands.addCommands().queue();

        for(Guild guild : jda.getGuildCache()){
            servers.put(guild, new Server(guild));
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

    public DiscordRoleplay getChat(Guild guild) {
        if(!this.chats.containsKey(guild))
            AIBot.bot.createChat(guild);

        return this.chats.get(guild);
    }

    public Server getServerData(Guild guild){
        return servers.get(guild);
    }

    public void createChat(Guild guild) {
        DiscordRoleplay chat = new DiscordRoleplay(guild);
        Server serverData = getServerData(guild);
//        chat.addInstructions(serverData.getInstructionDatas().get("non-nsfw"));

        this.chats.put(guild, chat);
//        this.funnyMessage = chat.createCustomResponse("[System Command: Respond to the following message in 10 or less words]: \"Fuck you lol, what you gonna do\"");
    }

    public boolean userChattedTo(CharacterData character, Message msg) {
        DiscordRoleplay roleplay = AIBot.bot.getChat(msg.getGuild());
        if(roleplay.isRunningRoleplay()){
            roleplay.addCharacter(character);
            roleplay.setCurrentCharacter(character.getName());
            roleplay.sendRoleplayMessage(msg);
            return true;
        }
        return false;
    }

//    public String getFunnyMessage(Guild guild){
//        if(funnyMessage == null || funnyMessage.isEmpty())
//            chats.get(guild).
//        return funnyMessage;
//    }
}
