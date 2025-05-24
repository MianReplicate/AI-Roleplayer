package discord.mian.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.mongodb.client.MongoCursor;
import discord.mian.custom.ConfigEntry;
import discord.mian.custom.Constants;
import discord.mian.custom.PromptType;
import discord.mian.custom.Util;
import discord.mian.data.character.Character;
import discord.mian.data.character.CharacterDocument;
import discord.mian.data.instruction.Instruction;
import discord.mian.data.instruction.InstructionDocument;
import discord.mian.data.world.World;
import discord.mian.data.world.WorldDocument;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private HashMap<String, Character> characterDatas;
    private HashMap<String, Instruction> instructionDatas;
    private HashMap<String, World> worldDatas;
    private final Guild guild;

    public Server(Guild guild) {
        this.guild = guild;
        getServerData(); // ensures it exists
    }

    public Role getMasterRole() {
        return guild.getRoleById(((ConfigEntry.LongConfig) getConfig().get("bot_role_id")).value);
    }

    public File getServerData() {
        File serverFolder = Util.createFileRelativeToData(getServerPath());
        if (!serverFolder.exists()) {
            serverFolder.mkdir();

            // new server, let's add some data :D
            File defaults = new File(Util.getDataFolder().getPath() + "/defaults");
            try {
                // copies directories and files within the defaults
                Util.copyDirectory(defaults.toPath(), serverFolder.toPath());
            } catch (Exception e) {
                // whoops, we tried :(
                Constants.LOGGER.error("Failed to copy default files!", e);
            }
        }

        return serverFolder;
    }

    public String getServerPath() {
        return "servers/" + guild.getIdLong();
    }

    public void generateConfig() throws IOException {
        File dataJson = Util.createFileRelativeToData(getServerPath() + "/config.json");
        if (!dataJson.exists())
            dataJson.createNewFile();

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();

        Map<String, ConfigEntry> configEntries;
        try {
            configEntries = objectMapper.readValue(dataJson, new TypeReference<HashMap<String, ConfigEntry>>() {
            });
        } catch (Exception ignored) {
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

        ConfigEntry.DoubleConfig temperature = new ConfigEntry.DoubleConfig();
        temperature.description = "Temperature of model";
        temperature.hidden = true;
        temperature.value = 1;
        configEntries.putIfAbsent("temperature", temperature);

        ConfigEntry.IntConfig tokens = new ConfigEntry.IntConfig();
        tokens.description = "Max tokens of model";
        tokens.hidden = true;
        tokens.value = 8192;
        configEntries.putIfAbsent("tokens", tokens);

        ConfigEntry.StringConfig provider = new ConfigEntry.StringConfig();
        provider.description = "Provider of model";
        provider.hidden = true;
        provider.value = "";
        configEntries.putIfAbsent("provider", provider);

        ConfigEntry.StringConfig model = new ConfigEntry.StringConfig();
        model.description = "The model being used";
        model.hidden = true;
        model.value = Constants.DEFAULT_MODEL;
        configEntries.putIfAbsent("model", model);

//        ConfigEntry.BoolConfig useChatCompletions = new ConfigEntry.BoolConfig();
//        useChatCompletions.description = "Whether to use chat completions for the AI or text completions";
//        useChatCompletions.value = true;
//        configEntries.putIfAbsent("use_chat_completions", useChatCompletions);

//        ConfigEntry.IntConfig autoMode = new ConfigEntry.IntConfig();
//        autoMode.description = "How many seconds until the AI responds automatically, set to -1 to disable";
//        autoMode.value = -1;
//        configEntries.put("auto_mode", autoMode);

        writer.writeValue(dataJson, configEntries);
    }

    public boolean saveToConfig(HashMap<String, ConfigEntry> entries) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
        try {
            writer.writeValue(Util.createFileRelativeToData(getServerPath() + "/config.json"), entries);
            return true;
        } catch (Exception e) {
            Constants.LOGGER.error("Failed to save to configuration", e);
        }
        return false;
    }

    public HashMap<String, ConfigEntry> getConfig() {
        File dataJson = Util.createFileRelativeToData(getServerPath() + "/config.json");

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            generateConfig();
            return objectMapper.readValue(dataJson, new TypeReference<HashMap<String, ConfigEntry>>() {
            });
        } catch (Exception e) {
            Constants.LOGGER.error("Failed to get configuration", e);
        }
        return null;
    }

    public String getKey() {
        return ((ConfigEntry.StringConfig) getConfig().get("open_router_key")).value;
    }

    public HashMap<String, ? extends Data> getDatas(PromptType promptType) {
        return switch (promptType) {
            case INSTRUCTION -> getInstructionDatas();
            case CHARACTER -> getCharacterDatas();
            case WORLD -> getWorldDatas();
            case null, default -> null;
        };
    }

    public HashMap<String, World> getWorldDatas() {
//        File worldsFolder = Util.createFileRelativeToData(getServerPath() + "/worlds");
//        if (!worldsFolder.exists())
//            worldsFolder.mkdir();

        if (worldDatas == null) {
            worldDatas = new HashMap<>();
        }

        try(MongoCursor<WorldDocument> cursor = Util.DATABASE.getCollection("world", WorldDocument.class)
                .find().iterator()){
            while(cursor.hasNext()){
                WorldDocument document = cursor.next();
                worldDatas.putIfAbsent(document.getName(), new World(document));
            }
        }
//
//        for (File world : Objects.requireNonNull(worldsFolder.listFiles())) {
//            try {
//                int place = world.getName().lastIndexOf(".");
//                if (place != -1) {
//                    if (world.getName().substring(place).equals(".txt")) {
//                        worldDatas.putIfAbsent(world.getName().substring(0, place), new World(world));
//                    }
//                }
//            } catch (Exception e) {
//                Constants.LOGGER.info("Failed to load world " + world.getName() + " for guild: " + guild.getName());
//                Constants.LOGGER.error(String.valueOf(e));
//            }
//        }

        return worldDatas;
    }

    public HashMap<String, Instruction> getInstructionDatas() {
//        File instructionsFolder = Util.createFileRelativeToData(getServerPath() + "/instructions");
//        if (!instructionsFolder.exists())
//            instructionsFolder.mkdir();

        if (instructionDatas == null) {
            instructionDatas = new HashMap<>();
        }

        try(MongoCursor<InstructionDocument> cursor = Util.DATABASE.getCollection("instruction", InstructionDocument.class)
                .find().iterator()){
            while(cursor.hasNext()){
                InstructionDocument document = cursor.next();
                instructionDatas.putIfAbsent(document.getName(), new Instruction(document));
            }
        }

//        for (File instruction : Objects.requireNonNull(instructionsFolder.listFiles())) {
//            try {
//                int place = instruction.getName().lastIndexOf(".");
//                if (place != -1) {
//                    if (instruction.getName().substring(place).equals(".txt")) {
//                        instructionDatas.putIfAbsent(instruction.getName().substring(0, place), new Instruction(instruction));
//                    }
//                }
//            } catch (Exception e) {
//                Constants.LOGGER.info("Failed to load instruction " + instruction.getName() + " for guild: " + guild.getName());
//                Constants.LOGGER.error(String.valueOf(e));
//            }
//        }

        return instructionDatas;
    }

    public HashMap<String, Character> getCharacterDatas() {
//        File charactersFolder = getCharactersFolder();

        if (characterDatas == null) {
            characterDatas = new HashMap<>();
        }

        try(MongoCursor<CharacterDocument> cursor = Util.DATABASE.getCollection("character", CharacterDocument.class)
                .find().iterator()){
            while(cursor.hasNext()){
                CharacterDocument document = cursor.next();
                characterDatas.putIfAbsent(document.getName(), new Character(document));
            }
        }

//        for (File character : Objects.requireNonNull(charactersFolder.listFiles())) {
//            try {
//                characterDatas.putIfAbsent(character.getName(), new discord.mian.data.character.Character(this, character));
//            } catch (Exception e) {
//                Constants.LOGGER.info("Failed to load character " + character.getName() + " for guild: " + guild.getName());
//                Constants.LOGGER.error(String.valueOf(e));
//            }
//        }

        return characterDatas;
    }

