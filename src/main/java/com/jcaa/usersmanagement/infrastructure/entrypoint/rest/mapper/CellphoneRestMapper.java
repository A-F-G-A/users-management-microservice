package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.mapper;

import com.jcaa.usersmanagement.application.service.dto.command.CreateCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UpdateCellphoneCommand;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.CellphoneRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.CellphoneRestResponse;
import java.util.List;

/** Named builders keep each of the 21 attributes bound to the right field. */
public final class CellphoneRestMapper {

  private CellphoneRestMapper() {}

  public static CreateCellphoneCommand toCreateCommand(final CellphoneRestRequest r) {
    return CreateCellphoneCommand.builder()
        .brand(r.brand()).imei(r.imei())
        .screenSize(r.screenSize()).megapixels(r.megapixels())
        .ram(r.ram()).primaryStorage(r.primaryStorage()).secondaryStorage(r.secondaryStorage())
        .operatingSystem(r.operatingSystem()).operator(r.operator())
        .bandTechnology(r.bandTechnology())
        .wifi(r.wifi()).bluetooth(r.bluetooth()).cameras(r.cameras())
        .cpuBrand(r.cpuBrand()).cpuSpeed(r.cpuSpeed())
        .nfc(r.nfc()).fingerprint(r.fingerprint()).infrared(r.infrared())
        .waterResistant(r.waterResistant()).simCount(r.simCount())
        .build();
  }

  public static UpdateCellphoneCommand toUpdateCommand(final String id, final CellphoneRestRequest r) {
    return UpdateCellphoneCommand.builder()
        .id(id)
        .brand(r.brand()).imei(r.imei())
        .screenSize(r.screenSize()).megapixels(r.megapixels())
        .ram(r.ram()).primaryStorage(r.primaryStorage()).secondaryStorage(r.secondaryStorage())
        .operatingSystem(r.operatingSystem()).operator(r.operator())
        .bandTechnology(r.bandTechnology())
        .wifi(r.wifi()).bluetooth(r.bluetooth()).cameras(r.cameras())
        .cpuBrand(r.cpuBrand()).cpuSpeed(r.cpuSpeed())
        .nfc(r.nfc()).fingerprint(r.fingerprint()).infrared(r.infrared())
        .waterResistant(r.waterResistant()).simCount(r.simCount())
        .build();
  }

  public static CellphoneRestResponse toResponse(final CellphoneModel c) {
    return CellphoneRestResponse.builder()
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

  public static List<CellphoneRestResponse> toResponseList(final List<CellphoneModel> cellphones) {
    return cellphones.stream().map(CellphoneRestMapper::toResponse).toList();
  }
}
