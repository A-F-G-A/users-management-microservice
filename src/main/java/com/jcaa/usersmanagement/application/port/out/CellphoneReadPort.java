package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import java.util.List;
import java.util.Optional;

/** Persistence contract for read-only operations. */
public interface CellphoneReadPort {
  Optional<CellphoneModel> getById(String id);
  List<CellphoneModel> getAll();
}
