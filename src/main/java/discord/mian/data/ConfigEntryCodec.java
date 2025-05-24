package discord.mian.data;

import discord.mian.Constants;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;

public class ConfigEntryCodec implements Codec<ConfigEntry<?>> {
    @Override
    public void encode(BsonWriter bsonWriter, ConfigEntry<?> configEntry, EncoderContext encoderContext) {
        bsonWriter.writeStartDocument();
        bsonWriter.writeString("type", configEntry.getType());
        bsonWriter.writeString("description", configEntry.getDescription());
        bsonWriter.writeBoolean("hidden", configEntry.getHidden());

        bsonWriter.writeName("value");
        Object value = configEntry.getValue();
        if(value == null){
            bsonWriter.writeNull();
        } else {
            Codec<Object> codec = (Codec<Object>) CodecRegistries.fromProviders(
                    new ValueCodecProvider()
            ).get(value.getClass());
            codec.encode(bsonWriter, value, encoderContext);
        }

        bsonWriter.writeEndDocument();
    }

    @Override
    public ConfigEntry<?> decode(BsonReader bsonReader, DecoderContext decoderContext) {
        bsonReader.readStartDocument();

        String type = bsonReader.readString("type");
        String desc = bsonReader.readString("description");
        boolean hidden = bsonReader.readBoolean("hidden");

        if(type == null)
            throw new RuntimeException("Unknown type: " + type);

        ConfigEntry<?> entry;

        try{
            Class<?> clazz = Class.forName(type);
            Codec<Object> codec = (Codec<Object>) CodecRegistries.fromProviders(
                    new ValueCodecProvider()
            ).get(clazz);
            entry = new ConfigEntry<>(clazz);
            entry.setDescription(desc);
            entry.setHidden(hidden);

            if(bsonReader.readBsonType() == BsonType.NULL){
                bsonReader.readNull();
                entry.setValue(null);
            } else {
                Object value = codec.decode(bsonReader, decoderContext);
                ConfigEntry.toType(entry, Object.class).setValue(value);
            }
        }catch(ClassNotFoundException e){
            throw new RuntimeException("Unknown class: " + type, e);
        }

        bsonReader.readEndDocument();
        return entry;
    }

    @Override
    public Class<ConfigEntry<?>> getEncoderClass() {
        return (Class<ConfigEntry<?>>)(Class<?>) ConfigEntry.class;
    }
}
