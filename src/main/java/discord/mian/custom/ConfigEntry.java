package discord.mian.custom;

public class ConfigEntry<T> {
    private String description;
    private boolean hidden = false;
    private T value;

    public ConfigEntry(){}

    public ConfigEntry(String description, boolean hidden, T value){
        this.description = description;
        this.hidden = hidden;
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setValue(T value){
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public boolean getHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
