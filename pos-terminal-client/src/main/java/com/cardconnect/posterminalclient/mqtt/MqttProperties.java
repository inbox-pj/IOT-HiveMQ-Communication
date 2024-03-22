package com.cardconnect.posterminalclient.mqtt;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.net.URI;


@Configuration
@Getter
public class MqttProperties {

    @Value("${hivemq.client.server-uri}")
    protected URI serverUri;

    @Value("${hivemq.client.client-id}")
    protected String clientId;



    public String getServerHost() {
        return serverUri != null ? serverUri.getHost() : null;
    }

    public int getServerPort() {
        return serverUri != null ? serverUri.getPort() : -1;
    }

}