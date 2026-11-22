package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import java.nio.file.Path;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.SectorRepository;
import ec.uta.multijunta.repository.PredioRepository;
import ec.uta.multijunta.repository.RecursoRepository;
import ec.uta.multijunta.repository.SocioRepository;
import ec.uta.multijunta.repository.TurnoRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracionJuntaService {

    private static final int LONGITUD_SECTOR = 80;

    private final JuntaRepository juntas;
    private final SectorRepository sectores;
    private final SocioRepository socios;
    private final PredioRepository predios;
    private final RecursoRepository recursos;
    private final TurnoRepository turnos;
    private final ArchivoService archivos;
    private final UsuarioActual usuarioActual;

    public ConfiguracionJuntaService(JuntaRepository juntas, SectorRepository sectores, SocioRepository socios,
            PredioRepository predios, RecursoRepository recursos, TurnoRepository turnos, ArchivoService archivos,
            UsuarioActual usuarioActual) {
        this.juntas = juntas;
        this.sectores = sectores;
        this.socios = socios;
        this.predios = predios;
        this.recursos = recursos;
        this.turnos = turnos;
        this.archivos = archivos;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public Junta junta() {
        return juntas.findById(usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public List<Sector> sectores() {
        return sectores.findAllByJuntaIdOrderByNombreAsc(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public Path logo(Long juntaId) {
        boolean permitido = usuarioActual.rol() == Rol.SUPER_ADMIN || juntaId.equals(usuarioActual.juntaId());
        if (!permitido) {
            throw new RecursoNoEncontradoException();
        }
        Junta junta = juntas.findById(juntaId).orElseThrow(RecursoNoEncontradoException::new);
        return archivos.logo(junta.getLogoUrl()).orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public boolean tieneLogo() {
        return usuarioActual.sesion()
                .map(sesion -> sesion.getJuntaId())
                .flatMap(juntas::findById)
                .map(junta -> junta.getLogoUrl() != null)
                .orElse(false);
    }

    @Transactional
    public void actualizar(DatosConfiguracion datos) {
        Junta junta = junta();
        if (datos.direccion() == null || datos.direccion().isBlank()) {
            throw new ReglaNegocioException("configuracion.direccionObligatoria", "direccion");
        }
        BigDecimal monto = datos.montoMultaInasistencia();
        if (monto != null && (monto.signum() < 0 || monto.stripTrailingZeros().scale() > 2)) {
            throw new ReglaNegocioException("configuracion.montoInvalido", "montoMultaInasistencia");
        }
        List<Sector> quitados = validarQuitados(datos.sectoresQuitados());
        List<String> nuevos = validarNuevos(junta.getId(), datos.sectoresNuevos(), quitados);
        boolean conLogo = datos.logo() != null && !datos.logo().isEmpty();
        String extension = conLogo ? archivos.extensionDeLogo(datos.logo()) : null;

        junta.setTelefono(vacioANulo(datos.telefono()));
        junta.setDireccion(datos.direccion().trim());
        junta.setMontoMultaInasistencia(monto);
        sectores.deleteAll(quitados);
        sectores.flush();
        nuevos.forEach(nombre -> {
            Sector sector = new Sector();
            sector.setJunta(junta);
            sector.setNombre(nombre);
            sectores.save(sector);
        });
        if (conLogo) {
            junta.setLogoUrl(archivos.guardarLogo(junta.getId(), datos.logo(), extension, junta.getLogoUrl()));
        }
        juntas.save(junta);
    }

    private List<Sector> validarQuitados(List<Long> ids) {
        List<Sector> resultado = new ArrayList<>();
        for (Long id : ids == null ? List.<Long>of() : ids) {
            Sector sector = sectores.findByIdAndJuntaId(id, usuarioActual.juntaId())
                    .orElseThrow(RecursoNoEncontradoException::new);
            Long sectorId = sector.getId();
            if (socios.existsBySectorId(sectorId) || predios.existsBySectorId(sectorId)
                    || recursos.existsBySectorId(sectorId) || turnos.existsBySectorId(sectorId)) {
                throw new ReglaNegocioException("sector.enUso", "sectores");
            }
            resultado.add(sector);
        }
        return resultado;
    }

    private List<String> validarNuevos(Long juntaId, List<String> nombres, List<Sector> quitados) {
        Set<String> vistos = new HashSet<>();
        Set<String> liberados = new HashSet<>();
        quitados.forEach(sector -> liberados.add(clave(sector.getNombre())));
        List<String> resultado = new ArrayList<>();
        for (String nombre : nombres == null ? List.<String>of() : nombres) {
            if (nombre == null || nombre.isBlank()) {
                throw new ReglaNegocioException("sector.vacio", "sectores");
            }
            String limpio = nombre.trim();
            boolean existente = sectores.existsByJuntaIdAndNombreIgnoreCase(juntaId, limpio)
                    && !liberados.contains(clave(limpio));
            if (limpio.length() > LONGITUD_SECTOR) {
                throw new ReglaNegocioException("sector.largo", "sectores");
            }
            if (existente || !vistos.add(clave(limpio))) {
                throw new ReglaNegocioException("sector.duplicado", "sectores");
            }
            resultado.add(limpio);
        }
        return resultado;
    }

    private static String clave(String nombre) {
        return nombre.trim().toLowerCase(Locale.ROOT);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
