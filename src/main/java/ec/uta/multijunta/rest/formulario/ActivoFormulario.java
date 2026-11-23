package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.CategoriaActivo;
import ec.uta.multijunta.model.EstadoActivo;
import ec.uta.multijunta.service.DatosActivo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class ActivoFormulario {

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 120)
    private String nombre;

    private CategoriaActivo categoria;

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaAdquisicion;

    @NotNull(message = "{activo.valor}")
    @DecimalMin(value = "0.01", message = "{activo.valor}")
    @Digits(integer = 10, fraction = 2, message = "{monto.decimales}")
    private BigDecimal valorAdquisicion;

    @NotNull(message = "{campo.obligatorio}")
    private EstadoActivo estado = EstadoActivo.NUEVO;

    public DatosActivo datos() {
        return new DatosActivo(nombre, categoria, fechaAdquisicion, valorAdquisicion, estado);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public CategoriaActivo getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaActivo categoria) {
        this.categoria = categoria;
    }

    public LocalDate getFechaAdquisicion() {
        return fechaAdquisicion;
    }

    public void setFechaAdquisicion(LocalDate fechaAdquisicion) {
        this.fechaAdquisicion = fechaAdquisicion;
    }

    public BigDecimal getValorAdquisicion() {
        return valorAdquisicion;
    }

    public void setValorAdquisicion(BigDecimal valorAdquisicion) {
        this.valorAdquisicion = valorAdquisicion;
    }

    public EstadoActivo getEstado() {
        return estado;
    }

    public void setEstado(EstadoActivo estado) {
        this.estado = estado;
    }
}
