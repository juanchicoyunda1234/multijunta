package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.service.DatosEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

public class EventoFormulario {

    @NotNull(message = "{campo.obligatorio}")
    private TipoEvento tipo;

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime hora;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 150)
    private String lugar;

    @Size(max = 500)
    private String descripcion;

    public DatosEvento datos() {
        return new DatosEvento(tipo, fecha, hora, lugar, descripcion);
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public void setTipo(TipoEvento tipo) {
        this.tipo = tipo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
