package discord.mian.ai.data;

import discord.mian.custom.Constants;
import discord.mian.custom.Util;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Server {
    private HashMap<String, CharacterData> characterDatas;
    private HashMap<String, InstructionData> instructionDatas;
    private Guild guild;

    public Server(Guild guild){
        this.guild = guild;
        getServerData(); // ensures it exists
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
                characterDatas.putIfAbsent(character.getName(), new CharacterData(character));
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

    public void createCharacter(String name, String definition) throws IOException {
        File charactersFolder = getCharactersFolder();
        File characterFolder = new File(charactersFolder.getPath() + "/" + name);

        String defPath = charactersFolder.getPath() + "/" + "definition.txt";

        if(!characterFolder.mkdir())
            characterFolder.mkdir();

        File file = new File(defPath);
        if(!file.exists())
            file.createNewFile();

        FileWriter writer = new FileWriter(defPath);
        writer.write(definition);
        writer.close();

        characterDatas.putIfAbsent(name, new CharacterData(characterFolder));
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

//    public boolean addCharacter(String name, Message.Attachment definition, Message.Attachment avatar){
//        File charactersFolder = getCharactersFolder();
//        File characterFolder = new File(charactersFolder.getPath() + "/" + name);
//
//        String defPath = charactersFolder.getPath() + "/" + "definition.txt";
//        String avatarPath = characterFolder.getPath() + "/" + "avatar." + avatar.getFileExtension();
//
//        if(!characterFolder.exists()){
//            try{
//                if(definition == null || avatar == null)
//                    throw new RuntimeException("No definition or avatar provided for " + name);
//
//                characterFolder.mkdir();
//
//                File definitionFile =
//                        definition.getProxy()
//                                .downloadToFile(new File(defPath)).get();
//
//                Files.readString(definitionFile.toPath(), StandardCharsets.UTF_8);
//
//                File avatarFile =
//                        avatar.getProxy()
//                                .downloadToFile(new File(avatarPath)).get();
//
//                if(!Util.isValidImage(avatarFile)){
//                    throw new RuntimeException("Invalid avatar image!");
//                }
//            } catch (Exception e){
//                Constants.LOGGER.info("Failed to add character: " + name);
//                characterFolder.delete();
//
//                return false;
//            }
//        } else {
//            if(definition != null){
//                try{
//                    File definitionFile =
//                            definition.getProxy()
//                                    .downloadToFile(new File(defPath)).get();
//                    Files.readString(definitionFile.toPath(), StandardCharsets.UTF_8);
//                } catch(Exception e){
//                    Constants.LOGGER.info("Failed to replace definition for character: " + name);
//                    return false;
//                }
//            }
//            if(avatar != null){
//                try{
//                    File avatarFile =
//                            avatar.getProxy()
//                                    .downloadToFile(new File(avatarPath)).get();
//
//                    if(!Util.isValidImage(avatarFile)){
//                        throw new RuntimeException("Invalid avatar image!");
//                    }
//                } catch(Exception e){
//                    Constants.LOGGER.info("Failed to replace avatar for character: " + name);
//                    return false;
//                }
//            }
//        }
//
//        return true;
//    }

    public void removeCharacter(){

    }
}
