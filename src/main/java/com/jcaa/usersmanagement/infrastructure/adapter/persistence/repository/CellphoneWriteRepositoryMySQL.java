package com.jcaa.usersmanagement.infrastructure.adapter.persistence.repository;

import com.jcaa.usersmanagement.application.port.out.CellphoneWritePort;
import com.jcaa.usersmanagement.domain.exception.CellphoneAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.CellphoneNotFoundException;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC write adapter. All values are bound parameters; no input is concatenated into SQL.
 *
 * <p>UPDATE relies on MySQL Connector/J reporting matched rows (useAffectedRows=false, the
 * default), so updating a row with identical values still returns 1 and 0 means "not found".
 */
@Repository
@RequiredArgsConstructor
public class CellphoneWriteRepositoryMySQL implements CellphoneWritePort {

  /** Name of the unique constraint declared in V2__create_cellphones.sql. */
  static final String IMEI_UNIQUE_CONSTRAINT = "uq_cellphones_imei";

  private static final String INSERT = """
      INSERT INTO cellphones (id, brand, imei, screen_size, megapixels, ram, primary_storage,
      secondary_storage, operating_system, operator_name, band_technology, wifi, bluetooth,
      cameras, cpu_brand, cpu_speed, nfc, fingerprint, infrared, water_resistant, sim_count)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;
  private static final String UPDATE = """
      UPDATE cellphones SET brand=?, imei=?, screen_size=?, megapixels=?, ram=?, primary_storage=?,
      secondary_storage=?, operating_system=?, operator_name=?, band_technology=?, wifi=?,
      bluetooth=?, cameras=?, cpu_brand=?, cpu_speed=?, nfc=?, fingerprint=?, infrared=?,
      water_resistant=?, sim_count=? WHERE id=?
      """;
  private static final String DELETE = "DELETE FROM cellphones WHERE id = ?";

  private final JdbcTemplate jdbc;

  @Override
  public CellphoneModel save(final CellphoneModel c) {
    try {
      jdbc.update(INSERT, c.id(), c.brand(), c.imei(), c.screenSize(), c.megapixels(), c.ram(),
          c.primaryStorage(), c.secondaryStorage(), c.operatingSystem(), c.operator(),
          c.bandTechnology(), c.wifi(), c.bluetooth(), c.cameras(), c.cpuBrand(), c.cpuSpeed(),
          c.nfc(), c.fingerprint(), c.infrared(), c.waterResistant(), c.simCount());
      return c;
    } catch (final DuplicateKeyException exception) {
      throw translateDuplicate(exception, c);
    }
  }

  @Override
  public CellphoneModel update(final CellphoneModel c) {
    final int rows;
    try {
      rows = jdbc.update(UPDATE, c.brand(), c.imei(), c.screenSize(), c.megapixels(), c.ram(),
          c.primaryStorage(), c.secondaryStorage(), c.operatingSystem(), c.operator(),
          c.bandTechnology(), c.wifi(), c.bluetooth(), c.cameras(), c.cpuBrand(), c.cpuSpeed(),
          c.nfc(), c.fingerprint(), c.infrared(), c.waterResistant(), c.simCount(), c.id());
    } catch (final DuplicateKeyException exception) {
      throw translateDuplicate(exception, c);
    }
    if (rows == 0) throw CellphoneNotFoundException.becauseIdWasNotFound(c.id());
    return c;
  }

  @Override
  public void delete(final String id) {
    if (jdbc.update(DELETE, id) == 0) {
      throw CellphoneNotFoundException.becauseIdWasNotFound(id);
    }
  }

  /** Only the IMEI constraint is a domain conflict; any other duplicate (e.g. PK) is rethrown. */
  private static RuntimeException translateDuplicate(
      final DuplicateKeyException exception, final CellphoneModel c) {
    final String detail = exception.getMostSpecificCause().getMessage();
    if (detail != null && detail.contains(IMEI_UNIQUE_CONSTRAINT)) {
      return CellphoneAlreadyExistsException.becauseImeiAlreadyExists(c.imei());
    }
    return exception;
  }
}
