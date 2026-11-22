package ec.uta.multijunta.service;

import java.time.LocalDate;
import java.time.LocalTime;

public record DatosTurno(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String responsable, Long sectorId,
                         Long recursoId) {
}
