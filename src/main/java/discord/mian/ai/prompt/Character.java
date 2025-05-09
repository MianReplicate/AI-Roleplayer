package discord.mian.ai.prompt;

import discord.mian.api.Type;
import io.github.sashirestela.openai.domain.chat.ChatMessage;

import java.util.List;
import java.util.Map;

public class Character extends Prompt<Character.CharacteristicType> {
    public Character(Map<CharacteristicType, List<String>> characteristicMap) {
        super("character_data",characteristicMap);
    }

    public ChatMessage.SystemMessage getPrompt(){
        StringBuilder content = new StringBuilder("\n{\n");
        for (CharacteristicType characteristicType : this.typeMap.keySet()) {
            String wholeString = characteristicType.getName() + "(";
            for (String string : this.typeMap.get(characteristicType)) {
                wholeString += "\""+string+"\"" + "+";
            }
            wholeString = wholeString.substring(0, wholeString.length() - 1) + ")";
            content.append(wholeString).append("\n");
        }

        content.append("}");
        return ChatMessage.SystemMessage.of(
                content.toString(),
                this.promptTypeName
        );
    }

    public String getName(){
        return getType(CharacteristicType.ALIASES).getFirst();
    }

    public String toString(){
        return this.getPrompt().getContent();
    }

    public enum CharacteristicType implements Type {
        ALIASES("Aliases"),
        BIRTHDAY("Birthday"),
        OCCUPATION("Occupation"),
        AGE("Age"),
        LIKES("Likes"),
        DISLIKES("Dislikes"),
        IDENTITY("Identity"),
        SEXUALITY("Sexuality"),
        APPEARANCE("Appearance"),
        BODY("Body"),
        MIND("Mind"),
        ATTRIBUTES("Attributes"),
        PERSONALITY("Personality"),
        HABITS("Habits"),
        BACKSTORY("Backstory"),
        SPECIES("Species"),
        HEIGHT("Height"),
        SKILLS("Skills");

        CharacteristicType(String name){
            this.name = name;
        }

        private final String name;

        @Override
        public String getName() {
            return this.name;
        }
    }
}
