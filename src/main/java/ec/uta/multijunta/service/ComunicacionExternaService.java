package ec.uta.multijunta.service;

import ec.uta.multijunta.model.ComunicacionExterna;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.SecuenciaIngreso;
import ec.uta.multijunta.repository.ComunicacionExternaRepository;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.SecuenciaIngresoRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ComunicacionExternaService {

    private final ComunicacionExternaRepository comunicaciones;
    private final SecuenciaIngresoRepository secuencias;
    private final JuntaRepository juntas;
    private final ArchivoService archivos;
    private final UsuarioActual usuarioActual;

    public ComunicacionExternaService(ComunicacionExternaRepository comunicaciones, SecuenciaIngresoRepository secuencias,
            JuntaRepository juntas, ArchivoService archivos, UsuarioActual usuarioActual) {
        this.comunicaciones = comunicaciones;
        this.secuencias = secuencias;
        this.juntas = juntas;
        this.archivos = archivos;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<ComunicacionExterna> listar() {
        return comunicaciones.findByJuntaIdOrderByFechaDescIdDesc(usuarioActual.juntaId());
    }

    @Transactional
    public ComunicacionExterna registrar(DatosComunicacion datos, MultipartFile documento) {
        if (datos.fecha().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("comunicacion.fechaFutura", "fecha");
        }
        String extension = ArchivoService.tieneArchivo(documento) ? archivos.validarDocumento(documento, "documento") : null;
        Junta junta = juntas.findById(usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
        int anio = LocalDate.now().getYear();
        SecuenciaIngreso secuencia = secuencias.findByJuntaIdAndAnio(junta.getId(), anio).orElseGet(() -> {
            SecuenciaIngreso nueva = new SecuenciaIngreso();
            nueva.setJunta(junta);
            nueva.setAnio(anio);
            return nueva;
        });
        int numero = secuencia.siguiente();
        secuencias.save(secuencia);
        ComunicacionExterna comunicacion = new ComunicacionExterna();
        comunicacion.setJunta(junta);
        comunicacion.setNumeroIngreso(String.format("CE-%d-%04d", anio, numero));
        comunicacion.setRemitente(datos.remitente().trim());
        comunicacion.setAsunto(datos.asunto().trim());
        comunicacion.setFecha(datos.fecha());
        comunicacion.setTipoDocumento(datos.tipoDocumento());
        comunicacion.setRegistradoPor(usuarioActual.entidad());
        ComunicacionExterna guardada = comunicaciones.save(comunicacion);
        if (extension != null) {
            guardada.setDocumentoNombre(archivos.guardar(ArchivoService.COMUNICACIONES, "comunicacion-" + guardada.getId(),
                    documento, extension, null));
        }
        return guardada;
    }

    @Transactional
    public void adjuntar(Long id, MultipartFile documento) {
        ComunicacionExterna comunicacion = obtener(id);
        String extension = archivos.validarDocumento(documento, "documento");
        comunicacion.setDocumentoNombre(archivos.guardar(ArchivoService.COMUNICACIONES, "comunicacion-" + comunicacion.getId(),
                documento, extension, comunicacion.getDocumentoNombre()));
        comunicaciones.save(comunicacion);
    }

    @Transactional
    public void eliminar(Long id) {
        ComunicacionExterna comunicacion = obtener(id);
        Optional<Path> archivo = archivos.leer(ArchivoService.COMUNICACIONES, comunicacion.getDocumentoNombre());
        comunicaciones.delete(comunicacion);
        archivo.ifPresent(ruta -> {
            try {
                Files.deleteIfExists(ruta);
            } catch (IOException excepcion) {
                throw new UncheckedIOException(excepcion);
            }
        });
    }

    @Transactional(readOnly = true)
    public Path documento(Long id) {
        return archivos.leer(ArchivoService.COMUNICACIONES, obtener(id).getDocumentoNombre())
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    private ComunicacionExterna obtener(Long id) {
        return comunicaciones.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }
}
