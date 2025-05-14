package discord.mian.custom;

import java.util.LinkedHashMap;
import java.util.Map;

public class SizeHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxEntries;

    public SizeHashMap(int maxEntries) {
        super(16, 0.75f, true); // accessOrder = true
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxEntries;
    }
}
