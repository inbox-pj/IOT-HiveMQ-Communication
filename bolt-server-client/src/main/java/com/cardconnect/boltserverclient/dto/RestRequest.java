package com.cardconnect.boltserverclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public abstract class RestRequest {
    private final String merchantId;
    private final String hsn;

}