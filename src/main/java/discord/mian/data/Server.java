package discord.mian.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import discord.mian.custom.ConfigEntry;
import discord.mian.custom.Constants;
import discord.mian.custom.Util;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Server {
    private HashMap<String, CharacterData> characterDatas;
    private HashMap<String, InstructionData> instructionDatas;
    private Guild guild;

    public Server(Guild guild) {
        this.guild = guild;
        getServerData(); // ensures it exists
    }

    public Role getMasterRole(){
        return guild.getRoleById(((ConfigEntry.LongConfig)getConfig().get("bot_role_id")).value);
    }

    public File getServerData(){
        File serverFolder = Util.createFileRelativeToData(getServerPath());
        if(!serverFolder.exists()){
            serverFolder.mkdir();

            // new server, let's add some data :D
            File defaults = new File(Util.getDataFolder().getPath() + "\\defaults");
            try{
                // copies directories and files within the defaults
                Util.copyDirectory(defaults.toPath(), serverFolder.toPath());
            }catch(Exception ignored){
                // whoops, we tried :(
                Constants.LOGGER.info("Failed to copy default files!");
            }
        }

        return serverFolder;
    }

    public String getServerPath(){
        return "servers/" + guild.getIdLong();
    }

    public void generateConfig() throws IOException {
        File dataJson = Util.createFileRelativeToData(getServerPath() + "/config.json");
        if(!dataJson.exists())
            dataJson.createNewFile();

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();

        Map<String, ConfigEntry> configEntries;
        try{
            configEntries = objectMapper.readValue(dataJson, new TypeReference<HashMap<String, ConfigEntry>>(){});
        }catch(Exception ignored){
            configEntries = new HashMap<>();
        }

        ConfigEntry.StringConfig openRouter = new ConfigEntry.StringConfig();
        openRouter.description = "The API key to use for models on Open Router";
        openRouter.value = "";
        configEntries.putIfAbsent("open_router_key", openRouter);

        ConfigEntry.StringConfig imgbb = new ConfigEntry.StringConfig();
        imgbb.description = "The API key to use for getting and posting avatars on IMGBB";
        imgbb.value = "";
        configEntries.putIfAbsent("imgbb_key", imgbb);

        ConfigEntry.BoolConfig onlyChatOnMention = new ConfigEntry.BoolConfig();
        onlyChatOnMention.description = "Whether the AI will only reply when mentioned through reply or its name";
        onlyChatOnMention.value = false;
        configEntries.putIfAbsent("only_chat_on_mention", onlyChatOnMention);

        ConfigEntry.LongConfig botRole = new ConfigEntry.LongConfig();
        botRole.description = "Long ID of the bot controller role";
        botRole.hidden = true;
        botRole.value = 0L;
        configEntries.putIfAbsent("bot_role_id", botRole);

//        ConfigEntry.IntConfig autoMode = new ConfigEntry.IntConfig();
//        autoMode.description = "How many seconds until the AI responds automatically, set to -1 to disable";
//        autoMode.value = -1;
//        configEntries.put("auto_mode", autoMode);

        writer.writeValue(dataJson, configEntries);
    }

    public boolean saveToConfig(HashMap<String, ConfigEntry> entries) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
        try{
            writer.writeValue(Util.createFileRelativeToData(getServerPath() + "/config.json"), entries);
            return true;
        } catch(Exception ignored){

        }
        return false;
    }

    public HashMap<String, ConfigEntry> getConfig() {
        File dataJson = Util.createFileRelativeToData(getServerPath() + "/config.json");

        ObjectMapper objectMapper = new ObjectMapper();
        try{
            generateConfig();
            return objectMapper.readValue(dataJson, new TypeReference<HashMap<String, ConfigEntry>>(){});
        } catch (Exception exception){
            Constants.LOGGER.info(String.valueOf(exception));
        }
        return null;
    }

    public HashMap<String, InstructionData> getInstructionDatas(){
        File instructionsFolder = Util.createFileRelativeToData(getServerPath() + "/instructions");
        if(!instructionsFolder.exists())
            instructionsFolder.mkdir();

        if(instructionDatas == null){
            instructionDatas = new HashMap<>();
        }

        for(File instruction : Objects.requireNonNull(instructionsFolder.listFiles())){
            try{
                int place = instruction.getName().lastIndexOf(".");
                if(place != -1){
                    if(instruction.getName().substring(place).equals(".txt")){
                        instructionDatas.putIfAbsent(instruction.getName().substring(0, place), new InstructionData(instruction));
                    }
                }
            }catch(Exception e){
                Constants.LOGGER.info("Failed to load instruction " + instruction.getName() + " for guild: "+guild.getName());
                Constants.LOGGER.error(String.valueOf(e));
            }
        }

        return instructionDatas;
    }

    public HashMap<String, CharacterData> getCharacterDatas() {
        File charactersFolder = getCharactersFolder();

        if(characterDatas == null){
            characterDatas = new HashMap<>();
        }

        for(File character : Objects.requireNonNull(charactersFolder.listFiles())){
            try{
                characterDatas.putIfAbsent(character.getName(), new CharacterData(this, character));
            }catch(Exception e){
                Constants.LOGGER.info("Failed to load character " + character.getName() + " for guild: "+guild.getName());
                Constants.LOGGER.error(String.valueOf(e));
            }
        }

        return characterDatas;
    }

    private File getCharactersFolder(){
        File charactersFolder = Util.createFileRelativeToData(getServerPath() + "/characters");
        if(!charactersFolder.exists())
            charactersFolder.mkdir();
        return charactersFolder;
    }

    private File getInstructionsFolder(){
        File instructionsFolder = Util.createFileRelativeToData(getServerPath() + "/instructions");
        if(!instructionsFolder.exists())
            instructionsFolder.mkdir();
        return instructionsFolder;
    }

    public void createCharacter(String name, String definition, double talkability) throws IOException {
        File charactersFolder = getCharactersFolder();
        File characterFolder = new File(charactersFolder.getPath() + "/" + name);

        String defPath = characterFolder.getPath() + "/" + "definition.txt";

        if(!characterFolder.mkdir())
            characterFolder.mkdir();

        File file = new File(defPath);
        if(!file.exists())
            file.createNewFile();

        FileWriter writer = new FileWriter(defPath);
        writer.write(definition);
        writer.close();

        CharacterData data = new CharacterData(this, characterFolder);
        data.setTalkability(talkability);

        characterDatas.putIfAbsent(name, data);
    }

    public void createInstruction(String name, String prompt) throws IOException {
        File instructionsFolder = getInstructionsFolder();
        File instructionFile = new File(instructionsFolder.getPath() + "/" + name);

        if(!instructionFile.exists())
            instructionFile.createNewFile();

        FileWriter writer = new FileWriter(instructionFile);
        writer.write(prompt);
        writer.close();

        instructionDatas.putIfAbsent(name, new InstructionData(instructionFile));
    }
}
