package ec.uta.multijunta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "asistencia_id"))
public class Multa extends Obligacion {

    @Column(name = "asistencia_id", nullable = false)
    private Long asistenciaId;

    @Column(nullable = false)
    private Long eventoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoEvento tipoEvento;

    @Column(nullable = false)
    private LocalDate fechaEvento;

    @Column(nullable = false, length = 160)
    private String motivo;

    @Override
    public String getConcepto() {
        return motivo;
    }

    @Override
    public LocalDate getVencimiento() {
        return null;
    }

    @Override
    public String getClave() {
        return "M-" + getId();
    }

    @Override
    public String getTipo() {
        return "Multa";
    }

    public Long getAsistenciaId() {
        return asistenciaId;
    }

    public void setAsistenciaId(Long asistenciaId) {
        this.asistenciaId = asistenciaId;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEvento tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public LocalDate getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(LocalDate fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
