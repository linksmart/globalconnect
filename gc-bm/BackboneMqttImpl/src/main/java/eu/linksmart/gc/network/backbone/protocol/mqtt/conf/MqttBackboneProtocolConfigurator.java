package eu.linksmart.gc.network.backbone.protocol.mqtt.conf;

import org.apache.log4j.Logger;

/**/
public class MqttBackboneProtocolConfigurator {
	
    private Logger LOG = Logger.getLogger(MqttBackboneProtocolConfigurator.class.getName());

	public static String BACKBONE_PID = "eu.linksmart.gc.network.backbone.protocol.mqtt";
	public static String CONFIGURATION_FILE = "etc/linksmart/MQTTBackboneProtocol.cfg";//"/mqttprotocol.properties";

	public static final String BACKBONE_DESCRIPTION = "BackboneMQTT.description";

    public static final String SECURITY_PARAMETERS = "backbone.mqtt.SecurityParameters";

    public static final String BROKER_AS_SERVICE = "BackboneMQTT.BrokerAsService";

    public static final String QoS = "BackboneMQTT.MQTT.QoS";

    public static final String PERSISTENCE = "BackboneMQTT.MQTT.Persistence";

    public static final String BROADCAST_TOPIC = "BackboneMQTT.Broadcast.Topic";

    public static final String ALLOWED_LOCAL_MESSAGING_LOOP = "BackboneMQTT.AllowingLocalLoop";

    public static final String SUBSCRIBE_TO = "BackboneMQTT.mapping.SubscribeTo";
    public static String PUBLISH_TO = "BackboneMQTT.mapping.PublishTo";
    public static String UNSUBSCRIBE_TO = "BackboneMQTT.mapping.UnsubscribeTo";
  //  public static String LIST = "BackboneMQTT.mapping.List";

    public static String MESSAGE_CONTROL_CLEANER_TIMEOUT = "BackboneMQTT.Timeout";

    public static String MESSAGE_REPETITION_CONTROL = "BackboneMQTT.advance.MessageRepetitionControl";

    public static String BROKER_NAME = "BackboneMQTT.MQTT.BrokerName";

    public static String BROKER_PORT = "BackboneMQTT.MQTT.BrokerPort";

    public static final String BROADCAST_PROPAGATION ="BackboneMQTT.Broadcast.PropagationFeature";
 
}
