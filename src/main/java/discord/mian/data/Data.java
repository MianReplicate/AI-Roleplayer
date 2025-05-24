package discord.mian.data;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import discord.mian.api.AIDocument;
import discord.mian.custom.Util;

public class Data<T extends AIDocument> {
    private final Class<T> type;
    private final T document;

    public Data(Class<T> type, T document){
        this.document = document;
        this.type = type;
    }

    public T getDocument(){
        return document;
    }

    // run every time changes are made
    public void saveDocument(){
        MongoCollection<T> collection = Util.DATABASE
                .getCollection("prompts", type);

        collection.replaceOne(Filters.eq("name", document.getName()), document);
    }
}
