package com.cardconnect.boltserverclient.util;

import com.cardconnect.boltserverclient.dto.ReadCardResponse;
import com.cardconnect.boltserverclient.dto.TerminalRequest;
import com.cardconnect.boltserverclient.dto.TerminalResponse;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.datatypes.MqttUtf8String;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling;
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.Mqtt5Unsubscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class MQTTUtil {

    private static final Logger logger = LoggerFactory.getLogger(MQTTUtil.class);

    public static Function<TerminalRequest, CompletableFuture<Mqtt5PublishResult>> publish(TerminalRequest message, Mqtt5Client client) {
        CompletableFuture<Mqtt5PublishResult> resultFuture = new CompletableFuture<>();

        client.toAsync().connect()
                .thenAcceptAsync(connAck -> logger.debug("<<<<<<<<<<<<<<<<Successfully connected with identifier {}", client.getConfig().getClientIdentifier().get()))
                .thenComposeAsync(connAck -> {
                    logger.debug("<<<<<<<<<<<<<<<<Sending message {} to topic {}", message.toString(), message.getRequest().getHsn()+"-REQ");
                    return client.toAsync().publishWith()
                            .topic(message.getRequest().getHsn()+"-REQ")
                            .qos(MqttQos.EXACTLY_ONCE)
                            .payload(message.toString().getBytes(StandardCharsets.UTF_8))
                            .contentType(MqttUtf8String.of("text/plain"))
                            .messageExpiryInterval(120)
                            .send();
                })
                .thenApplyAsync(publishResult -> {
                    logger.debug("<<<<<<<<<<<<<<<<Successfully sent message {} to topic {}", message.toString(), message.getRequest().getHsn()+"-REQ");
                    client.toAsync().disconnect();
                    resultFuture.complete(publishResult);
                    return resultFuture.join();
                })
                .exceptionally(throwable -> {
                    logger.error("<<<<<<<<<<<<<<<<Something went wrong with message {} on topic {} with details {}", message,  message.getRequest().getHsn()+"-REQ", throwable.getMessage());
                    throw new CompletionException(throwable);
                });

        return publishResult -> resultFuture;
    }


    public static CompletableFuture<TerminalResponse> subscribe(TerminalRequest message, Mqtt5Client client) {
        if (client.getState().isConnectedOrReconnect()) {
            // Client is already connected, proceed with the subscription
            return subscribeToTopic(message, client);
        } else {
            // Client is not connected, connect first and then subscribe
            return client.toAsync().connect()
                    .thenCompose(connAck -> {
                        logger.debug(">>>>>>>>>>>>>>>>>Successfully connected with identifier {}", client.getConfig().getClientIdentifier().get());
                        return subscribeToTopic(message, client);
                    })
                    .exceptionally(throwable -> {
                        logger.error(">>>>>>>>>>>>>>>>>Failed to connect: {}", throwable.getMessage());
                        throw new CompletionException(throwable);
                    });
        }
    }

    private static CompletableFuture<TerminalResponse> subscribeToTopic(TerminalRequest message, Mqtt5Client client) {
        CompletableFuture<TerminalResponse> responseFuture = new CompletableFuture<>();

        client.toAsync().subscribeWith()
                .topicFilter(message.getRequest().getHsn() + "-RES")
                .qos(MqttQos.EXACTLY_ONCE)
                .noLocal(true)
                .retainHandling(Mqtt5RetainHandling.DO_NOT_SEND)
                .retainAsPublished(true)
                .callback(publish -> {
                    logger.debug(">>>>>>>>>>>>>>>>>Received subscribe with payload: " + new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8));

                    // Create and complete the response with the received message
                    TerminalResponse response = new TerminalResponse(message.getRequest(), new ReadCardResponse(message.getRequest().getMerchantId(), message.getRequest().getHsn(), "Message Processed"), new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8), HttpStatus.OK);
                    responseFuture.complete(response);
                })
                .send()
                .thenApply(subAck -> {
                    logger.debug(">>>>>>>>>>>>>>>>>Subscribed to topic {}", message.getRequest().getHsn() + "-RES");
                    return responseFuture; // Return the response future to the next stage
                })
                .thenCompose(future -> future) // Wait for the response to be completed
                .thenAccept(response -> {
                    // Unsubscribe from the topic after processing the response
                    client.toAsync().unsubscribe(Mqtt5Unsubscribe.builder().topicFilter(message.getRequest().getHsn() + "-RES").build())
                            .whenComplete((unsubAck, throwable) -> {
                                if (throwable != null) {
                                    logger.error(">>>>>>>>>>>>>>>>>Error unsubscribing from topic {}", message.getRequest().getHsn() + "-RES", throwable);
                                }
                                // Disconnect from the client after unsubscribing
                                client.toAsync().disconnect();
                            });
                })
                .exceptionally(throwable -> {
                    logger.error(">>>>>>>>>>>>>>>>>Something went wrong with message {} on topic {} with details {}", message, message.getRequest().getHsn() + "-RES", throwable.getMessage());
                    throw new CompletionException(throwable);
                });

        return responseFuture;
    }


}