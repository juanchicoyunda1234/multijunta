package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.service.DatosItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ItemFormulario {

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 120)
    private String nombre;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 30)
    private String unidadMedida;

    @NotNull(message = "{campo.obligatorio}")
    @DecimalMin(value = "0", message = "{inventario.stockMinimo}")
    @Digits(integer = 10, fraction = 2, message = "{monto.decimales}")
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    public DatosItem datos() {
        return new DatosItem(nombre, unidadMedida, stockMinimo);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }
}
