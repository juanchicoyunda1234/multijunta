package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Acta;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.repository.ActaRepository;
import ec.uta.multijunta.repository.JuntaRepository;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ActaService {

    private final ActaRepository actas;
    private final JuntaRepository juntas;
    private final ArchivoService archivos;
    private final UsuarioActual usuarioActual;

    public ActaService(ActaRepository actas, JuntaRepository juntas, ArchivoService archivos,
            UsuarioActual usuarioActual) {
        this.actas = actas;
        this.juntas = juntas;
        this.archivos = archivos;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Acta> listar() {
        return actas.findByJuntaIdOrderByFechaDescIdDesc(usuarioActual.juntaId());
    }

    @Transactional
    public Acta guardar(DatosActa datos, MultipartFile documento) {
        if (datos.fecha().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("acta.fechaFutura", "fecha");
        }
        archivos.validarPdf(documento, "documento");
        Junta junta = juntas.findById(usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
        Acta acta = new Acta();
        acta.setJunta(junta);
        acta.setTitulo(datos.titulo().trim());
        acta.setFecha(datos.fecha());
        acta.setTipoEvento(datos.tipoEvento());
        acta.setContenido(datos.contenido() == null || datos.contenido().isBlank() ? null : datos.contenido().trim());
        acta.setPublicaSocios(datos.publicaSocios());
        acta.setDocumentoNombre("pendiente");
        Acta guardada = actas.save(acta);
        guardada.setDocumentoNombre(archivos.guardar(ArchivoService.ACTAS, "acta-" + guardada.getId(), documento, "pdf",
                null));
        return guardada;
    }

    @Transactional
    public void cambiarVisibilidad(Long id, boolean publica) {
        Acta acta = obtener(id);
        acta.setPublicaSocios(publica);
        actas.save(acta);
    }

    @Transactional(readOnly = true)
    public Path documento(Long id) {
        return archivos.leer(ArchivoService.ACTAS, obtener(id).getDocumentoNombre())
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    private Acta obtener(Long id) {
        return actas.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }
}
