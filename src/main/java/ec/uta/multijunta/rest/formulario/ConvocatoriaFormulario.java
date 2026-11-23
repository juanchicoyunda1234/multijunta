package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.Convocatoria;
import ec.uta.multijunta.model.PrioridadConvocatoria;
import ec.uta.multijunta.model.TipoConvocatoria;
import ec.uta.multijunta.service.DatosConvocatoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

public class ConvocatoriaFormulario {

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 150)
    private String titulo;

    @NotNull(message = "{campo.obligatorio}")
    private TipoConvocatoria tipo = TipoConvocatoria.ASAMBLEA;

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaEvento;

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime hora;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 150)
    private String lugar;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 4000)
    private String contenido;

    @NotNull(message = "{campo.obligatorio}")
    private PrioridadConvocatoria prioridad = PrioridadConvocatoria.URGENTE;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME, fallbackPatterns = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime fechaPublicacion;

    public static ConvocatoriaFormulario desde(Convocatoria convocatoria) {
        ConvocatoriaFormulario formulario = new ConvocatoriaFormulario();
        formulario.titulo = convocatoria.getTitulo();
        formulario.tipo = convocatoria.getTipo();
        formulario.fechaEvento = convocatoria.getFechaEvento();
        formulario.hora = convocatoria.getHora();
        formulario.lugar = convocatoria.getLugar();
        formulario.contenido = convocatoria.getContenido();
        formulario.prioridad = convocatoria.getPrioridad();
        formulario.fechaPublicacion = convocatoria.getFechaPublicacion();
        return formulario;
    }

    public DatosConvocatoria datos() {
        return new DatosConvocatoria(titulo, tipo, fechaEvento, hora, lugar, contenido, prioridad, fechaPublicacion);
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public TipoConvocatoria getTipo() {
        return tipo;
    }

    public void setTipo(TipoConvocatoria tipo) {
        this.tipo = tipo;
    }

    public LocalDate getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(LocalDate fechaEvento) {
        this.fechaEvento = fechaEvento;
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

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public PrioridadConvocatoria getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(PrioridadConvocatoria prioridad) {
        this.prioridad = prioridad;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }
}
