package com.rm.sos.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Shipment {

    private String id;

    @JsonProperty("tracking_number")
    private String trackingNumber;

    @JsonProperty("reference_number")
    private String referenceNumber;

    private String status;
    private String carrier;
    private String service;
    private String priority;

    @JsonProperty("origin_address")
    private Address originAddress;

    @JsonProperty("destination_address")
    private Address destinationAddress;

    @JsonProperty("current_location")
    private GeoPoint currentLocation;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("estimated_delivery")
    private Instant estimatedDelivery;

    @JsonProperty("actual_delivery")
    private Instant actualDelivery;

    @JsonProperty("weight_kg")
    private Double weightKg;

    @JsonProperty("volume_cbm")
    private Double volumeCbm;

    @JsonProperty("package_count")
    private Integer packageCount;

    @JsonProperty("declared_value")
    private Double declaredValue;

    private String currency;

    @JsonProperty("sender_name")
    private String senderName;

    @JsonProperty("sender_id")
    private String senderId;

    @JsonProperty("recipient_name")
    private String recipientName;

    @JsonProperty("recipient_id")
    private String recipientId;

    private List<String> tags;
    private String notes;
}