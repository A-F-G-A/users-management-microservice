package com.jcaa.usersmanagement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.application.port.out.CellphoneWritePort;
import com.jcaa.usersmanagement.application.service.dto.command.CreateCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UpdateCellphoneCommand;
import com.jcaa.usersmanagement.domain.exception.CellphoneNotFoundException;
import com.jcaa.usersmanagement.domain.exception.InvalidCellphoneException;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CellphoneCommandHandlerTest {

  private CellphoneWritePort writePort;
  private CellphoneCommandHandler handler;

  @BeforeEach
  void setUp() {
    writePort = mock(CellphoneWritePort.class);
    handler = new CellphoneCommandHandler(writePort);
  }

  @Test
  void shouldGenerateIdAndSaveOnCreate() {
    when(writePort.save(any())).thenAnswer(call -> call.getArgument(0));
    final CellphoneModel created = handler.create(command());
    assertThat(UUID.fromString(created.id())).isNotNull();
    assertThat(created.imei()).isEqualTo("012345678901237");
    verify(writePort).save(created);
  }

  @Test
  void shouldMapEveryCommandFieldToTheModel() {
    final ArgumentCaptor<CellphoneModel> captor = ArgumentCaptor.forClass(CellphoneModel.class);
    when(writePort.update(captor.capture())).thenAnswer(call -> call.getArgument(0));

    handler.update(new UpdateCellphoneCommand(
        "cell-1", "Acme", "012345678901237", new BigDecimal("6.20"), new BigDecimal("48.00"), 8,
        128, 256, "Android", "Carrier", "5G", true, false, 3, "CPU", new BigDecimal("2.40"),
        true, false, true, false, 2));

    final CellphoneModel m = captor.getValue();
    assertThat(m.id()).isEqualTo("cell-1");
    assertThat(m.brand()).isEqualTo("Acme");
    assertThat(m.operatingSystem()).isEqualTo("Android");
    assertThat(m.operator()).isEqualTo("Carrier");
    assertThat(m.bandTechnology()).isEqualTo("5G");
    assertThat(m.cpuBrand()).isEqualTo("CPU");
    assertThat(m.screenSize()).isEqualByComparingTo("6.20");
    assertThat(m.megapixels()).isEqualByComparingTo("48");
    assertThat(m.cpuSpeed()).isEqualByComparingTo("2.4");
    assertThat(m.ram()).isEqualTo(8);
    assertThat(m.primaryStorage()).isEqualTo(128);
    assertThat(m.secondaryStorage()).isEqualTo(256);
    assertThat(m.cameras()).isEqualTo(3);
    assertThat(m.simCount()).isEqualTo(2);
    assertThat(m.wifi()).isTrue();
    assertThat(m.bluetooth()).isFalse();
    assertThat(m.nfc()).isTrue();
    assertThat(m.fingerprint()).isFalse();
    assertThat(m.infrared()).isTrue();
    assertThat(m.waterResistant()).isFalse();
  }

  @Test
  void shouldPropagateNotFoundOnUpdate() {
    when(writePort.update(any()))
        .thenThrow(CellphoneNotFoundException.becauseIdWasNotFound("missing"));
    assertThrows(CellphoneNotFoundException.class, () -> handler.update(new UpdateCellphoneCommand(
        "missing", "Acme", "012345678901237", BigDecimal.valueOf(6.2), BigDecimal.TEN, 8,
        128, null, "Android", "Carrier", "5G", false, false, 2, "CPU", BigDecimal.ONE,
        false, false, false, false, 1)));
  }

  @Test
  void shouldPropagateNotFoundOnDelete() {
    doThrow(CellphoneNotFoundException.becauseIdWasNotFound("missing"))
        .when(writePort).delete("missing");
    assertThrows(CellphoneNotFoundException.class,
        () -> handler.delete(new DeleteCellphoneCommand("missing")));
  }

  @Test
  void shouldRejectInvalidImeiBeforeReachingPersistence() {
    final CreateCellphoneCommand invalid = command().toBuilder().imei("012345678901234").build();
    assertThrows(InvalidCellphoneException.class, () -> handler.create(invalid));
    verifyNoInteractions(writePort);
  }

  private static CreateCellphoneCommand command() {
    return new CreateCellphoneCommand("Acme", "012345678901237", BigDecimal.valueOf(6.2),
        BigDecimal.TEN, 8, 128, null, "Android", "Carrier", "5G", false, false, 2, "CPU",
        BigDecimal.ONE, false, false, false, false, 1);
  }
}
