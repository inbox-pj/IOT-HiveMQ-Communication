package com.cardconnect.boltserverclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class ReadCardResponse implements RestResponse {
    private final String merchantId;
    private final String hsn;
    private final String message;

}
