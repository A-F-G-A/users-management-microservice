package com.jcaa.usersmanagement.application.service.dto.command;

import java.math.BigDecimal;

public record UpdateCellphoneCommand(
    String id, String brand, String imei, BigDecimal screenSize, BigDecimal megapixels, Integer ram,
    Integer primaryStorage, Integer secondaryStorage, String operatingSystem, String operator,
    String bandTechnology, Boolean wifi, Boolean bluetooth, Integer cameras, String cpuBrand,
    BigDecimal cpuSpeed, Boolean nfc, Boolean fingerprint, Boolean infrared,
    Boolean waterResistant, Integer simCount) {}
