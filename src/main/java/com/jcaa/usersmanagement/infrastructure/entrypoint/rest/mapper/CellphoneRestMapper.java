package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.mapper;

import com.jcaa.usersmanagement.application.service.dto.command.CreateCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UpdateCellphoneCommand;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.CellphoneRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.CellphoneRestResponse;
import java.util.List;

public final class CellphoneRestMapper {

  private CellphoneRestMapper() {}

  public static CreateCellphoneCommand toCreateCommand(final CellphoneRestRequest r) {
    return new CreateCellphoneCommand(r.brand(), r.imei(), r.screenSize(), r.megapixels(), r.ram(),
        r.primaryStorage(), r.secondaryStorage(), r.operatingSystem(), r.operator(),
        r.bandTechnology(), r.wifi(), r.bluetooth(), r.cameras(), r.cpuBrand(), r.cpuSpeed(),
        r.nfc(), r.fingerprint(), r.infrared(), r.waterResistant(), r.simCount());
  }

  public static UpdateCellphoneCommand toUpdateCommand(final String id, final CellphoneRestRequest r) {
    return new UpdateCellphoneCommand(id, r.brand(), r.imei(), r.screenSize(), r.megapixels(),
        r.ram(), r.primaryStorage(), r.secondaryStorage(), r.operatingSystem(), r.operator(),
        r.bandTechnology(), r.wifi(), r.bluetooth(), r.cameras(), r.cpuBrand(), r.cpuSpeed(),
        r.nfc(), r.fingerprint(), r.infrared(), r.waterResistant(), r.simCount());
  }

  public static CellphoneRestResponse toResponse(final CellphoneModel c) {
    return new CellphoneRestResponse(c.id(), c.brand(), c.imei(), c.screenSize(), c.megapixels(),
        c.ram(), c.primaryStorage(), c.secondaryStorage(), c.operatingSystem(), c.operator(),
        c.bandTechnology(), c.wifi(), c.bluetooth(), c.cameras(), c.cpuBrand(), c.cpuSpeed(),
        c.nfc(), c.fingerprint(), c.infrared(), c.waterResistant(), c.simCount());
  }

  public static List<CellphoneRestResponse> toResponseList(final List<CellphoneModel> cellphones) {
    return cellphones.stream().map(CellphoneRestMapper::toResponse).toList();
  }
}
