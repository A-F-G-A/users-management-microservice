package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CellphoneRestRequest(
    @JsonProperty("marca") @NotBlank @Size(max = 100) String brand,
    @JsonProperty("imei") @NotBlank @Pattern(regexp = "[0-9]{15}", message = "imei debe contener exactamente 15 dígitos") String imei,
    @JsonProperty("pulgadas") @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 4, fraction = 2) BigDecimal screenSize,
    @JsonProperty("megapixeles") @NotNull @PositiveOrZero @Digits(integer = 5, fraction = 2) BigDecimal megapixels,
    @NotNull @Positive Integer ram,
    @JsonProperty("almacenamientoPrincipal") @NotNull @Positive Integer primaryStorage,
    @JsonProperty("almacenamientoSecundario") @PositiveOrZero Integer secondaryStorage,
    @JsonProperty("sistemaOperativo") @NotBlank @Size(max = 100) String operatingSystem,
    @JsonProperty("operador") @NotBlank @Size(max = 100) String operator,
    @JsonProperty("tecnologiaBanda") @NotBlank @Size(max = 100) String bandTechnology,
    @NotNull Boolean wifi,
    @NotNull Boolean bluetooth,
    @JsonProperty("camaras") @NotNull @Positive Integer cameras,
    @JsonProperty("marcaCpu") @NotBlank @Size(max = 100) String cpuBrand,
    @JsonProperty("velocidadCpu") @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 4, fraction = 2) BigDecimal cpuSpeed,
    @NotNull Boolean nfc,
    @JsonProperty("huella") @NotNull Boolean fingerprint,
    @JsonProperty("ir") @NotNull Boolean infrared,
    @JsonProperty("resisteAgua") @NotNull Boolean waterResistant,
    @JsonProperty("cantidadSim") @NotNull @Positive Integer simCount) {}
