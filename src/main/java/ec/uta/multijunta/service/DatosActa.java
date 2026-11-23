package ec.uta.multijunta.service;

import ec.uta.multijunta.model.TipoEvento;
import java.time.LocalDate;

public record DatosActa(String titulo, LocalDate fecha, TipoEvento tipoEvento, String contenido,
                        boolean publicaSocios) {
}
