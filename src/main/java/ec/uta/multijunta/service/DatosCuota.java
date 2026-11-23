package ec.uta.multijunta.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DatosCuota(DestinoCuota destino, Long sectorId, Long socioId, String concepto, BigDecimal monto,
                         LocalDate fechaVencimiento) {
}
