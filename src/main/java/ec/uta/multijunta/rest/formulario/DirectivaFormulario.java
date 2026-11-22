package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import jakarta.validation.constraints.NotNull;

public class DirectivaFormulario extends CuentaFormulario {

    @NotNull(message = "{campo.obligatorio}")
    private Rol rol;

    public static DirectivaFormulario desde(Usuario usuario) {
        DirectivaFormulario formulario = new DirectivaFormulario();
        formulario.setCedula(usuario.getCedula());
        formulario.setNombres(usuario.getNombres());
        formulario.setApellidos(usuario.getApellidos());
        formulario.setEmail(usuario.getEmail());
        formulario.setRol(usuario.getRol());
        return formulario;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }
}
