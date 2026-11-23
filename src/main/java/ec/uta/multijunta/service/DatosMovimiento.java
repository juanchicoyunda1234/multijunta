package ec.uta.multijunta.service;

import ec.uta.multijunta.model.TipoMovimiento;
import java.math.BigDecimal;

public record DatosMovimiento(Long itemId, TipoMovimiento tipo, BigDecimal cantidad, String motivo) {
}
