package ec.uta.multijunta.service;

import ec.uta.multijunta.model.TipoEvento;
import java.time.LocalDate;
import java.time.LocalTime;

public record DatosEvento(TipoEvento tipo, LocalDate fecha, LocalTime hora, String lugar, String descripcion) {
}
