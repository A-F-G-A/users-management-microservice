package com.jcaa.usersmanagement.infrastructure.adapter.persistence.mapper;

import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Maps JDBC rows to the domain without introducing persistence concerns into the domain. */
public final class CellphonePersistenceMapper {

  private CellphonePersistenceMapper() {}

  public static CellphoneModel fromResultSet(final ResultSet rs) throws SQLException {
    return CellphoneModel.builder()
        .id(rs.getString("id"))
        .brand(rs.getString("brand"))
        .imei(rs.getString("imei"))
        .screenSize(rs.getBigDecimal("screen_size"))
        .megapixels(rs.getBigDecimal("megapixels"))
        .ram((Integer) rs.getObject("ram"))
        .primaryStorage((Integer) rs.getObject("primary_storage"))
        .secondaryStorage((Integer) rs.getObject("secondary_storage"))
        .operatingSystem(rs.getString("operating_system"))
        .operator(rs.getString("operator_name"))
        .bandTechnology(rs.getString("band_technology"))
        .wifi(rs.getBoolean("wifi"))
        .bluetooth(rs.getBoolean("bluetooth"))
        .cameras((Integer) rs.getObject("cameras"))
        .cpuBrand(rs.getString("cpu_brand"))
        .cpuSpeed(rs.getBigDecimal("cpu_speed"))
        .nfc(rs.getBoolean("nfc"))
        .fingerprint(rs.getBoolean("fingerprint"))
        .infrared(rs.getBoolean("infrared"))
        .waterResistant(rs.getBoolean("water_resistant"))
        .simCount((Integer) rs.getObject("sim_count"))
        .build();
  }
}
