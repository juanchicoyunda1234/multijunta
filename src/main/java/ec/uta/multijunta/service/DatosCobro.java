package ec.uta.multijunta.service;

import ec.uta.multijunta.model.MetodoPago;
import java.math.BigDecimal;
import java.util.List;

public record DatosCobro(List<String> obligaciones, BigDecimal monto, MetodoPago metodo, boolean confirmarParcial) {
}
