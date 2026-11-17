package com.jcaa.usersmanagement.domain.model;

import com.jcaa.usersmanagement.domain.exception.InvalidCellphoneException;
import java.math.BigDecimal;
import lombok.Builder;

/**
 * Pure domain model for a mobile device. IMEI is kept as text to preserve leading zeros. Every
 * invariant (including lengths and numeric limits matching the cellphones table) is enforced here,
 * so no adapter can persist an invalid device. Prefer {@link #builder()} over the 21-argument
 * canonical constructor: named setters make swapped positional arguments impossible to miss.
 */
@Builder(toBuilder = true)
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

  public static final int ID_MAX_LENGTH = 36;
  public static final int TEXT_MAX_LENGTH = 100;
  public static final int IMEI_LENGTH = 15;
  public static final int DECIMAL_SCALE = 2;
  /** DECIMAL(6,2) in the cellphones table. */
  public static final BigDecimal MAX_DECIMAL_6_2 = new BigDecimal("9999.99");
  /** DECIMAL(7,2) in the cellphones table. */
  public static final BigDecimal MAX_DECIMAL_7_2 = new BigDecimal("99999.99");

  public CellphoneModel {
    requireText(id, "id", ID_MAX_LENGTH);
    requireText(brand, "marca", TEXT_MAX_LENGTH);
    requireImei(imei);
    requireDecimal(screenSize, "pulgadas", false, MAX_DECIMAL_6_2);
    requireDecimal(megapixels, "megapixeles", true, MAX_DECIMAL_7_2);
    requirePositive(ram, "ram");
    requirePositive(primaryStorage, "almacenamientoPrincipal");
    if (secondaryStorage != null && secondaryStorage < 0) {
      throw invalid("almacenamientoSecundario no puede ser negativo.");
    }
    requireText(operatingSystem, "sistemaOperativo", TEXT_MAX_LENGTH);
    requireText(operator, "operador", TEXT_MAX_LENGTH);
    requireText(bandTechnology, "tecnologiaBanda", TEXT_MAX_LENGTH);
    requirePresent(wifi, "wifi");
    requirePresent(bluetooth, "bluetooth");
    requirePositive(cameras, "camaras");
    requireText(cpuBrand, "marcaCpu", TEXT_MAX_LENGTH);
    requireDecimal(cpuSpeed, "velocidadCpu", false, MAX_DECIMAL_6_2);
    requirePresent(nfc, "nfc");
    requirePresent(fingerprint, "huella");
    requirePresent(infrared, "ir");
    requirePresent(waterResistant, "resisteAgua");
    requirePositive(simCount, "cantidadSim");
  }

  /** Luhn (ISO/IEC 7812) check used by the 15th IMEI digit. */
  static boolean hasValidLuhnCheckDigit(final String digits) {
    int sum = 0;
    for (int i = 0; i < digits.length(); i++) {
      int digit = digits.charAt(digits.length() - 1 - i) - '0';
      if (i % 2 == 1) {
        digit *= 2;
        if (digit > 9) digit -= 9;
      }
      sum += digit;
    }
    return sum % 10 == 0;
  }

  private static void requireImei(final String imei) {
    requireText(imei, "imei", IMEI_LENGTH);
    if (!imei.matches("[0-9]{" + IMEI_LENGTH + "}")) {
      throw invalid("El IMEI debe contener exactamente 15 dígitos.");
    }
    if (!hasValidLuhnCheckDigit(imei)) {
      throw invalid("El IMEI no es válido: el dígito verificador (Luhn) no coincide.");
    }
  }

  private static void requireText(final String value, final String field, final int maxLength) {
    if (value == null || value.isBlank()) throw invalid(field + " es obligatorio.");
    if (value.length() > maxLength) {
      throw invalid(field + " no puede superar " + maxLength + " caracteres.");
    }
  }

  private static void requirePresent(final Object value, final String field) {
    if (value == null) throw invalid(field + " es obligatorio.");
  }

  private static void requirePositive(final Integer value, final String field) {
    if (value == null || value <= 0) throw invalid(field + " debe ser mayor que cero.");
  }

  private static void requireDecimal(
      final BigDecimal value, final String field, final boolean zeroAllowed, final BigDecimal max) {
    if (value == null) throw invalid(field + " es obligatorio.");
    if (zeroAllowed ? value.signum() < 0 : value.signum() <= 0) {
      throw invalid(field + (zeroAllowed ? " no puede ser negativo." : " debe ser mayor que cero."));
    }
    if (value.compareTo(max) > 0) throw invalid(field + " no puede superar " + max + ".");
    if (value.stripTrailingZeros().scale() > DECIMAL_SCALE) {
      throw invalid(field + " admite como máximo " + DECIMAL_SCALE + " decimales.");
    }
  }

  private static InvalidCellphoneException invalid(final String message) {
    return InvalidCellphoneException.becauseValueIsInvalid(message);
  }
}
