package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.service.DatosTurno;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

public class TurnoFormulario {

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaFin;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 100)
    private String responsable;

    @NotNull(message = "{campo.obligatorio}")
    private Long sectorId;

    @NotNull(message = "{campo.obligatorio}")
    private Long recursoId;

    public DatosTurno datos() {
        return new DatosTurno(fecha, horaInicio, horaFin, responsable, sectorId, recursoId);
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }

    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(Long recursoId) {
        this.recursoId = recursoId;
    }
}
