package ec.uta.multijunta.service;

import ec.uta.multijunta.model.TipoEvento;
import java.time.LocalDate;

public record Inasistencia(Long asistenciaId, Long eventoId, TipoEvento tipoEvento, LocalDate fecha, Long socioId,
                           String cedula, String nombreCompleto) {
}
