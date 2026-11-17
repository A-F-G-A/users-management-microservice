package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import com.jcaa.usersmanagement.application.port.in.CellphoneCommandUseCase;
import com.jcaa.usersmanagement.application.port.in.CellphoneQueryUseCase;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteCellphoneCommand;
import com.jcaa.usersmanagement.application.service.dto.query.GetCellphoneByIdQuery;
import com.jcaa.usersmanagement.infrastructure.config.OpenApiConfig;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.CellphoneRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.ApiErrorResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.CellphoneRestResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.mapper.CellphoneRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cellphones")
@RequiredArgsConstructor
@Tag(name = "Celulares", description = "CRUDL de celulares. Lecturas para ADMIN/REVIEWER; escrituras para ADMIN.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CellphoneRestController {

  private final CellphoneCommandUseCase commandUseCase;
  private final CellphoneQueryUseCase queryUseCase;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Crear celular", description = "Requiere rol ADMIN. El IMEI debe ser único.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Celular creado."),
    @ApiResponse(responseCode = "400", description = "Datos inválidos."),
    @ApiResponse(responseCode = "401", description = "No autenticado."),
    @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN."),
    @ApiResponse(responseCode = "409", description = "IMEI duplicado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public CellphoneRestResponse create(@Valid @RequestBody final CellphoneRestRequest request) {
    return CellphoneRestMapper.toResponse(commandUseCase.create(CellphoneRestMapper.toCreateCommand(request)));
  }

  @GetMapping
  @Operation(summary = "Listar celulares", description = "Requiere rol ADMIN o REVIEWER.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de celulares."),
    @ApiResponse(responseCode = "401", description = "No autenticado."),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente.")
  })
  public List<CellphoneRestResponse> getAll() {
    return CellphoneRestMapper.toResponseList(queryUseCase.getAll());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar celular", description = "Requiere rol ADMIN o REVIEWER.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Celular encontrado."),
    @ApiResponse(responseCode = "401", description = "No autenticado."),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente."),
    @ApiResponse(responseCode = "404", description = "Celular inexistente.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public CellphoneRestResponse getById(@PathVariable final String id) {
    return CellphoneRestMapper.toResponse(queryUseCase.getById(new GetCellphoneByIdQuery(id)));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Actualizar celular", description = "Requiere rol ADMIN. El IMEI debe ser único.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Celular actualizado."),
    @ApiResponse(responseCode = "400", description = "Datos inválidos."),
    @ApiResponse(responseCode = "401", description = "No autenticado."),
    @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN."),
    @ApiResponse(responseCode = "404", description = "Celular inexistente."),
    @ApiResponse(responseCode = "409", description = "IMEI duplicado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public CellphoneRestResponse update(@PathVariable final String id,
      @Valid @RequestBody final CellphoneRestRequest request) {
    return CellphoneRestMapper.toResponse(commandUseCase.update(CellphoneRestMapper.toUpdateCommand(id, request)));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Eliminar celular", description = "Requiere rol ADMIN.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Celular eliminado."),
    @ApiResponse(responseCode = "401", description = "No autenticado."),
    @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN."),
    @ApiResponse(responseCode = "404", description = "Celular inexistente.")
  })
  public void delete(@PathVariable final String id) {
    commandUseCase.delete(new DeleteCellphoneCommand(id));
  }
}
