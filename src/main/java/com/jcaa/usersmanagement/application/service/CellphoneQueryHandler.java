package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.CellphoneQueryUseCase;
import com.jcaa.usersmanagement.application.port.out.CellphoneReadPort;
import com.jcaa.usersmanagement.application.service.dto.query.GetCellphoneByIdQuery;
import com.jcaa.usersmanagement.domain.exception.CellphoneNotFoundException;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side handler; it has no dependency on mutation ports. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CellphoneQueryHandler implements CellphoneQueryUseCase {

  private final CellphoneReadPort readPort;

  @Override
  public CellphoneModel getById(final GetCellphoneByIdQuery query) {
    return readPort.getById(query.id())
        .orElseThrow(() -> CellphoneNotFoundException.becauseIdWasNotFound(query.id()));
  }

  @Override
  public List<CellphoneModel> getAll() {
    return readPort.getAll();
  }
}
