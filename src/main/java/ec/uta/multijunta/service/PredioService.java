package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Predio;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.repository.PredioRepository;
import ec.uta.multijunta.repository.SectorRepository;
import ec.uta.multijunta.repository.SocioRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PredioService {

    private final PredioRepository predios;
    private final SocioRepository socios;
    private final SectorRepository sectores;
    private final ArchivoService archivos;
    private final UsuarioActual usuarioActual;

    public PredioService(PredioRepository predios, SocioRepository socios, SectorRepository sectores,
            ArchivoService archivos, UsuarioActual usuarioActual) {
        this.predios = predios;
        this.socios = socios;
        this.sectores = sectores;
        this.archivos = archivos;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Predio> delSocio(Long socioId) {
        return predios.findBySocioIdAndJuntaIdOrderByIdAsc(socioId, usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public Predio obtener(Long id) {
        return predios.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public Socio socio(Long socioId) {
        return socios.findByIdAndJuntaId(socioId, usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public List<Sector> sectores() {
        return sectores.findAllByJuntaIdOrderByNombreAsc(usuarioActual.juntaId());
    }

    @Transactional
    public Predio crear(Long socioId, DatosPredio datos) {
        Socio socio = socio(socioId);
        Predio predio = new Predio();
        predio.setJunta(socio.getJunta());
        predio.setSocio(socio);
        predio.setCodigo(siguienteCodigo());
        return guardar(predio, datos);
    }

    @Transactional
    public Predio editar(Long id, DatosPredio datos) {
        return guardar(obtener(id), datos);
    }

    @Transactional(readOnly = true)
    public Path foto(Long id) {
        Predio predio = obtener(id);
        return archivos.leer(ArchivoService.PREDIOS, predio.getFotoNombre())
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    private Predio guardar(Predio predio, DatosPredio datos) {
        if (datos.area() == null || datos.area().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("predio.area", "area");
        }
        if (datos.latitud() == null || datos.longitud() == null) {
            throw new ReglaNegocioException("predio.sinUbicacion", "latitud");
        }
        if (!Predio.dentroDelEcuador(datos.latitud(), datos.longitud())) {
            throw new ReglaNegocioException("predio.fueraEcuador", "latitud");
        }
        if (datos.sectorId() == null) {
            throw new ReglaNegocioException("campo.obligatorio", "sectorId");
        }
        Sector sector = sectores.findByIdAndJuntaId(datos.sectorId(), usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
        boolean conFoto = datos.foto() != null && !datos.foto().isEmpty();
        String extension = conFoto
                ? archivos.validarImagen(datos.foto(), "predio.fotoGrande", "predio.fotoTipo", "foto")
                : null;
        predio.setSector(sector);
        predio.setArea(datos.area());
        predio.setUbicacion(datos.ubicacion() == null || datos.ubicacion().isBlank() ? null : datos.ubicacion().trim());
        predio.setLatitud(datos.latitud());
        predio.setLongitud(datos.longitud());
        Predio guardado = predios.save(predio);
        if (conFoto) {
            guardado.setFotoNombre(archivos.guardar(ArchivoService.PREDIOS, "predio-" + guardado.getJunta().getId(),
                    datos.foto(), extension, guardado.getFotoNombre()));
            guardado = predios.save(guardado);
        }
        return guardado;
    }

    private String siguienteCodigo() {
        Long juntaId = usuarioActual.juntaId();
        long numero = predios.countByJuntaId(juntaId) + 1;
        String codigo = String.format("P-%04d", numero);
        while (predios.existsByJuntaIdAndCodigo(juntaId, codigo)) {
            numero++;
            codigo = String.format("P-%04d", numero);
        }
        return codigo;
    }
}
