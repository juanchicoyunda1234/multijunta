package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.service.DatosCuenta;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CuentaFormulario {

    @NotBlank(message = "{campo.obligatorio}")
    @Pattern(regexp = "\\s*\\d{10}\\s*", message = "{cuenta.cedulaFormato}")
    private String cedula;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 100)
    private String nombres;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 100)
    private String apellidos;

    @NotBlank(message = "{campo.obligatorio}")
    @Email(message = "{cuenta.correoFormato}")
    @Size(max = 120)
    private String email;

    public DatosCuenta datos(Rol rol) {
        return new DatosCuenta(cedula, nombres, apellidos, email, rol);
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
