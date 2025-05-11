package discord.mian.ai.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord.mian.api.Data;
import discord.mian.api.Chattable;
import discord.mian.custom.Constants;
import discord.mian.custom.Util;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class CharacterData implements Data, Chattable {
    private final File characterFolder;

    public CharacterData(File characterFolder) throws IOException {
        this.characterFolder = characterFolder;

        if(getPrompt() == null)
            throw new RuntimeException("Invalid character!");
    }

    public String getAvatarLink(){
        File file = getAvatar();
        String link = null;
        if(file != null){
            try {
                boolean success = false;
                File dataJson = new File(characterFolder.getPath() + "/data.json");

                ObjectMapper objectMapper = new ObjectMapper();
                if(!dataJson.exists())
                    objectMapper.writeValue(dataJson, new HashMap<String, String>());
                else{
                    Map<String, String> map = objectMapper.readValue(dataJson, Map.class);
                    link = map.get("avatar_link");

                    if(link != null && !link.isEmpty()){
                        HttpClient httpClient = HttpClient.newHttpClient();
                        HttpRequest request = HttpRequest
                                .newBuilder(URI.create(link))
                                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                                .timeout(Duration.ofSeconds(5)) // wait 5 seconds and if it doesn't work, fuck it
                                .build();
                        int statusCode = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream()).statusCode();
                        if(statusCode >= 200 && statusCode < 400)
                            success = true;
                        Constants.LOGGER.info("Found avatar link for " + getName());
                    }
                }

                if(!success){
                    Map<String, String> map = objectMapper.readValue(dataJson, Map.class);
                    link = Util.uploadImageToImgur(file);
                    map.put("avatar_link", link);
                    objectMapper.writeValue(dataJson, map);
                    Constants.LOGGER.info("Writing avatar link for " + getName());
                }
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException("Failed to find a valid avatar link for this character: " + e);
            }
        }
        return link;
    }

    public File getCharacterFolder() {
        return characterFolder;
    }

    public File getAvatar() {
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".webp"};

        File avatar = null;
        for(String extension : extensions){
            File temp = new File(characterFolder.getPath()+"\\avatar"+extension);
            if(temp.exists()){
                avatar = temp;
                break;
            }
        }

        return avatar;
    }

    public String getName(){
        return characterFolder.getName();
    }

    public String getPrompt() throws IOException {
        File definition = new File(characterFolder.getPath() + "/definition.txt");
        if(!definition.exists())
            return null;

        return Files.readString(definition.toPath(), StandardCharsets.UTF_8);
    }

    public void addOrReplacePrompt(String text) throws IOException {
        File definition = new File(characterFolder.getPath() + "/definition.txt");
        if(!definition.exists())
            definition.createNewFile();

        FileWriter writer = new FileWriter(definition.getPath());
        writer.write(text);
        writer.close();
    }

    public ChatMessage getChatMessage(CharacterData ignored){
        String definition;
        try {
            definition = getPrompt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        definition = definition.replaceAll("\\{\\{char}}", getName());

        return ChatMessage.SystemMessage.of(
                definition,
                "Character Definition"
        );
    }

    // when worst comes to worst!
    public void nuke() throws IOException {
        if(characterFolder.exists()){
            Files.walk(characterFolder.toPath()).forEach(path -> path.toFile().delete());
        }
        characterFolder.delete();
    }
}
