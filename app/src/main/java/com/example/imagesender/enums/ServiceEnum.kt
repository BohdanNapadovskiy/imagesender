package com.example.imagesender.enums;

public enum ServiceEnum {
    NUS_SERVICE_UUID("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"),
    HTTPS_SERVER("https://navlink.net"),
    NUS_TX_CHARACTERISTIC_UUID("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"),
    MAC_ADDRESS_BLUETOOTH(""),
    NAME_BLUETOOTH("My_Device");

    public String value;

    private ServiceEnum(String value) {
        this.value = value;
    }
}
