package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.TipoJunta;
import ec.uta.multijunta.service.DatosJunta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class JuntaFormulario {

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 150)
    private String nombre;

    @NotBlank(message = "{campo.obligatorio}")
    @Pattern(regexp = "\\d{13}", message = "{junta.rucFormato}")
    private String ruc;

    @NotNull(message = "{campo.obligatorio}")
    private TipoJunta tipo;

    @NotBlank(message = "{campo.obligatorio}")
    private String provincia;

    @NotBlank(message = "{campo.obligatorio}")
    private String canton;

    public static JuntaFormulario desde(Junta junta) {
        JuntaFormulario formulario = new JuntaFormulario();
        formulario.setNombre(junta.getNombre());
        formulario.setRuc(junta.getRuc());
        formulario.setTipo(junta.getTipo());
        formulario.setProvincia(junta.getProvincia());
        formulario.setCanton(junta.getCanton());
        return formulario;
    }

    public DatosJunta datos() {
        return new DatosJunta(nombre, ruc, tipo, provincia, canton);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public TipoJunta getTipo() {
        return tipo;
    }

    public void setTipo(TipoJunta tipo) {
        this.tipo = tipo;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getCanton() {
        return canton;
    }

    public void setCanton(String canton) {
        this.canton = canton;
    }
}
