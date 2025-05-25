package discord.mian.data;

import org.bson.codecs.pojo.annotations.BsonId;

import java.util.HashMap;
import java.util.Map;

public class ServerConfig {
    @BsonId
    private long id;
    private Map<String, ConfigEntry<?>> entries;

    public ServerConfig() {
        entries = new HashMap<>();
    }

    public ServerConfig(long id, Map<String, ConfigEntry<?>> entries) {
        this.id = id;
        this.entries = entries;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setEntries(Map<String, ConfigEntry<?>> entries) {
        this.entries = entries;
    }

    public Map<String, ConfigEntry<?>> getEntries() {
        return entries;
    }

    public long getId() {
        return id;
    }

    public ConfigEntry<?> get(String key) {
        return entries.get(key);
    }

    public <T> ConfigEntry<T> get(String key, Class<T> generic) {
        ConfigEntry<?> entry = entries.get(key);
        if (!entry.getTypeClass().isAssignableFrom(generic))
            throw new RuntimeException(entry.getValue() + " isn't of type " + generic.getTypeName());
        return (ConfigEntry<T>) entry;
    }

    public void put(String key, ConfigEntry<?> entry) {
        entries.put(key, entry);
    }

    public void putIfAbsent(String key, ConfigEntry<?> entry) {
        entries.putIfAbsent(key, entry);
    }
}
