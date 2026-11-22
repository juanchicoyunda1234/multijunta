package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdministradorService {

    public static final int MAXIMO_ADMINISTRADORES = 2;

    private final JuntaRepository juntas;
    private final UsuarioRepository usuarios;
    private final CuentaFactory cuentas;

    public AdministradorService(JuntaRepository juntas, UsuarioRepository usuarios, CuentaFactory cuentas) {
        this.juntas = juntas;
        this.usuarios = usuarios;
        this.cuentas = cuentas;
    }

    @Transactional
    public Credencial asignar(Long juntaId, DatosCuenta datos, Long reemplazadoId) {
        Junta junta = juntas.findById(juntaId).orElseThrow(RecursoNoEncontradoException::new);
        Usuario reemplazado = reemplazadoId == null ? null : usuarios.findByIdAndJuntaId(reemplazadoId, juntaId)
                .filter(usuario -> usuario.getRol() == Rol.ADMIN_JUNTA && usuario.isActivo())
                .orElseThrow(RecursoNoEncontradoException::new);
        cuentas.validarCedulaLibre(datos.cedula());
        cuentas.validarCorreoLibre(datos.email(), null);
        if (reemplazado == null) {
            if (usuarios.countByJuntaIdAndRolAndActivoTrue(juntaId, Rol.ADMIN_JUNTA) >= MAXIMO_ADMINISTRADORES) {
                throw new ReglaNegocioException("admin.limite");
            }
        } else {
            reemplazado.setActivo(false);
            usuarios.saveAndFlush(reemplazado);
        }
        return cuentas.crear(new DatosCuenta(datos.cedula(), datos.nombres(), datos.apellidos(), datos.email(),
                Rol.ADMIN_JUNTA), junta);
    }
}
