package com.jcaa.usersmanagement.application.port.in;

import com.jcaa.usersmanagement.application.service.dto.query.GetCellphoneByIdQuery;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import java.util.List;

/** Read-side input port: queries do not modify data. */
public interface CellphoneQueryUseCase {
  CellphoneModel getById(GetCellphoneByIdQuery query);
  List<CellphoneModel> getAll();
}
