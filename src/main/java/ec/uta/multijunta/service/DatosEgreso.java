package ec.uta.multijunta.service;

import ec.uta.multijunta.model.CategoriaEgreso;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DatosEgreso(CategoriaEgreso categoria, String concepto, BigDecimal monto, LocalDate fecha,
                          String proveedor, boolean confirmarNegativo) {
}
