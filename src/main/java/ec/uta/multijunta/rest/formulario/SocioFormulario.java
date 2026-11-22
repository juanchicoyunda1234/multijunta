package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.service.DatosSocio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SocioFormulario {

    @NotBlank(message = "{socio.cedulaAyuda}")
    @Pattern(regexp = "\\s*\\d{10}\\s*", message = "{socio.cedulaAyuda}")
    private String cedula;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 100)
    private String nombres;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 100)
    private String apellidos;

    @Pattern(regexp = "(\\d{10})?", message = "{socio.celularFormato}")
    private String celular;

    @NotNull(message = "{campo.obligatorio}")
    private Long sectorId;

    public static SocioFormulario desde(Socio socio) {
        SocioFormulario formulario = new SocioFormulario();
        formulario.setCedula(socio.getCedula());
        formulario.setNombres(socio.getNombres());
        formulario.setApellidos(socio.getApellidos());
        formulario.setCelular(socio.getCelular());
        formulario.setSectorId(socio.getSector().getId());
        return formulario;
    }

    public DatosSocio datos() {
        return new DatosSocio(cedula, nombres, apellidos, celular, sectorId);
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
    }
}
