package ec.uta.multijunta.service;

import ec.uta.multijunta.model.TipoEvento;
import java.time.LocalDate;

public record EventoSinCerrar(Long eventoId, TipoEvento tipoEvento, LocalDate fecha, String lugar) {
}
