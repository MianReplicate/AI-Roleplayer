package discord.mian.loxi.common.ai.prompt;

public enum PromptType {
    INSTRUCTION("Instruction"), CHARACTER("Character"), WORLD("World");

    PromptType(final String name){
        this.name = name;
    }

    private final String name;

    public String getName() {
        return this.name;
    }
}
