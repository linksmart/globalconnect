package eu.linksmart.global.backbone.zmq;

import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by carlos on 24.11.14.
 */
public class Message {

    public String topic;
    public byte type;
    public long timestamp;
    public String sender;
    public byte[] payload;

    private static Logger LOG = Logger.getLogger(Message.class.getName());

    public static long deserializeTimestamp(byte[] raw){

        byte[] unixTimeSerialized = raw;
        ByteBuffer unixTime = ByteBuffer.wrap(unixTimeSerialized);
        return unixTime.getLong();

    }
    public static byte[] serializeTimestamp(){
        long timestamp = System.currentTimeMillis();
        return ByteBuffer.allocate(8).putLong(timestamp).array();
    }

    public static void printMessage(Message msg) {
        LOG.trace("*********************************************");
        LOG.trace("message topic   : " + msg.topic);
        LOG.trace("message type    : " + String.format("%02x", msg.type & 0xff));
        LOG.trace("message time    : " + msg.timestamp);
        LOG.trace("message sender  : " + msg.sender);
        LOG.trace("message payload : " + new String(msg.payload));
        LOG.trace("*********************************************");
    }

    public static boolean isUUID(String rawUUID) {
        try {
            UUID parsed = java.util.UUID.fromString(rawUUID);
            LOG.trace("parsed UUID : " + parsed.toString());
            return true;
        } catch (IllegalArgumentException ex) {
            LOG.warn("Could not parse an UUID.",ex);
            return false;
        }
    }

//    public static ZMsg serialize(Message aMessage){
//
//        ZMsg serialized = new ZMsg();
//
//        // add topic to message
//        serialized.add(aMessage.topic);
//
//        // add type to message
//        byte[] serializedType  = new byte[1];
//        serializedType[0] = aMessage.type;
//        serialized.add(serializedType);
//
//        // add timestamp to message
//        ByteBuffer unixTime = ByteBuffer.allocate(8);
//        unixTime.order(ByteOrder.BIG_ENDIAN);
//        unixTime.putLong(aMessage.timestamp);
//
//        byte[] unixTimeSerialized = new byte[unixTime.capacity()];
//        unixTime.get(unixTimeSerialized, 0, unixTimeSerialized.length);
//        serialized.add(unixTimeSerialized);
//
//        // add sender to message
//        serialized.add(aMessage.sender);
//
//        // add payload to message
//        serialized.add(aMessage.payload);
//
//        return serialized;
//
//    }
//    public static Message deserialize(ZMsg aMessage){
//
//        Message deserialized = new Message();
//
//        Iterator<ZFrame> it = aMessage.iterator();
//
//        // read topic
//        ZFrame frame = it.next();
//        deserialized.topic =  frame.getData().toString();
//
//        // read type
//        frame = it.next();
//        deserialized.type = frame.getData()[0];
//
//        // read timestamp
//        frame = it.next();
//        byte[] unixTimeSerialized = frame.getData();
//        ByteBuffer unixTime = ByteBuffer.wrap(unixTimeSerialized);
//        unixTime.order(ByteOrder.BIG_ENDIAN);
//        deserialized.timestamp = unixTime.getLong();
//
//        // read sender
//        frame = it.next();
//        deserialized.sender = frame.getData().toString();
//
//        // read payload
//
//        frame = it.next();
//        deserialized.payload = frame.getData();
//
//
//        return deserialized;
//    }
}

