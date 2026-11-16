package com.jcaa.usersmanagement.domain.model;

import com.jcaa.usersmanagement.domain.exception.InvalidCellphoneException;
import java.math.BigDecimal;

/** Pure domain model for a mobile device. IMEI is kept as text to preserve its representation. */
public record CellphoneModel(
    String id,
    String brand,
    String imei,
    BigDecimal screenSize,
    BigDecimal megapixels,
    Integer ram,
    Integer primaryStorage,
    Integer secondaryStorage,
    String operatingSystem,
    String operator,
    String bandTechnology,
    Boolean wifi,
    Boolean bluetooth,
    Integer cameras,
    String cpuBrand,
    BigDecimal cpuSpeed,
    Boolean nfc,
    Boolean fingerprint,
    Boolean infrared,
    Boolean waterResistant,
    Integer simCount) {

  public CellphoneModel {
    requireText(id, "id");
    requireText(brand, "marca");
    requireText(imei, "imei");
    if (!imei.matches("[0-9]{15}")) {
      throw InvalidCellphoneException.becauseValueIsInvalid("El IMEI debe contener exactamente 15 dígitos.");
    }
    requirePositive(screenSize, "pulgadas");
    requireNonNegative(megapixels, "megapixeles");
    requirePositive(ram, "ram");
    requirePositive(primaryStorage, "almacenamientoPrincipal");
    if (secondaryStorage != null) requireNonNegative(secondaryStorage, "almacenamientoSecundario");
    requireText(operatingSystem, "sistemaOperativo");
    requireText(operator, "operador");
    requireText(bandTechnology, "tecnologiaBanda");
    requirePresent(wifi, "wifi");
    requirePresent(bluetooth, "bluetooth");
    requirePositive(cameras, "camaras");
    requireText(cpuBrand, "marcaCpu");
    requirePositive(cpuSpeed, "velocidadCpu");
    requirePresent(nfc, "nfc");
    requirePresent(fingerprint, "huella");
    requirePresent(infrared, "ir");
    requirePresent(waterResistant, "resisteAgua");
    requirePositive(simCount, "cantidadSim");
  }

  private static void requireText(final String value, final String field) {
    if (value == null || value.isBlank()) throw InvalidCellphoneException.becauseValueIsInvalid(field + " es obligatorio.");
  }

  private static void requirePresent(final Object value, final String field) {
    if (value == null) throw InvalidCellphoneException.becauseValueIsInvalid(field + " es obligatorio.");
  }

  private static void requirePositive(final BigDecimal value, final String field) {
    if (value == null || value.signum() <= 0) throw InvalidCellphoneException.becauseValueIsInvalid(field + " debe ser mayor que cero.");
  }

  private static void requirePositive(final Integer value, final String field) {
    if (value == null || value <= 0) throw InvalidCellphoneException.becauseValueIsInvalid(field + " debe ser mayor que cero.");
  }

  private static void requireNonNegative(final BigDecimal value, final String field) {
    if (value == null || value.signum() < 0) throw InvalidCellphoneException.becauseValueIsInvalid(field + " no puede ser negativo.");
  }

  private static void requireNonNegative(final Integer value, final String field) {
    if (value < 0) throw InvalidCellphoneException.becauseValueIsInvalid(field + " no puede ser negativo.");
  }
}
