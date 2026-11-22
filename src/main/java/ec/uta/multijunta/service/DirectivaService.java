package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectivaService {

    public static final Set<Rol> ROLES_GESTIONABLES = EnumSet.of(Rol.SECRETARIO, Rol.TESORERO);

    private final UsuarioRepository usuarios;
    private final JuntaRepository juntas;
    private final CuentaFactory cuentas;
    private final UsuarioActual usuarioActual;

    public DirectivaService(UsuarioRepository usuarios, JuntaRepository juntas, CuentaFactory cuentas,
            UsuarioActual usuarioActual) {
        this.usuarios = usuarios;
        this.juntas = juntas;
        this.cuentas = cuentas;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findByJuntaIdAndRolInOrderByApellidosAsc(usuarioActual.juntaId(),
                CuentaFactory.ROLES_ADMINISTRATIVOS);
    }

    @Transactional
    public Credencial crear(DatosCuenta datos) {
        exigirRolGestionable(datos.rol());
        cuentas.validarCedulaLibre(datos.cedula());
        cuentas.validarCorreoLibre(datos.email(), null);
        Junta junta = juntas.findById(usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
        return cuentas.crear(datos, junta);
    }

    @Transactional
    public void editar(Long id, DatosCuenta datos) {
        exigirRolGestionable(datos.rol());
        Usuario usuario = buscarGestionable(id);
        cuentas.validarCorreoLibre(datos.email(), id);
        usuario.setNombres(datos.nombres().trim());
        usuario.setApellidos(datos.apellidos().trim());
        usuario.setEmail(CuentaFactory.normalizarCorreo(datos.email()));
        usuario.setRol(datos.rol());
        usuarios.save(usuario);
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo) {
        Usuario usuario = buscarGestionable(id);
        usuario.setActivo(activo);
        usuarios.save(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario buscarGestionable(Long id) {
        Usuario usuario = usuarios.findByIdAndJuntaId(id, usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
        if (!ROLES_GESTIONABLES.contains(usuario.getRol())) {
            if (CuentaFactory.ROLES_ADMINISTRATIVOS.contains(usuario.getRol())) {
                throw new AccessDeniedException("Cuenta no gestionable");
            }
            throw new RecursoNoEncontradoException();
        }
        return usuario;
    }

    private void exigirRolGestionable(Rol rol) {
        if (rol == null || !ROLES_GESTIONABLES.contains(rol)) {
            throw new AccessDeniedException("Rol no permitido");
        }
    }
}
