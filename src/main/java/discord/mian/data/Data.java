package discord.mian.data;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import discord.mian.api.AIDocument;
import discord.mian.custom.Util;

import java.util.function.Consumer;

public class Data<T extends AIDocument> {
    private final Class<T> type;
    protected final T document;
    private final MongoCollection<T> collection;

    public Data(Class<T> type, T document){
        this.document = document;
        this.type = type;
        this.collection = Util.DATABASE
                .getCollection(type.getSimpleName().toLowerCase(), type);
    }

    public String getName(){
        return document.getName();
    }

    public T getDocument(){
        return document;
    }

    public String getPrompt(){
        return document.getPrompt();
    }

    public void updateDocument(Consumer<T> documentUpdater){
        documentUpdater.accept(document);
        collection.replaceOne(Filters.eq("name", document.getName()), document, new ReplaceOptions().upsert(true));
    }

    // delete document
    public void nuke(){
        collection.deleteOne(Filters.eq("name", document.getName()));
    }
}
