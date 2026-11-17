package com.jcaa.usersmanagement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.application.port.out.CellphoneReadPort;
import com.jcaa.usersmanagement.application.port.out.CellphoneWritePort;
import com.jcaa.usersmanagement.application.service.dto.command.CreateCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UpdateCellphoneCommand;
import com.jcaa.usersmanagement.domain.exception.CellphoneNotFoundException;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CellphoneCommandHandlerTest {

  private CellphoneReadPort readPort;
  private CellphoneWritePort writePort;
  private CellphoneCommandHandler handler;

  @BeforeEach
  void setUp() {
    readPort = mock(CellphoneReadPort.class);
    writePort = mock(CellphoneWritePort.class);
    handler = new CellphoneCommandHandler(writePort, readPort);
  }

  @Test
  void shouldGenerateIdAndSaveOnCreate() {
    when(writePort.save(org.mockito.ArgumentMatchers.any())).thenAnswer(call -> call.getArgument(0));
    final CellphoneModel created = handler.create(command());
    assertThat(created.id()).isNotBlank();
    assertThat(created.imei()).isEqualTo("012345678901237");
    verify(writePort).save(created);
  }

  @Test
  void shouldRequireExistingCellphoneOnUpdate() {
    when(readPort.getById("missing")).thenReturn(Optional.empty());
    assertThrows(CellphoneNotFoundException.class, () -> handler.update(new UpdateCellphoneCommand(
        "missing", "Acme", "012345678901237", BigDecimal.valueOf(6.2), BigDecimal.TEN, 8,
        128, null, "Android", "Carrier", "5G", false, false, 2, "CPU", BigDecimal.ONE,
        false, false, false, false, 1)));
  }

  @Test
  void shouldRequireExistingCellphoneOnDelete() {
    when(readPort.getById("missing")).thenReturn(Optional.empty());
    assertThrows(CellphoneNotFoundException.class,
        () -> handler.delete(new DeleteCellphoneCommand("missing")));
  }

  private static CreateCellphoneCommand command() {
    return new CreateCellphoneCommand("Acme", "012345678901237", BigDecimal.valueOf(6.2),
        BigDecimal.TEN, 8, 128, null, "Android", "Carrier", "5G", false, false, 2, "CPU",
        BigDecimal.ONE, false, false, false, false, 1);
  }
}
