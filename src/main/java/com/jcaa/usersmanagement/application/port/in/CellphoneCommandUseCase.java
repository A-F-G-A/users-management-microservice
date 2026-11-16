package com.jcaa.usersmanagement.application.port.in;

import com.jcaa.usersmanagement.application.service.dto.command.CreateCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UpdateCellphoneCommand;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;

/** Write-side input port: commands only. */
public interface CellphoneCommandUseCase {
  CellphoneModel create(CreateCellphoneCommand command);
  CellphoneModel update(UpdateCellphoneCommand command);
  void delete(DeleteCellphoneCommand command);
}
