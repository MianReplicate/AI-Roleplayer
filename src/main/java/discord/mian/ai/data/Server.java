package discord.mian.ai.data;

import discord.mian.custom.Constants;
import discord.mian.custom.Util;
import net.dv8tion.jda.api.entities.Guild;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;

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
        if(!serverFolder.exists())
            serverFolder.mkdir();

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

            for(File instruction : Objects.requireNonNull(instructionsFolder.listFiles())){
                try{
                    int place = instruction.getName().indexOf(".");
                    if(place != -1){
                        if(instruction.getName().substring(place).equals(".txt")){
                            instructionDatas.put(instruction.getName().substring(0, place), new InstructionData(instruction));
                        }
                    }
                }catch(Exception e){
                    Constants.LOGGER.info("Failed to load instruction " + instruction.getName() + " for guild: "+guild.getName());
                    Constants.LOGGER.error(String.valueOf(e));
                }
            }
        }

        return instructionDatas;
    }

    public HashMap<String, CharacterData> getCharacterDatas() {
        File charactersFolder = Util.createFileRelativeToData(getServerPath() + "/characters");
        if(!charactersFolder.exists())
            charactersFolder.mkdir();

        if(characterDatas == null){
            characterDatas = new HashMap<>();

            for(File character : Objects.requireNonNull(charactersFolder.listFiles())){
                try{
                    characterDatas.put(character.getName(), new CharacterData(character));
                }catch(Exception e){
                    Constants.LOGGER.info("Failed to load character " + character.getName() + " for guild: "+guild.getName());
                    Constants.LOGGER.error(String.valueOf(e));
                }
            }
        }

        return characterDatas;
    }

    public void addCharacter(){

    }

    public void removeCharacter(){

    }
}
