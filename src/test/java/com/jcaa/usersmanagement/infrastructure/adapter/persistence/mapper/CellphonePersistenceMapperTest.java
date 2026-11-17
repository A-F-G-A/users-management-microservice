package com.jcaa.usersmanagement.infrastructure.adapter.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import java.math.BigDecimal;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;

class CellphonePersistenceMapperTest {

  @Test
  void shouldPreserveImeisAndFalseCapabilitiesFromDatabase() throws Exception {
    final ResultSet row = mock(ResultSet.class);
    when(row.getString("id")).thenReturn("cell-1");
    when(row.getString("brand")).thenReturn("Acme");
    when(row.getString("imei")).thenReturn("012345678901237");
    when(row.getBigDecimal("screen_size")).thenReturn(BigDecimal.valueOf(6.2));
    when(row.getBigDecimal("megapixels")).thenReturn(BigDecimal.valueOf(48));
    when(row.getObject("ram")).thenReturn(8);
    when(row.getObject("primary_storage")).thenReturn(128);
    when(row.getObject("secondary_storage")).thenReturn(null);
    when(row.getString("operating_system")).thenReturn("Android");
    when(row.getString("operator_name")).thenReturn("Carrier");
    when(row.getString("band_technology")).thenReturn("5G");
    when(row.getBoolean("wifi")).thenReturn(false);
    when(row.getBoolean("bluetooth")).thenReturn(false);
    when(row.getObject("cameras")).thenReturn(2);
    when(row.getString("cpu_brand")).thenReturn("Acme CPU");
    when(row.getBigDecimal("cpu_speed")).thenReturn(BigDecimal.valueOf(2.4));
    when(row.getBoolean("nfc")).thenReturn(false);
    when(row.getBoolean("fingerprint")).thenReturn(false);
    when(row.getBoolean("infrared")).thenReturn(false);
    when(row.getBoolean("water_resistant")).thenReturn(false);
    when(row.getObject("sim_count")).thenReturn(2);

    final CellphoneModel mapped = CellphonePersistenceMapper.fromResultSet(row);

    assertThat(mapped.imei()).isEqualTo("012345678901237");
    assertThat(mapped.secondaryStorage()).isNull();
    assertThat(mapped.wifi()).isFalse();
    assertThat(mapped.bluetooth()).isFalse();
    assertThat(mapped.nfc()).isFalse();
  }
}
