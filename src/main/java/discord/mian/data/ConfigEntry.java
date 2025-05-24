package discord.mian.data;

public class ConfigEntry<T> {
    private String type;
    private String description;
    private boolean hidden = false;
    private T value;

    public ConfigEntry(Class<T> clazz){
        this.type = clazz.getTypeName();
    }

    public ConfigEntry(String description, boolean hidden, T value, Class<T> clazz){
        this(clazz);
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

    public static <R> ConfigEntry<R> toType(ConfigEntry<?> entry, Class<R> type){
        return (ConfigEntry<R>) entry;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getType(){
        return type;
    }

    public Class<T> getTypeClass() {
        Class<T> clazz;
        try{
            clazz = (Class<T>) Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return clazz;
    }

    public boolean getHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
