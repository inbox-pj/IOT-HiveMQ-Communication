package com.cardconnect.boltserverclient;

import com.cardconnect.boltserverclient.mqtt.Mqtt5ClientConfigurer;
import com.cardconnect.boltserverclient.mqtt.MqttProperties;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BoltServerClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoltServerClientApplication.class, args);
    }

    @Bean
    public Mqtt5Client mqttClient(final Mqtt5ClientConfigurer clientFactory, final MqttProperties properties) {
        return clientFactory.mqttClient(properties);
    }

}
