package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.CellphoneCommandUseCase;
import com.jcaa.usersmanagement.application.port.out.CellphoneWritePort;
import com.jcaa.usersmanagement.application.service.dto.command.CreateCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UpdateCellphoneCommand;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles commands and owns transaction boundaries for the cellphone write model. Existence is
 * checked by the write adapter from the affected-row count (UPDATE/DELETE ... WHERE id = ?), so no
 * extra SELECT is issued and there is no check-then-act race.
 */
@Service
@RequiredArgsConstructor
public class CellphoneCommandHandler implements CellphoneCommandUseCase {

  private final CellphoneWritePort writePort;

  @Override
  @Transactional
  public CellphoneModel create(final CreateCellphoneCommand command) {
    return writePort.save(toModel(command));
  }

  @Override
  @Transactional
  public CellphoneModel update(final UpdateCellphoneCommand command) {
    return writePort.update(toModel(command));
  }

  @Override
  @Transactional
  public void delete(final DeleteCellphoneCommand command) {
    writePort.delete(command.id());
  }

  private static CellphoneModel toModel(final CreateCellphoneCommand c) {
    return CellphoneModel.builder()
        .id(UUID.randomUUID().toString())
        .brand(c.brand()).imei(c.imei())
        .screenSize(c.screenSize()).megapixels(c.megapixels())
        .ram(c.ram()).primaryStorage(c.primaryStorage()).secondaryStorage(c.secondaryStorage())
        .operatingSystem(c.operatingSystem()).operator(c.operator())
        .bandTechnology(c.bandTechnology())
        .wifi(c.wifi()).bluetooth(c.bluetooth()).cameras(c.cameras())
        .cpuBrand(c.cpuBrand()).cpuSpeed(c.cpuSpeed())
        .nfc(c.nfc()).fingerprint(c.fingerprint()).infrared(c.infrared())
        .waterResistant(c.waterResistant()).simCount(c.simCount())
        .build();
  }

  private static CellphoneModel toModel(final UpdateCellphoneCommand c) {
    return CellphoneModel.builder()
        .id(c.id())
        .brand(c.brand()).imei(c.imei())
        .screenSize(c.screenSize()).megapixels(c.megapixels())
        .ram(c.ram()).primaryStorage(c.primaryStorage()).secondaryStorage(c.secondaryStorage())
        .operatingSystem(c.operatingSystem()).operator(c.operator())
        .bandTechnology(c.bandTechnology())
        .wifi(c.wifi()).bluetooth(c.bluetooth()).cameras(c.cameras())
        .cpuBrand(c.cpuBrand()).cpuSpeed(c.cpuSpeed())
        .nfc(c.nfc()).fingerprint(c.fingerprint()).infrared(c.infrared())
        .waterResistant(c.waterResistant()).simCount(c.simCount())
        .build();
  }
}
