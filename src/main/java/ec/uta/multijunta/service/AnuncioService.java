package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Acta;
import ec.uta.multijunta.model.Convocatoria;
import ec.uta.multijunta.model.EstadoConvocatoria;
import ec.uta.multijunta.repository.ActaRepository;
import ec.uta.multijunta.repository.ConvocatoriaRepository;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnuncioService {

    private final ConvocatoriaRepository convocatorias;
    private final ActaRepository actas;
    private final ArchivoService archivos;
    private final UsuarioActual usuarioActual;

    public AnuncioService(ConvocatoriaRepository convocatorias, ActaRepository actas, ArchivoService archivos,
            UsuarioActual usuarioActual) {
        this.convocatorias = convocatorias;
        this.actas = actas;
        this.archivos = archivos;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Convocatoria> anuncios() {
        return MiInformacionService.ordenarAnuncios(convocatorias.findByJuntaIdAndEstadoOrderByFechaPublicacionDescIdDesc(
                usuarioActual.juntaId(), EstadoConvocatoria.PUBLICADA));
    }

    @Transactional(readOnly = true)
    public List<Acta> repositorio() {
        return actas.findByJuntaIdAndPublicaSociosTrueOrderByFechaDescIdDesc(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public Path documento(Long actaId) {
        Acta acta = actas.findByIdAndJuntaIdAndPublicaSociosTrue(actaId, usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
        return archivos.leer(ArchivoService.ACTAS, acta.getDocumentoNombre()).orElseThrow(RecursoNoEncontradoException::new);
    }
}
