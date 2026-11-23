package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.TipoMovimiento;
import ec.uta.multijunta.service.DatosMovimiento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class MovimientoFormulario {

    @NotNull(message = "{campo.obligatorio}")
    private Long itemId;

    @NotNull(message = "{campo.obligatorio}")
    private TipoMovimiento tipo = TipoMovimiento.ENTRADA;

    @NotNull(message = "{inventario.cantidad}")
    @DecimalMin(value = "0.01", message = "{inventario.cantidad}")
    @Digits(integer = 10, fraction = 2, message = "{monto.decimales}")
    private BigDecimal cantidad;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 160)
    private String motivo;

    public DatosMovimiento datos() {
        return new DatosMovimiento(itemId, tipo, cantidad, motivo);
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
