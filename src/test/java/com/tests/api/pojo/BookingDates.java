package com.tests.api.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO for Booking Dates (checkin/checkout).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingDates {

    @JsonProperty("checkin")
    private String checkin;

    @JsonProperty("checkout")
    private String checkout;

    public BookingDates() {
    }

    public BookingDates(String checkin, String checkout) {
        this.checkin = checkin;
        this.checkout = checkout;
    }

    public String getCheckin() {
        return checkin;
    }

    public void setCheckin(String checkin) {
        this.checkin = checkin;
    }

    public String getCheckout() {
        return checkout;
    }

    public void setCheckout(String checkout) {
        this.checkout = checkout;
    }
}

