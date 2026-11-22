package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.service.DatosConfiguracion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class ConfiguracionFormulario {

    @Size(max = 20, message = "{configuracion.telefonoInvalido}")
    @Pattern(regexp = "[0-9 +\\-]*", message = "{configuracion.telefonoInvalido}")
    private String telefono;

    @Size(max = 200)
    private String direccion;

    @DecimalMin(value = "0.00", message = "{configuracion.montoInvalido}")
    @Digits(integer = 8, fraction = 2, message = "{configuracion.montoInvalido}")
    private BigDecimal montoMultaInasistencia;

    private MultipartFile logo;

    private List<String> sectoresNuevos = new ArrayList<>();

    private List<Long> sectoresQuitados = new ArrayList<>();

    public static ConfiguracionFormulario desde(Junta junta) {
        ConfiguracionFormulario formulario = new ConfiguracionFormulario();
        formulario.setTelefono(junta.getTelefono());
        formulario.setDireccion(junta.getDireccion());
        formulario.setMontoMultaInasistencia(junta.getMontoMultaInasistencia());
        return formulario;
    }

    public DatosConfiguracion datos() {
        return new DatosConfiguracion(telefono, direccion, montoMultaInasistencia, logo, sectoresNuevos,
                sectoresQuitados);
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public BigDecimal getMontoMultaInasistencia() {
        return montoMultaInasistencia;
    }

    public void setMontoMultaInasistencia(BigDecimal montoMultaInasistencia) {
        this.montoMultaInasistencia = montoMultaInasistencia;
    }

    public MultipartFile getLogo() {
        return logo;
    }

    public void setLogo(MultipartFile logo) {
        this.logo = logo;
    }

    public List<String> getSectoresNuevos() {
        return sectoresNuevos;
    }

    public void setSectoresNuevos(List<String> sectoresNuevos) {
        this.sectoresNuevos = sectoresNuevos == null ? new ArrayList<>() : sectoresNuevos;
    }

    public List<Long> getSectoresQuitados() {
        return sectoresQuitados;
    }

    public void setSectoresQuitados(List<Long> sectoresQuitados) {
        this.sectoresQuitados = sectoresQuitados == null ? new ArrayList<>() : sectoresQuitados;
    }
}
