package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.CategoriaEgreso;
import ec.uta.multijunta.service.DatosEgreso;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class EgresoFormulario {

    @NotNull(message = "{campo.obligatorio}")
    private CategoriaEgreso categoria;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 160)
    private String concepto;

    @NotNull(message = "{monto.positivo}")
    @DecimalMin(value = "0.01", message = "{monto.positivo}")
    @Digits(integer = 10, fraction = 2, message = "{monto.decimales}")
    private BigDecimal monto;

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha = LocalDate.now();

    @Size(max = 120)
    private String proveedor;

    private boolean confirmarNegativo;

    public DatosEgreso datos() {
        return new DatosEgreso(categoria, concepto, monto, fecha, proveedor, confirmarNegativo);
    }

    public CategoriaEgreso getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaEgreso categoria) {
        this.categoria = categoria;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public boolean isConfirmarNegativo() {
        return confirmarNegativo;
    }

    public void setConfirmarNegativo(boolean confirmarNegativo) {
        this.confirmarNegativo = confirmarNegativo;
    }
}
