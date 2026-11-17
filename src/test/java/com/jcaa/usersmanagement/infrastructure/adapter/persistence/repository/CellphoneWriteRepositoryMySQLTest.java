package com.jcaa.usersmanagement.infrastructure.adapter.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.domain.exception.CellphoneAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.CellphoneNotFoundException;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Unit-level contract checks; real SQL behaviour is covered by CellphoneMySqlIT. */
class CellphoneWriteRepositoryMySQLTest {

  private static final String IMEI_DUPLICATE =
      "Duplicate entry '012345678901237' for key 'cellphones.uq_cellphones_imei'";

  @Test
  void shouldReturnCreatedCellphoneAfterInsert() {
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final CellphoneModel cellphone = validCellphone();
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

    final CellphoneModel saved = new CellphoneWriteRepositoryMySQL(jdbc).save(cellphone);

    assertThat(saved).isEqualTo(cellphone);
  }

  @Test
  void shouldTranslateUniqueKeyViolationToDomainConflict() {
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.update(anyString(), any(Object[].class)))
        .thenThrow(new DuplicateKeyException(IMEI_DUPLICATE));

    assertThrows(CellphoneAlreadyExistsException.class,
        () -> new CellphoneWriteRepositoryMySQL(jdbc).save(validCellphone()));
  }

  @Test
  void shouldTranslateImeiViolationOnUpdate() {
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.update(anyString(), any(Object[].class)))
        .thenThrow(new DuplicateKeyException(IMEI_DUPLICATE));

    assertThrows(CellphoneAlreadyExistsException.class,
        () -> new CellphoneWriteRepositoryMySQL(jdbc).update(validCellphone()));
  }

  @Test
  void shouldNotReportOtherDuplicatesAsImeiConflicts() {
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.update(anyString(), any(Object[].class)))
        .thenThrow(new DuplicateKeyException("Duplicate entry 'cell-1' for key 'cellphones.PRIMARY'"));

    assertThrows(DuplicateKeyException.class,
        () -> new CellphoneWriteRepositoryMySQL(jdbc).save(validCellphone()));
  }

  @Test
  void shouldReportMissingRowsAsNotFound() {
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    final CellphoneWriteRepositoryMySQL repository = new CellphoneWriteRepositoryMySQL(jdbc);

    assertThrows(CellphoneNotFoundException.class, () -> repository.update(validCellphone()));
    assertThrows(CellphoneNotFoundException.class, () -> repository.delete("missing"));
  }

  private static CellphoneModel validCellphone() {
    return new CellphoneModel("cell-1", "Acme", "012345678901237", BigDecimal.valueOf(6.2),
        BigDecimal.valueOf(48), 8, 128, null, "Android", "Carrier", "5G", false, false,
        2, "Acme CPU", BigDecimal.valueOf(2.4), false, false, false, false, 2);
  }
}