//    private File getCharactersFolder() {
//        File charactersFolder = Util.createFileRelativeToData(getServerPath() + "/characters");
//        if (!charactersFolder.exists())
//            charactersFolder.mkdir();
//        return charactersFolder;
//    }
//
//    private File getInstructionsFolder() {
//        File instructionsFolder = Util.createFileRelativeToData(getServerPath() + "/instructions");
//        if (!instructionsFolder.exists())
//            instructionsFolder.mkdir();
//        return instructionsFolder;
//    }
//
//    private File getWorldsFolder() {
//        File worldsFolder = Util.createFileRelativeToData(getServerPath() + "/worlds");
//        if (!worldsFolder.exists())
//            worldsFolder.mkdir();
//        return worldsFolder;
//    }

    public void createCharacter(String name, String definition, double talkability) throws IOException {
//        File charactersFolder = getCharactersFolder();
//        File characterFolder = new File(charactersFolder.getPath() + "/" + name);
//
//        String defPath = characterFolder.getPath() + "/" + "definition.txt";
//
//        if (!characterFolder.mkdir())
//            characterFolder.mkdir();

//        File file = new File(defPath);
//        if (!file.exists())
//            file.createNewFile();

//        FileWriter writer = new FileWriter(defPath);
//        writer.write(definition);
//        writer.close();
        Character data = new Character(new CharacterDocument(name));
        data.updateDocument(document -> {
            document.setPrompt(definition);
            document.setTalkability(talkability);
        });

        characterDatas.putIfAbsent(name, data);
    }

    public void createInstruction(String name, String prompt) throws IOException {
//        File instructionsFolder = getInstructionsFolder();
//        File instructionFile = new File(instructionsFolder.getPath() + "/" + name + ".txt");
//
//        if (!instructionFile.exists())
//            instructionFile.createNewFile();
//
//        FileWriter writer = new FileWriter(instructionFile);
//        writer.write(prompt);
//        writer.close();
        Instruction data = new Instruction(new InstructionDocument(name));
        data.updateDocument(document -> document.setPrompt(prompt));

        instructionDatas.putIfAbsent(name, data);
    }

    public void createWorld(String name, String prompt) throws IOException {
//        File worldsFolder = getWorldsFolder();
//        File worldsFile = new File(worldsFolder.getPath() + "/" + name + ".txt");
//
//        if (!worldsFile.exists())
//            worldsFile.createNewFile();
//
//        FileWriter writer = new FileWriter(worldsFile);
//        writer.write(prompt);
//        writer.close();
        World data = new World(new WorldDocument(name));
        data.updateDocument(document -> document.setPrompt(prompt));

        worldDatas.putIfAbsent(name, data);
    }

}
