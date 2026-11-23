package ec.uta.multijunta.service;

import ec.uta.multijunta.model.PrioridadConvocatoria;
import ec.uta.multijunta.model.TipoConvocatoria;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DatosConvocatoria(String titulo, TipoConvocatoria tipo, LocalDate fechaEvento, LocalTime hora,
                                String lugar, String contenido, PrioridadConvocatoria prioridad,
                                LocalDateTime fechaPublicacion) {
}
