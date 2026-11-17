package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.CellphoneModel;

/** Persistence contract for mutations. */
public interface CellphoneWritePort {
  CellphoneModel save(CellphoneModel cellphone);
  CellphoneModel update(CellphoneModel cellphone);
  void delete(String id);
}
