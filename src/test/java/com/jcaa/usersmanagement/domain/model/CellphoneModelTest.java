package com.jcaa.usersmanagement.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jcaa.usersmanagement.domain.exception.DomainException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CellphoneModelTest {

  @Test
  void shouldKeepFalseCapabilitiesAndLeadingZeroImei() {
    final CellphoneModel cellphone = validCellphone("012345678901234");
    assertThat(cellphone.imei()).isEqualTo("012345678901234");
    assertThat(cellphone.wifi()).isFalse();
    assertThat(cellphone.bluetooth()).isFalse();
  }

  @Test
  void shouldRejectImeiThatIsNotFifteenDigits() {
    assertThrows(DomainException.class, () -> validCellphone("1234"));
  }

  @Test
  void shouldRejectNegativeTechnicalValues() {
    assertThrows(DomainException.class, () -> new CellphoneModel("id", "brand", "012345678901234",
        BigDecimal.valueOf(-1), BigDecimal.ZERO, 4, 64, null, "OS", "operator", "5G", true,
        true, 2, "CPU", BigDecimal.ONE, false, false, false, false, 1));
  }

  static CellphoneModel validCellphone(final String imei) {
    return new CellphoneModel("cellphone-id", "Acme", imei, BigDecimal.valueOf(6.2),
        BigDecimal.valueOf(48), 8, 128, null, "Android", "Carrier", "5G", false, false,
        3, "Acme CPU", BigDecimal.valueOf(2.4), false, false, false, false, 2);
  }
}
