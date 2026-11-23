package ec.uta.multijunta.service;

import ec.uta.multijunta.model.CategoriaActivo;
import ec.uta.multijunta.model.EstadoActivo;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DatosActivo(String nombre, CategoriaActivo categoria, LocalDate fechaAdquisicion,
                          BigDecimal valorAdquisicion, EstadoActivo estado) {
}
