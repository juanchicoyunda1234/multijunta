package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Recurso;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.RecursoRepository;
import ec.uta.multijunta.repository.SectorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecursoService {

    private final RecursoRepository recursos;
    private final SectorRepository sectores;
    private final JuntaRepository juntas;
    private final UsuarioActual usuarioActual;

    public RecursoService(RecursoRepository recursos, SectorRepository sectores, JuntaRepository juntas,
            UsuarioActual usuarioActual) {
        this.recursos = recursos;
        this.sectores = sectores;
        this.juntas = juntas;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Recurso> listar() {
        return recursos.findByJuntaIdOrderByNombreAsc(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public List<Recurso> activos() {
        return recursos.findByJuntaIdAndActivoTrueOrderByNombreAsc(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public List<Sector> sectores() {
        return sectores.findAllByJuntaIdOrderByNombreAsc(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public Recurso obtener(Long id) {
        return recursos.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional
    public Recurso crear(DatosRecurso datos) {
        Long juntaId = usuarioActual.juntaId();
        if (recursos.existsByJuntaIdAndNombreIgnoreCase(juntaId, datos.nombre().trim())) {
            throw new ReglaNegocioException("recurso.duplicado", "nombre");
        }
        Junta junta = juntas.findById(juntaId).orElseThrow(RecursoNoEncontradoException::new);
        Recurso recurso = new Recurso();
        recurso.setJunta(junta);
        recurso.setActivo(true);
        return guardar(recurso, datos);
    }

    @Transactional
    public Recurso editar(Long id, DatosRecurso datos) {
        Recurso recurso = obtener(id);
        if (recursos.existsByJuntaIdAndNombreIgnoreCaseAndIdNot(usuarioActual.juntaId(), datos.nombre().trim(), id)) {
            throw new ReglaNegocioException("recurso.duplicado", "nombre");
        }
        return guardar(recurso, datos);
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo) {
        Recurso recurso = obtener(id);
        recurso.setActivo(activo);
        recursos.save(recurso);
    }

    private Recurso guardar(Recurso recurso, DatosRecurso datos) {
        if (datos.caudal() == null || datos.caudal().signum() <= 0) {
            throw new ReglaNegocioException("recurso.caudal", "caudal");
        }
        Sector sector = sectores.findByIdAndJuntaId(datos.sectorId(), usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
        recurso.setNombre(datos.nombre().trim());
        recurso.setTipo(datos.tipo());
        recurso.setSector(sector);
        recurso.setCaudal(datos.caudal());
        return recursos.save(recurso);
    }
}
