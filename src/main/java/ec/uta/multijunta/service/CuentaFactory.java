package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CuentaFactory {

    public static final Set<Rol> ROLES_ADMINISTRATIVOS = EnumSet.of(Rol.ADMIN_JUNTA, Rol.SECRETARIO, Rol.TESORERO);

    private final UsuarioRepository usuarios;
    private final PasswordService passwords;

    public CuentaFactory(UsuarioRepository usuarios, PasswordService passwords) {
        this.usuarios = usuarios;
        this.passwords = passwords;
    }

    public void validarCedulaLibre(String cedula) {
        if (cedula == null || usuarios.existsByCedula(cedula.trim())) {
            throw new ReglaNegocioException("cuenta.cedulaDuplicada", "cedula");
        }
    }

    public void validarCorreoLibre(String email, Long excluirId) {
        String normalizado = normalizarCorreo(email);
        boolean ocupado = excluirId == null
                ? usuarios.existsByEmailIgnoreCaseAndRolIn(normalizado, ROLES_ADMINISTRATIVOS)
                : usuarios.existsByEmailIgnoreCaseAndRolInAndIdNot(normalizado, ROLES_ADMINISTRATIVOS, excluirId);
        if (ocupado) {
            throw new ReglaNegocioException("cuenta.correoDuplicado", "email");
        }
    }

    public Credencial crear(DatosCuenta datos, Junta junta) {
        String temporal = passwords.generarTemporal();
        Usuario usuario = new Usuario();
        usuario.setCedula(datos.cedula().trim());
        usuario.setUsername(datos.cedula().trim());
        usuario.setNombres(datos.nombres().trim());
        usuario.setApellidos(datos.apellidos().trim());
        usuario.setEmail(normalizarCorreo(datos.email()));
        usuario.setRol(datos.rol());
        usuario.setJunta(junta);
        usuario.setActivo(true);
        usuario.setDebeCambiarPassword(true);
        usuario.setPassword(passwords.codificar(temporal));
        usuarios.save(usuario);
        return new Credencial(usuario.getNombreCompleto(), usuario.getUsername(), temporal);
    }

    public static String normalizarCorreo(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
