package com.cardconnect.boltserverclient.controller;

import com.cardconnect.boltserverclient.dto.EmptyResponse;
import com.cardconnect.boltserverclient.dto.ErrorResponse;
import com.cardconnect.boltserverclient.dto.ReadCardRequest;
import com.cardconnect.boltserverclient.dto.ReadCardResponse;
import com.cardconnect.boltserverclient.dto.RestRequest;
import com.cardconnect.boltserverclient.dto.RestResponse;
import com.cardconnect.boltserverclient.dto.TerminalRequest;
import com.cardconnect.boltserverclient.dto.TerminalResponse;
import com.cardconnect.boltserverclient.util.Constants;
import com.cardconnect.boltserverclient.util.MQTTUtil;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/tips/v1")
public class ServerController {

    private static final Logger logger = LoggerFactory.getLogger(ServerController.class);

    private Mqtt5Client client;

    public ServerController(Mqtt5Client client) {
        this.client = client;
    }

    @PostMapping("/readcard")
    public CompletableFuture<ResponseEntity<RestResponse>> readCard(@RequestBody ReadCardRequest readCardRequest, @RequestHeader HttpHeaders headers) throws Exception {
        logger.debug(">>>>>>>>>>>>>>INCOMING REST Message Received -->" + readCardRequest.toString());
        return sendTerminalRequest(readCardRequest, headers, this::createRestResponse);
    }

    private ResponseEntity<RestResponse> createRestResponse(TerminalResponse response, Throwable throwable) {

        HttpStatus status = response != null ? response.getHttpStatus() : null;

        if (throwable == null && response != null && !response.isError()) {
            return new ResponseEntity<>(response.getResponse(), status != null ? status : OK);

        } else if (throwable == null && response != null) {
            return new ResponseEntity<>(response.getResponse(), status != null ? status : INTERNAL_SERVER_ERROR);

        } else if (throwable != null) {
            logger.error("processing response with exception: ", throwable);
            Throwable t = throwable;
            if (throwable instanceof CompletionException) {
                t = t.getCause();
            }
            ErrorResponse errorResponse = new ErrorResponse(Constants.GENERIC_ERROR, t.getMessage());
            return new ResponseEntity<>(errorResponse, status != null ? status : INTERNAL_SERVER_ERROR);

        } else {
            return new ResponseEntity<>(new EmptyResponse(), INTERNAL_SERVER_ERROR);
        }
    }

    private CompletableFuture<ResponseEntity<RestResponse>> sendTerminalRequest(RestRequest request, HttpHeaders headers, ResponseFunction responseFunction) {
        return sendMessage(request, responseFunction)
                .thenApplyAsync((ResponseEntity<RestResponse> responseEntity) -> {
                    ZonedDateTime expTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("Asia/Kolkata"));
                    HttpHeaders respHeaders = new HttpHeaders();
                    respHeaders.putAll(responseEntity.getHeaders());
                    respHeaders.putAll(headers);
                    respHeaders.put(Constants.SESSION_KEY_HEADER, singletonList(expTime.format(ISO_OFFSET_DATE_TIME)));
                    return new ResponseEntity<>(responseEntity.getBody(), respHeaders, responseEntity.getStatusCode());
                });
    }

    private CompletableFuture<ResponseEntity<RestResponse>> sendMessage(RestRequest restRequest,
                                                                        ResponseFunction responseFunction) {
        return sendMessage(restRequest)
                .handleAsync(responseFunction);
    }


    public CompletableFuture<TerminalResponse> sendMessage(RestRequest restRequest) {
        String correlationId = UUID.randomUUID().toString();

        TerminalRequest terminalRequest = new TerminalRequest(restRequest, correlationId);

        return validateRequest(terminalRequest)
                .apply(terminalRequest)
                .thenComposeAsync(MQTTUtil.publish(terminalRequest, client))
                .thenComposeAsync(prepareResponse(terminalRequest));
    }

    private Function<Mqtt5PublishResult, CompletionStage<TerminalResponse>> prepareResponse(TerminalRequest terminalRequest) {
        return publishResult -> MQTTUtil.subscribe(terminalRequest, client);

    }

    private Function<TerminalRequest, CompletableFuture<TerminalRequest>> validateRequest(TerminalRequest terminalRequest) {
        return data -> {
            if (terminalRequest.getRequest().getHsn() == null) {
                throw new CompletionException("Invalid hsn", new Exception());
            }

            return completedFuture(data);
        };
    }

    @FunctionalInterface
    public interface ResponseFunction
            extends BiFunction<TerminalResponse, Throwable, ResponseEntity<RestResponse>> {
    }


}
