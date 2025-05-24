package discord.mian.data;

public enum PromptType {
    INSTRUCTION("Instructions"), WORLD("Worlds"), CHARACTER("Characters");

    public final String displayName;

    PromptType(String displayName) {
        this.displayName = displayName;
    }
}
