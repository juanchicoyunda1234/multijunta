package ec.uta.multijunta.service;

import ec.uta.multijunta.model.TipoRecurso;
import java.math.BigDecimal;

public record DatosRecurso(String nombre, TipoRecurso tipo, Long sectorId, BigDecimal caudal) {
}
