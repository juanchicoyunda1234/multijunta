package ec.uta.multijunta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import java.util.Locale;

@Entity
public class Cuota extends Obligacion {

    @Column(nullable = false, length = 120)
    private String concepto;

    @Column(nullable = false, length = 120)
    private String conceptoNormalizado;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    public static String normalizar(String concepto) {
        return concepto == null ? "" : concepto.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    @Override
    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto.trim();
        this.conceptoNormalizado = normalizar(concepto);
    }

    public String getConceptoNormalizado() {
        return conceptoNormalizado;
    }

    @Override
    public LocalDate getVencimiento() {
        return fechaVencimiento;
    }

    @Override
    public String getClave() {
        return "C-" + getId();
    }

    @Override
    public String getTipo() {
        return "Cuota";
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }
}
