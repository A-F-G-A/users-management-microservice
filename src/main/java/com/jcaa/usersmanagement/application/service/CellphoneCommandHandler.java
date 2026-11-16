package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.CellphoneCommandUseCase;
import com.jcaa.usersmanagement.application.port.out.CellphoneReadPort;
import com.jcaa.usersmanagement.application.port.out.CellphoneWritePort;
import com.jcaa.usersmanagement.application.service.dto.command.CreateCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UpdateCellphoneCommand;
import com.jcaa.usersmanagement.domain.exception.CellphoneNotFoundException;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles commands and owns transaction boundaries for the cellphone write model. */
@Service
@RequiredArgsConstructor
public class CellphoneCommandHandler implements CellphoneCommandUseCase {

  private final CellphoneWritePort writePort;
  private final CellphoneReadPort readPort;

  @Override
  @Transactional
  public CellphoneModel create(final CreateCellphoneCommand command) {
    return writePort.save(toModel(UUID.randomUUID().toString(), command));
  }

  @Override
  @Transactional
  public CellphoneModel update(final UpdateCellphoneCommand command) {
    readPort.getById(command.id())
        .orElseThrow(() -> CellphoneNotFoundException.becauseIdWasNotFound(command.id()));
    return writePort.update(toModel(command));
  }

  @Override
  @Transactional
  public void delete(final DeleteCellphoneCommand command) {
    readPort.getById(command.id())
        .orElseThrow(() -> CellphoneNotFoundException.becauseIdWasNotFound(command.id()));
    writePort.delete(command.id());
  }

  private static CellphoneModel toModel(final String id, final CreateCellphoneCommand c) {
    return new CellphoneModel(id, c.brand(), c.imei(), c.screenSize(), c.megapixels(), c.ram(),
        c.primaryStorage(), c.secondaryStorage(), c.operatingSystem(), c.operator(),
        c.bandTechnology(), c.wifi(), c.bluetooth(), c.cameras(), c.cpuBrand(), c.cpuSpeed(),
        c.nfc(), c.fingerprint(), c.infrared(), c.waterResistant(), c.simCount());
  }

  private static CellphoneModel toModel(final UpdateCellphoneCommand c) {
    return new CellphoneModel(c.id(), c.brand(), c.imei(), c.screenSize(), c.megapixels(), c.ram(),
        c.primaryStorage(), c.secondaryStorage(), c.operatingSystem(), c.operator(),
        c.bandTechnology(), c.wifi(), c.bluetooth(), c.cameras(), c.cpuBrand(), c.cpuSpeed(),
        c.nfc(), c.fingerprint(), c.infrared(), c.waterResistant(), c.simCount());
  }
}
