package com.cardconnect.posterminalclient.util;

import com.cardconnect.posterminalclient.callback.SubscribeMessages;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.datatypes.MqttUtf8String;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Component
public class MQTTUtil {

    private static final Logger logger = LoggerFactory.getLogger(MQTTUtil.class);

    public static void publish(String message, Mqtt5Client client) {
        if (!client.getState().isConnectedOrReconnect()) {
            client.toAsync().connect()
                    .thenAccept(connAck -> {
                        logger.debug("<<<<<<<<<<<<<<<<<<<<<<<<Successfully connected with identifier {}", client.getConfig().getClientIdentifier().get());
                        publishMessage(client, message);
                    })
                    .exceptionally(throwable -> {
                        logger.error("<<<<<<<<<<<<<<<<<<<<<<<<Failed to connect on topic {} for subscribing# {}", 1122334455+"-RES", throwable.getMessage());
                        throw new CompletionException(throwable);
                    });
        } else {
            logger.debug("<<<<<<<<<<<<<<<<<<<<<<<<Client is already connected, skipping connection attempt.");
            publishMessage(client, message);
        }
    }

    private static void publishMessage(Mqtt5Client client, String message) {
        CompletableFuture<Mqtt5PublishResult> resultFuture = new CompletableFuture<>();
        client.toAsync().publishWith()
                .topic(1122334455+"-RES")
                .qos(MqttQos.EXACTLY_ONCE)
                .payload(message.getBytes(StandardCharsets.UTF_8))
                .contentType(MqttUtf8String.of("text/plain"))
                .messageExpiryInterval(120)
                .send()
                .thenAccept(publishResult -> {
                    logger.debug("<<<<<<<<<<<<<<Successfully sent message {} to topic {}", message, 1122334455+"-RES");
                    //client.toAsync().disconnect();
                    resultFuture.complete(publishResult);
                })
                .exceptionally(throwable -> {
                    logger.error("<<<<<<<<<<<<<<<Failed to publish message:{} on topic {}, details {}", message, 1122334455+"-RES", throwable.getMessage());
                    throw new CompletionException(throwable);
                });
    }



    public static void subscribe(Mqtt5Client client) {
        client.toAsync().connect()
                .thenRun(() -> {
                    logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>Successfully connected with identifier {}", client.getConfig().getClientIdentifier().get());
                    client.toAsync().subscribeWith()
                            .topicFilter(1122334455+"-REQ")
                            .qos(MqttQos.EXACTLY_ONCE)
                            .noLocal(true)
                            .retainHandling(Mqtt5RetainHandling.DO_NOT_SEND)
                            .retainAsPublished(true)
                            .callback(s -> SubscribeMessages.onMessageReceived(s, client))
                            .send();
                })
                .exceptionally(throwable -> {
                    logger.error(">>>>>>>>>>>>>>>>>>>>>>>>Failed to connect on topic {} for subscribing# {}", 1122334455+"-REQ", throwable.getMessage());
                    throw new CompletionException(throwable);
                });

    }

}