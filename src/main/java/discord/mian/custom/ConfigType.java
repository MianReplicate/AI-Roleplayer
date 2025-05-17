package discord.mian.custom;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ConfigType {
    STRING, BOOL, INT, DOUBLE, LONG;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
