package discord.mian.custom;

public enum PromptType {
    CHARACTER("Characters"), INSTRUCTION("Instructions"), WORLD("Worlds");

    public final String displayName;

    PromptType(String displayName){
        this.displayName = displayName;
    }
}
