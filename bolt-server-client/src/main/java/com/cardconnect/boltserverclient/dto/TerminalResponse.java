package com.cardconnect.boltserverclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
@ToString
public class TerminalResponse {

    private final RestRequest request;
    private final RestResponse response;
    private final String correlationId;
    private final HttpStatus httpStatus;

    public boolean isError() {
        return response instanceof ErrorResponse;
    }
}
