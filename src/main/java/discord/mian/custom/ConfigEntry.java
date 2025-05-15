package discord.mian.custom;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type", // whether it is an string or a bool
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConfigEntry.StringConfig.class, name = "string"), // check if type is string
        @JsonSubTypes.Type(value = ConfigEntry.BoolConfig.class, name = "bool"), // check if type is bool
        @JsonSubTypes.Type(value = ConfigEntry.IntConfig.class, name = "int"),
        @JsonSubTypes.Type(value = ConfigEntry.LongConfig.class, name = "long")
})
public abstract class ConfigEntry {
    protected ConfigType type;
    public String description;
    public boolean hidden = false;

    public ConfigType getType() {
        return type;
    }

    public static class StringConfig extends ConfigEntry{
        public String value;

        public StringConfig(){
            type = ConfigType.STRING;
        }
    }

    public static class BoolConfig extends ConfigEntry{
        public boolean value;

        public BoolConfig(){
            type = ConfigType.BOOL;
        }
    }

    public static class IntConfig extends ConfigEntry{
        public int value;

        public IntConfig(){
            type = ConfigType.INT;
        }
    }

    public static class LongConfig extends ConfigEntry{
        public long value;

        public LongConfig(){
            type = ConfigType.LONG;
        }
    }
}
