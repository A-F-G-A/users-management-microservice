package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record CellphoneRestResponse(
    String id, @JsonProperty("marca") String brand, String imei,
    @JsonProperty("pulgadas") BigDecimal screenSize,
    @JsonProperty("megapixeles") BigDecimal megapixels, Integer ram,
    @JsonProperty("almacenamientoPrincipal") Integer primaryStorage,
    @JsonProperty("almacenamientoSecundario") Integer secondaryStorage,
    @JsonProperty("sistemaOperativo") String operatingSystem,
    @JsonProperty("operador") String operator,
    @JsonProperty("tecnologiaBanda") String bandTechnology, Boolean wifi, Boolean bluetooth,
    @JsonProperty("camaras") Integer cameras, @JsonProperty("marcaCpu") String cpuBrand,
    @JsonProperty("velocidadCpu") BigDecimal cpuSpeed, Boolean nfc,
    @JsonProperty("huella") Boolean fingerprint, @JsonProperty("ir") Boolean infrared,
    @JsonProperty("resisteAgua") Boolean waterResistant,
    @JsonProperty("cantidadSim") Integer simCount) {}
