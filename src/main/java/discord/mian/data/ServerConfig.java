package discord.mian.data;

import discord.mian.custom.ConfigEntry;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.HashMap;
import java.util.Map;

public class ServerConfig {
    @BsonId
    private long id;
    private Map<String, ConfigEntry<?>> entries;

    public ServerConfig(){
        entries = new HashMap<>();
    }

    public ServerConfig(long id, Map<String, ConfigEntry<?>> entries){
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

    public <T>ConfigEntry<T> get(String key, Class<T> generic){
        ConfigEntry<?> entry = entries.get(key);
        Object value = entry.getValue();
        if(value != null && !generic.isInstance(value))
            throw new RuntimeException("Value isn't of type "+ generic.getSimpleName());
        return (ConfigEntry<T>) entry;
    }

    public void put(String key, ConfigEntry<?> entry){
        entries.put(key, entry);
    }

    public void putIfAbsent(String key, ConfigEntry<?> entry){
        entries.putIfAbsent(key, entry);
    }
}
