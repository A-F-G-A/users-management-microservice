package com.jcaa.usersmanagement.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jcaa.usersmanagement.domain.exception.DomainException;
import com.jcaa.usersmanagement.domain.exception.InvalidCellphoneException;
import java.math.BigDecimal;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class CellphoneModelTest {

  private static final String LONG_101 = "x".repeat(101);

  @Test
  void shouldKeepFalseCapabilitiesAndLeadingZeroImei() {
    final CellphoneModel cellphone = validCellphone("012345678901237");
    assertThat(cellphone.imei()).isEqualTo("012345678901237");
    assertThat(cellphone.wifi()).isFalse();
    assertThat(cellphone.bluetooth()).isFalse();
  }

  @Test
  void shouldRejectImeiThatIsNotFifteenDigits() {
    assertThrows(DomainException.class, () -> validCellphone("1234"));
  }

  @Test
  void shouldRejectNegativeTechnicalValues() {
    assertThrows(DomainException.class, () -> new CellphoneModel("id", "brand", "012345678901237",
        BigDecimal.valueOf(-1), BigDecimal.ZERO, 4, 64, null, "OS", "operator", "5G", true,
        true, 2, "CPU", BigDecimal.ONE, false, false, false, false, 1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"490154203237518", "356938035643809", "012345678901237", "860123456789014"})
  void shouldAcceptImeisWithValidLuhnCheckDigit(final String imei) {
    assertThat(validCellphone(imei).imei()).isEqualTo(imei);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "490154203237519", "012345678901234", "35693803564380", "3569380356438090",
    "35693803564380A", " 356938035643809", "３５６９３８０３５６４３８０９"
  })
  void shouldRejectInvalidImeis(final String imei) {
    assertThrows(InvalidCellphoneException.class, () -> validCellphone(imei));
  }

  @Test
  void luhnShouldMatchKnownVectors() {
    assertThat(CellphoneModel.hasValidLuhnCheckDigit("79927398713")).isTrue();
    assertThat(CellphoneModel.hasValidLuhnCheckDigit("79927398710")).isFalse();
  }

  static Stream<Arguments> invalidFieldValues() {
    return Stream.<Arguments>of(
        // id
        arg("id null", b -> b.id(null)),
        arg("id blank", b -> b.id(" ")),
        arg("id > 36", b -> b.id("x".repeat(37))),
        // text attributes: required and max 100 characters
        arg("marca null", b -> b.brand(null)),
        arg("marca blank", b -> b.brand("")),
        arg("marca > 100", b -> b.brand(LONG_101)),
        arg("imei null", b -> b.imei(null)),
        arg("sistemaOperativo blank", b -> b.operatingSystem(" ")),
        arg("sistemaOperativo > 100", b -> b.operatingSystem(LONG_101)),
        arg("operador null", b -> b.operator(null)),
        arg("operador > 100", b -> b.operator(LONG_101)),
        arg("tecnologiaBanda blank", b -> b.bandTechnology("")),
        arg("tecnologiaBanda > 100", b -> b.bandTechnology(LONG_101)),
        arg("marcaCpu null", b -> b.cpuBrand(null)),
        arg("marcaCpu > 100", b -> b.cpuBrand(LONG_101)),
        // decimals matching DECIMAL(6,2) / DECIMAL(7,2)
        arg("pulgadas null", b -> b.screenSize(null)),
        arg("pulgadas zero", b -> b.screenSize(BigDecimal.ZERO)),
        arg("pulgadas > 9999.99", b -> b.screenSize(new BigDecimal("10000"))),
        arg("pulgadas 3 decimals", b -> b.screenSize(new BigDecimal("6.123"))),
        arg("megapixeles null", b -> b.megapixels(null)),
        arg("megapixeles negative", b -> b.megapixels(new BigDecimal("-0.01"))),
        arg("megapixeles > 99999.99", b -> b.megapixels(new BigDecimal("100000"))),
        arg("megapixeles 3 decimals", b -> b.megapixels(new BigDecimal("12.345"))),
        arg("velocidadCpu null", b -> b.cpuSpeed(null)),
        arg("velocidadCpu zero", b -> b.cpuSpeed(BigDecimal.ZERO)),
        arg("velocidadCpu > 9999.99", b -> b.cpuSpeed(new BigDecimal("10000.00"))),
        arg("velocidadCpu 3 decimals", b -> b.cpuSpeed(new BigDecimal("2.555"))),
        // integers
        arg("ram null", b -> b.ram(null)),
        arg("ram zero", b -> b.ram(0)),
        arg("almacenamientoPrincipal null", b -> b.primaryStorage(null)),
        arg("almacenamientoPrincipal negative", b -> b.primaryStorage(-1)),
        arg("almacenamientoSecundario negative", b -> b.secondaryStorage(-1)),
        arg("camaras null", b -> b.cameras(null)),
        arg("camaras zero", b -> b.cameras(0)),
        arg("cantidadSim null", b -> b.simCount(null)),
        arg("cantidadSim zero", b -> b.simCount(0)),
        // booleans are mandatory: false is valid, null is not
        arg("wifi null", b -> b.wifi(null)),
        arg("bluetooth null", b -> b.bluetooth(null)),
        arg("nfc null", b -> b.nfc(null)),
        arg("huella null", b -> b.fingerprint(null)),
        arg("ir null", b -> b.infrared(null)),
        arg("resisteAgua null", b -> b.waterResistant(null)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidFieldValues")
  void shouldRejectEachInvalidAttribute(
      final String description, final UnaryOperator<CellphoneModel.CellphoneModelBuilder> change) {
    assertThrows(InvalidCellphoneException.class, () -> change.apply(validBuilder()).build());
  }

  @Test
  void shouldAcceptBoundaryValues() {
    assertThatCode(() -> validBuilder()
        .id("x".repeat(36))
        .brand("x".repeat(100))
        .screenSize(new BigDecimal("9999.99"))
        .megapixels(BigDecimal.ZERO)
        .cpuSpeed(new BigDecimal("0.01"))
        .secondaryStorage(0)
        .build()).doesNotThrowAnyException();
    assertThat(validBuilder().secondaryStorage(null).build().secondaryStorage()).isNull();
    assertThat(validBuilder().screenSize(new BigDecimal("6.100")).build().screenSize())
        .isEqualByComparingTo("6.1");
  }

  private static Arguments arg(
      final String description, final UnaryOperator<CellphoneModel.CellphoneModelBuilder> change) {
    return Arguments.of(description, change);
  }

  private static CellphoneModel.CellphoneModelBuilder validBuilder() {
    return validCellphone("356938035643809").toBuilder();
  }

  static CellphoneModel validCellphone(final String imei) {
    return new CellphoneModel("cellphone-id", "Acme", imei, BigDecimal.valueOf(6.2),
        BigDecimal.valueOf(48), 8, 128, null, "Android", "Carrier", "5G", false, false,
        3, "Acme CPU", BigDecimal.valueOf(2.4), false, false, false, false, 2);
  }
}
