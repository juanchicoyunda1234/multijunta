package ec.uta.multijunta.service;

import ec.uta.multijunta.model.FormatoReporte;
import ec.uta.multijunta.model.TipoReporte;
import java.time.LocalDate;

public record DatosReporte(TipoReporte tipo, LocalDate inicio, LocalDate fin, FormatoReporte formato) {
}
