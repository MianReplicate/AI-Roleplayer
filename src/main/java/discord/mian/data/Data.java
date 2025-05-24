package discord.mian.data;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import discord.mian.custom.Util;

import java.util.function.Consumer;

public class Data<T extends AIDocument> {
    private final Class<T> type;
    protected final T document;
    private final MongoCollection<T> collection;

    public Data(Class<T> type, T document){
        if(document == null)
            throw new RuntimeException("Document cannot be null!");
        if(document.getName() == null)
            throw new RuntimeException("Document needs a name!");
        this.document = document;
        this.type = type;
        this.collection = Util.DATABASE
                .getCollection("prompt", type);
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

    public void updateDocument(Consumer<T> documentUpdater) throws MongoException {
        documentUpdater.accept(document);
        collection.replaceOne(Filters.and(
                Filters.eq("_id", document.getName()),
                Filters.eq("server", document.getServer())
        ), document, new ReplaceOptions().upsert(true));
    }

    // delete document
    public void nuke() throws MongoException{
        collection.deleteOne(Filters.and(
                Filters.eq("_id", document.getName()),
                Filters.eq("server", document.getServer())
        ));
    }
}
