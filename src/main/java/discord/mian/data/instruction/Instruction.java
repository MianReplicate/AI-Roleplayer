package discord.mian.data.instruction;

import discord.mian.api.Chattable;
import discord.mian.data.Data;
import discord.mian.data.character.Character;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

public class Instruction extends Data<InstructionDocument> implements Chattable {
    public Instruction(InstructionDocument instructionDocument) {
        super(InstructionDocument.class, instructionDocument);
    }

    public ChatMessage.SystemMessage getChatMessage(Character data) {
        String definition = getPrompt();
        definition = definition.replaceAll("\\{\\{char}}", data.getName());

        return ChatMessage.SystemMessage.of(definition);
    }
}
