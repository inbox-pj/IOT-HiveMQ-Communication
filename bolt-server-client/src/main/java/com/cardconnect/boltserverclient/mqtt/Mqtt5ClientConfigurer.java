package com.cardconnect.boltserverclient.mqtt;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientTransportConfig;
import com.hivemq.client.mqtt.MqttClientTransportConfigBuilder;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public final class Mqtt5ClientConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(Mqtt5ClientConfigurer.class);

    public Mqtt5Client mqttClient(final MqttProperties configuration) {

        Mqtt5Client client = MqttClient.builder()
                .useMqttVersion5()
                .transportConfig(buildTransportConfig(configuration))
                .identifier(configuration.getClientId())
                .automaticReconnect(null)
                .addConnectedListener(context -> LOG.debug("MQTT client (re-)connected to broker '{}:{}'.", configuration.getServerHost(), configuration.getServerPort()))
                .addDisconnectedListener(context -> LOG.debug("MQTT client disconnected.", context.getCause()))
                .build();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Connecting client {} to {} on port {}", client.getConfig().getClientIdentifier().get(), configuration.getServerHost(), configuration.getServerPort());
        }

        return client;
    }

    private MqttClientTransportConfig buildTransportConfig(final MqttProperties configuration) {

        final MqttClientTransportConfigBuilder transportConfigBuilder = MqttClientTransportConfig.builder()
                .serverHost(configuration.getServerHost())
                .serverPort(configuration.getServerPort())
                .mqttConnectTimeout(MqttClientTransportConfig.DEFAULT_MQTT_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        return transportConfigBuilder.build();
    }
}