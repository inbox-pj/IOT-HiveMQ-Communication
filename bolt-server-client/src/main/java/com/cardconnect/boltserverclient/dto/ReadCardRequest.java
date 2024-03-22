package com.cardconnect.boltserverclient.dto;

public class ReadCardRequest extends RestRequest {

    public ReadCardRequest(String merchantId, String hsn) {
        super(merchantId, hsn);
    }
}
