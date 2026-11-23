package ec.uta.multijunta.service;

import ec.uta.multijunta.model.EstadoAsistencia;

public record AsistenciaCorregida(Long juntaId, Long asistenciaId, EstadoAsistencia estadoAnterior,
                                  EstadoAsistencia estadoNuevo) {
}
