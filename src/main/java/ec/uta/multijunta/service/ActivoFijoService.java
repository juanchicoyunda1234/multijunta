package ec.uta.multijunta.service;

import ec.uta.multijunta.model.ActivoFijo;
import ec.uta.multijunta.model.EstadoActivo;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.repository.ActivoFijoRepository;
import ec.uta.multijunta.repository.JuntaRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ActivoFijoService {

    private static final BigDecimal LIMITE_SIN_CATEGORIA = new BigDecimal("200");

    private final ActivoFijoRepository activos;
    private final JuntaRepository juntas;
    private final ArchivoService archivos;
    private final UsuarioActual usuarioActual;

    public ActivoFijoService(ActivoFijoRepository activos, JuntaRepository juntas, ArchivoService archivos,
            UsuarioActual usuarioActual) {
        this.activos = activos;
        this.juntas = juntas;
        this.archivos = archivos;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<ActivoFijo> listar() {
        return activos.findByJuntaIdOrderByIdDesc(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public BigDecimal patrimonio() {
        LocalDate hoy = LocalDate.now();
        return listar().stream().filter(activo -> !activo.isDadoDeBaja())
                .map(activo -> activo.valorEnLibros(hoy))
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    @Transactional
    public ActivoFijo registrar(DatosActivo datos, MultipartFile archivo) {
        if (datos.valorAdquisicion() == null || datos.valorAdquisicion().signum() <= 0) {
            throw new ReglaNegocioException("activo.valor", "valorAdquisicion");
        }
        Montos.validar(datos.valorAdquisicion(), "valorAdquisicion");
        if (datos.categoria() == null && datos.valorAdquisicion().compareTo(LIMITE_SIN_CATEGORIA) > 0) {
            throw new ReglaNegocioException("activo.categoria", "categoria");
        }
        if (datos.fechaAdquisicion().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("activo.fechaFutura", "fechaAdquisicion");
        }
        if (datos.estado() == EstadoActivo.DADO_DE_BAJA) {
            throw new ReglaNegocioException("campo.obligatorio", "estado");
        }
        String extension = ArchivoService.tieneArchivo(archivo) ? archivos.validarDocumento(archivo, "archivo") : null;
        Long juntaId = usuarioActual.juntaId();
        Junta junta = juntas.findById(juntaId).orElseThrow(RecursoNoEncontradoException::new);
        String prefijo = "AF-" + LocalDate.now().getYear() + "-";
        ActivoFijo activo = new ActivoFijo();
        activo.setJunta(junta);
        activo.setCodigo(prefijo + String.format("%04d", activos.countByJuntaIdAndCodigoStartingWith(juntaId, prefijo) + 1));
        activo.setNombre(datos.nombre().trim());
        activo.setCategoria(datos.categoria());
        activo.setFechaAdquisicion(datos.fechaAdquisicion());
        activo.setValorAdquisicion(Montos.normalizar(datos.valorAdquisicion()));
        activo.setEstado(datos.estado());
        ActivoFijo guardado = activos.save(activo);
        if (extension != null) {
            guardado.setArchivoNombre(archivos.guardar(ArchivoService.ACTIVOS, "activo-" + guardado.getId(), archivo,
                    extension, null));
        }
        return guardado;
    }

    @Transactional
    public void darDeBaja(Long id, String motivo) {
        ActivoFijo activo = obtener(id);
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("motivo.obligatorio");
        }
        if (activo.isDadoDeBaja()) {
            throw new ReglaNegocioException("activo.yaBaja");
        }
        activo.setEstado(EstadoActivo.DADO_DE_BAJA);
        activo.setMotivoBaja(motivo.trim());
        activo.setFechaBaja(LocalDate.now());
        activos.save(activo);
    }

    @Transactional(readOnly = true)
    public Optional<Path> archivo(Long id) {
        return archivos.leer(ArchivoService.ACTIVOS, obtener(id).getArchivoNombre());
    }

    private ActivoFijo obtener(Long id) {
        return activos.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }
}
