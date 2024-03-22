package com.cardconnect.boltserverclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class TerminalRequest {

    private final RestRequest request;

    private final String correlationId;
}
