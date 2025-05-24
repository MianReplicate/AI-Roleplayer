package discord.mian.data.character;

import discord.mian.api.Chattable;
import discord.mian.data.Data;
import discord.mian.data.Server;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Character extends Data<CharacterDocument> implements Chattable {
    public Character(CharacterDocument document) {
        super(CharacterDocument.class, document);
    }

    public InputStream downloadAvatar() throws IOException {
        URL url = new URL(getDocument().getAvatar());

        return url.openStream();
    }

    public String getFirstName() {
        String name = document.getName();
        int spaceIndex = name.indexOf(" ");
        if (spaceIndex != -1)
            return name.substring(0, spaceIndex);
        else
            return name;
    }

    public ChatMessage.SystemMessage getChatMessage(Character ignored) {
        String definition = getPrompt();
        definition = definition.replaceAll("\\{\\{char}}", getName());

        return ChatMessage.SystemMessage.of(definition, getName());
    }
}
