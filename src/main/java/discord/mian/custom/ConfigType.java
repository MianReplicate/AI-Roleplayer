package discord.mian.custom;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ConfigType {
    STRING, BOOL, INT, LONG;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
