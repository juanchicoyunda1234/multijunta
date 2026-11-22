package ec.uta.multijunta.service;

import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.SectorRepository;
import ec.uta.multijunta.repository.SocioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NominaService {

    private final SocioRepository socios;
    private final SectorRepository sectores;
    private final JuntaRepository juntas;
    private final SocioService socioService;
    private final CuentaFactory cuentas;
    private final PasswordService passwords;
    private final UsuarioActual usuarioActual;

    public NominaService(SocioRepository socios, SectorRepository sectores, JuntaRepository juntas,
            SocioService socioService, CuentaFactory cuentas, PasswordService passwords, UsuarioActual usuarioActual) {
        this.socios = socios;
        this.sectores = sectores;
        this.juntas = juntas;
        this.socioService = socioService;
        this.cuentas = cuentas;
        this.passwords = passwords;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Socio> buscar(String texto) {
        String limpio = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return socios.buscar(usuarioActual.juntaId(), limpio);
    }

    @Transactional(readOnly = true)
    public Socio obtener(Long id) {
        return socios.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public List<Sector> sectores() {
        return sectores.findAllByJuntaIdOrderByNombreAsc(usuarioActual.juntaId());
    }

    @Transactional
    public Socio registrar(DatosSocio datos) {
        Long juntaId = usuarioActual.juntaId();
        if (sectores().isEmpty()) {
            throw new ReglaNegocioException("sector.ninguno", "sectorId");
        }
        Sector sector = sector(datos.sectorId());
        cuentas.validarCedulaLibre(datos.cedula());
        Junta junta = juntas.findById(juntaId).orElseThrow(RecursoNoEncontradoException::new);
        String cedula = datos.cedula().trim();
        Socio socio = new Socio();
        socio.setJunta(junta);
        socio.setCedula(cedula);
        socio.setUsername(cedula);
        socio.setPassword(passwords.codificar(passwords.generarTemporal()));
        socio.setNombres(datos.nombres().trim());
        socio.setApellidos(datos.apellidos().trim());
        socio.setCelular(vacioANulo(datos.celular()));
        socio.setSector(sector);
        socio.setCodigoSocio(socioService.siguienteCodigo(juntaId));
        socio.setFechaIngreso(LocalDate.now());
        socio.setEstado(EstadoSocio.ACTIVO);
        socio.setTieneCuenta(false);
        socio.setActivo(false);
        socio.setDebeCambiarPassword(false);
        return socioService.registrar(socio);
    }

    @Transactional
    public Socio editar(Long id, DatosSocio datos) {
        Socio socio = obtener(id);
        socio.setNombres(datos.nombres().trim());
        socio.setApellidos(datos.apellidos().trim());
        socio.setCelular(vacioANulo(datos.celular()));
        socio.setSector(sector(datos.sectorId()));
        return socios.save(socio);
    }

    @Transactional
    public void cambiarEstado(Long id, EstadoSocio estado) {
        Socio socio = obtener(id);
        socio.setEstado(estado);
        socios.save(socio);
    }

    private Sector sector(Long sectorId) {
        if (sectorId == null) {
            throw new ReglaNegocioException("campo.obligatorio", "sectorId");
        }
        return sectores.findByIdAndJuntaId(sectorId, usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
