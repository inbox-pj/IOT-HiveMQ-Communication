package com.cardconnect.posterminalclient.callback;

import com.cardconnect.posterminalclient.util.MQTTUtil;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class SubscribeMessages {

    private static final Logger logger = LoggerFactory.getLogger(SubscribeMessages.class);

    public static void onMessageReceived(final Mqtt5Publish publishMessage, Mqtt5Client client)  {
        logger.debug(">>>>>>>>>>>>>>>Received publish with payload: on topic: {} -- " + new String(publishMessage.getPayloadAsBytes(), StandardCharsets.UTF_8), 1122334455+"-REQ");

        MQTTUtil.publish(new String(publishMessage.getPayloadAsBytes(), StandardCharsets.UTF_8), client);

    }



}