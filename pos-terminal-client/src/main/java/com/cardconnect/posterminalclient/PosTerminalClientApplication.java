package com.cardconnect.posterminalclient;

import com.cardconnect.posterminalclient.mqtt.Mqtt5ClientConfigurer;
import com.cardconnect.posterminalclient.mqtt.MqttProperties;
import com.cardconnect.posterminalclient.util.MQTTUtil;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PosTerminalClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(PosTerminalClientApplication.class, args);
    }

    @Bean
    public Mqtt5Client mqttClient(final Mqtt5ClientConfigurer clientFactory, final MqttProperties properties) {
        Mqtt5Client client = clientFactory.mqttClient(properties);

        MQTTUtil.subscribe(client);
        return client;
    }

}
