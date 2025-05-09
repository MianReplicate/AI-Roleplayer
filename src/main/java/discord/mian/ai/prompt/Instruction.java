package discord.mian.ai.prompt;

import discord.mian.api.Type;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

import java.util.List;
import java.util.Map;

public class Instruction extends Prompt<Instruction.InstructionType> {
    public Instruction(Map<InstructionType, List<String>> typeMap) {
        super("instruction_data",typeMap);
    }

    public ChatMessage.SystemMessage getPrompt(){
        StringBuilder content = new StringBuilder();
        for (InstructionType instructionType : typeMap.keySet()) {
            String wholeString;
            switch(instructionType){
                case InstructionType.INSTRUCTION:
                    wholeString = instructionType.getName() + "\n";
                    for (String string : typeMap.get(instructionType)) {
                        wholeString += string + "\n";
                    }
                    content.append(wholeString).append("\n");
                    break;
                case InstructionType.RULES:
                    List<String> rules = typeMap.get(instructionType);
                    wholeString = ("Important: The rules below must be followed! My life DEPENDS on it. \nRules:\n");
                    int number = 1;
                    for (String rule: rules) {
                        wholeString += number + ". "+rule+"\n";
                        number++;
                    }
                    content.append(wholeString).append("\n");
                    break;
                default:
                    throw new RuntimeException("Unknown Instruction Type: "+instructionType);
            }
        }

        return ChatMessage.SystemMessage.of(
                content.toString(),
                this.promptTypeName
        );
    }

    public String toString(){
        return this.getPrompt().getContent();
    }

    public enum InstructionType implements Type {
        INSTRUCTION("Instruction"),
        RULES("Rules");

        InstructionType(String name){
            this.name = name;
        }

        private final String name;

        @Override
        public String getName() {
            return this.name;
        }
    }
}
