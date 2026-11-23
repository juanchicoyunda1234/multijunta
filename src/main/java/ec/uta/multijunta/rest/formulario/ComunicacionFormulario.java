package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.TipoDocumentoExterno;
import ec.uta.multijunta.service.DatosComunicacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class ComunicacionFormulario {

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 150)
    private String remitente;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 200)
    private String asunto;

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha = LocalDate.now();

    @NotNull(message = "{campo.obligatorio}")
    private TipoDocumentoExterno tipoDocumento = TipoDocumentoExterno.OFICIO;

    public DatosComunicacion datos() {
        return new DatosComunicacion(remitente, asunto, fecha, tipoDocumento);
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public TipoDocumentoExterno getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumentoExterno tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }
}
