package com.jcaa.usersmanagement.infrastructure.adapter.persistence.repository;

import com.jcaa.usersmanagement.application.port.out.CellphoneReadPort;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.mapper.CellphonePersistenceMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** JDBC read adapter separated from the write adapter for CQRS. */
@Repository
@RequiredArgsConstructor
public class CellphoneReadRepositoryMySQL implements CellphoneReadPort {

  private static final String COLUMNS = "id, brand, imei, screen_size, megapixels, ram, "
      + "primary_storage, secondary_storage, operating_system, operator_name, band_technology, "
      + "wifi, bluetooth, cameras, cpu_brand, cpu_speed, nfc, fingerprint, infrared, "
      + "water_resistant, sim_count";
  private final JdbcTemplate jdbc;

  @Override
  public Optional<CellphoneModel> getById(final String id) {
    return jdbc.query("SELECT " + COLUMNS + " FROM cellphones WHERE id = ?", rs ->
        rs.next() ? Optional.of(CellphonePersistenceMapper.fromResultSet(rs)) : Optional.empty(), id);
  }

  @Override
  public List<CellphoneModel> getAll() {
    return jdbc.query("SELECT " + COLUMNS + " FROM cellphones ORDER BY brand, id",
        (rs, rowNum) -> CellphonePersistenceMapper.fromResultSet(rs));
  }
}
