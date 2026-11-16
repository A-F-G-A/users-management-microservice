package com.jcaa.usersmanagement.infrastructure.adapter.persistence.mapper;

import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Maps JDBC rows to the domain without introducing persistence concerns into the domain. */
public final class CellphonePersistenceMapper {

  private CellphonePersistenceMapper() {}

  public static CellphoneModel fromResultSet(final ResultSet rs) throws SQLException {
    return new CellphoneModel(
        rs.getString("id"), rs.getString("brand"), rs.getString("imei"),
        rs.getBigDecimal("screen_size"), rs.getBigDecimal("megapixels"),
        (Integer) rs.getObject("ram"), (Integer) rs.getObject("primary_storage"),
        (Integer) rs.getObject("secondary_storage"), rs.getString("operating_system"),
        rs.getString("operator_name"), rs.getString("band_technology"),
        rs.getBoolean("wifi"), rs.getBoolean("bluetooth"),
        (Integer) rs.getObject("cameras"), rs.getString("cpu_brand"),
        rs.getBigDecimal("cpu_speed"), rs.getBoolean("nfc"),
        rs.getBoolean("fingerprint"), rs.getBoolean("infrared"),
        rs.getBoolean("water_resistant"), (Integer) rs.getObject("sim_count"));
  }
}
