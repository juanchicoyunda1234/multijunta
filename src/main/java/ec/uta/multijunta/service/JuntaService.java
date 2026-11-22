package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JuntaService {

    public record FilaJunta(Junta junta, List<Usuario> administradores) {
    }

    private static final int MAXIMO_MOTIVO = 300;

    private final JuntaRepository juntas;
    private final UsuarioRepository usuarios;
    private final CatalogoUbicacionService catalogo;

    public JuntaService(JuntaRepository juntas, UsuarioRepository usuarios, CatalogoUbicacionService catalogo) {
        this.juntas = juntas;
        this.usuarios = usuarios;
        this.catalogo = catalogo;
    }

    @Transactional(readOnly = true)
    public List<FilaJunta> listar() {
        return juntas.findAllByOrderByNombreAsc().stream()
                .map(junta -> new FilaJunta(junta,
                        usuarios.findByJuntaIdAndRolAndActivoTrue(junta.getId(), Rol.ADMIN_JUNTA)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Junta obtener(Long id) {
        return juntas.findById(id).orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional
    public Junta registrar(DatosJunta datos) {
        if (juntas.existsByRuc(datos.ruc())) {
            throw new ReglaNegocioException("junta.rucDuplicado", "ruc");
        }
        Junta junta = new Junta();
        aplicar(junta, datos);
        junta.setActiva(true);
        return juntas.save(junta);
    }

    @Transactional
    public Junta editar(Long id, DatosJunta datos) {
        Junta junta = obtener(id);
        if (juntas.existsByRucAndIdNot(datos.ruc(), id)) {
            throw new ReglaNegocioException("junta.rucDuplicado", "ruc");
        }
        aplicar(junta, datos);
        return juntas.save(junta);
    }

    @Transactional
    public void suspender(Long id, String motivo) {
        Junta junta = obtener(id);
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("junta.motivoObligatorio", "motivo");
        }
        if (motivo.trim().length() > MAXIMO_MOTIVO) {
            throw new ReglaNegocioException("junta.motivoLargo", "motivo");
        }
        junta.setActiva(false);
        junta.setMotivoSuspension(motivo.trim());
        juntas.save(junta);
    }

    @Transactional
    public void reactivar(Long id) {
        Junta junta = obtener(id);
        junta.setActiva(true);
        junta.setMotivoSuspension(null);
        juntas.save(junta);
    }

    private void aplicar(Junta junta, DatosJunta datos) {
        if (!catalogo.esValido(datos.provincia(), datos.canton())) {
            throw new ReglaNegocioException("junta.cantonInvalido", "canton");
        }
        junta.setNombre(datos.nombre().trim());
        junta.setRuc(datos.ruc());
        junta.setTipo(datos.tipo());
        junta.setProvincia(datos.provincia());
        junta.setCanton(datos.canton());
    }
}
