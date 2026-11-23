package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.MetodoPago;
import ec.uta.multijunta.service.DatosCobro;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CobroFormulario {

    private List<String> obligaciones = new ArrayList<>();

    @NotNull(message = "{pago.montoInvalido}")
    @Digits(integer = 10, fraction = 2, message = "{monto.decimales}")
    private BigDecimal monto;

    @NotNull(message = "{campo.obligatorio}")
    private MetodoPago metodo = MetodoPago.EFECTIVO;

    private boolean confirmarParcial;

    public DatosCobro datos() {
        return new DatosCobro(obligaciones == null ? List.of() : obligaciones, monto, metodo, confirmarParcial);
    }

    public List<String> getObligaciones() {
        return obligaciones;
    }

    public void setObligaciones(List<String> obligaciones) {
        this.obligaciones = obligaciones;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPago metodo) {
        this.metodo = metodo;
    }

    public boolean isConfirmarParcial() {
        return confirmarParcial;
    }

    public void setConfirmarParcial(boolean confirmarParcial) {
        this.confirmarParcial = confirmarParcial;
    }
}
