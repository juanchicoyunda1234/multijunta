package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.service.DatosActa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class ActaFormulario {

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 150)
    private String titulo;

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha = LocalDate.now();

    @NotNull(message = "{campo.obligatorio}")
    private TipoEvento tipoEvento = TipoEvento.ASAMBLEA;

    @Size(max = 1000)
    private String contenido;

    private boolean publicaSocios = true;

    public DatosActa datos() {
        return new DatosActa(titulo, fecha, tipoEvento, contenido, publicaSocios);
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEvento tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public boolean isPublicaSocios() {
        return publicaSocios;
    }

    public void setPublicaSocios(boolean publicaSocios) {
        this.publicaSocios = publicaSocios;
    }
}
