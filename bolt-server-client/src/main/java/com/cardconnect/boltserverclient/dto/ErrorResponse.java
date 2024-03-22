package com.cardconnect.boltserverclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class ErrorResponse implements RestResponse {

    private final int errorCode;
    private final String errorMessage;

}
