package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.FormatoReporte;
import ec.uta.multijunta.model.TipoReporte;
import ec.uta.multijunta.service.DatosReporte;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class ReporteFormulario {

    @NotNull(message = "{campo.obligatorio}")
    private TipoReporte tipo = TipoReporte.BALANCE;

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate inicio = LocalDate.now().withDayOfMonth(1);

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fin = LocalDate.now();

    @NotNull(message = "{campo.obligatorio}")
    private FormatoReporte formato = FormatoReporte.PDF;

    public DatosReporte datos() {
        return new DatosReporte(tipo, inicio, fin, formato);
    }

    public TipoReporte getTipo() {
        return tipo;
    }

    public void setTipo(TipoReporte tipo) {
        this.tipo = tipo;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public void setInicio(LocalDate inicio) {
        this.inicio = inicio;
    }

    public LocalDate getFin() {
        return fin;
    }

    public void setFin(LocalDate fin) {
        this.fin = fin;
    }

    public FormatoReporte getFormato() {
        return formato;
    }

    public void setFormato(FormatoReporte formato) {
        this.formato = formato;
    }
}
