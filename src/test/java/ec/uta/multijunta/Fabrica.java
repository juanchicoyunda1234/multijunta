package ec.uta.multijunta;

import ec.uta.multijunta.config.UsuarioAutenticado;
import ec.uta.multijunta.model.Acta;
import ec.uta.multijunta.model.Asistencia;
import ec.uta.multijunta.model.Convocatoria;
import ec.uta.multijunta.model.EstadoConvocatoria;
import ec.uta.multijunta.model.PrioridadConvocatoria;
import ec.uta.multijunta.model.TipoConvocatoria;
import ec.uta.multijunta.repository.ActaRepository;
import ec.uta.multijunta.repository.ConvocatoriaRepository;
import ec.uta.multijunta.service.ArchivoService;
import org.springframework.mock.web.MockMultipartFile;
import ec.uta.multijunta.model.Cuota;
import ec.uta.multijunta.model.EstadoAsistencia;
import ec.uta.multijunta.model.Multa;
import ec.uta.multijunta.repository.AsistenciaRepository;
import ec.uta.multijunta.repository.CuotaRepository;
import ec.uta.multijunta.repository.MultaRepository;
import java.util.Map;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Predio;
import ec.uta.multijunta.model.Recurso;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.model.TipoRecurso;
import ec.uta.multijunta.model.Turno;
import ec.uta.multijunta.repository.EventoRepository;
import ec.uta.multijunta.repository.PredioRepository;
import ec.uta.multijunta.repository.RecursoRepository;
import ec.uta.multijunta.repository.TurnoRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoJunta;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.SectorRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@Component
public class Fabrica {

    public static final String CLAVE = "Clave1234";

    private static final AtomicLong SECUENCIA = new AtomicLong(1100000000L);

    private final JuntaRepository juntas;
    private final SectorRepository sectores;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private String claveCodificada;

    public Fabrica(JuntaRepository juntas, SectorRepository sectores, UsuarioRepository usuarios,
            PasswordEncoder codificador) {
        this.juntas = juntas;
        this.sectores = sectores;
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @Autowired
    private EventoRepository eventos;

    @Autowired
    private RecursoRepository recursos;

    @Autowired
    private TurnoRepository turnos;

    @Autowired
    private PredioRepository predios;

    @Autowired
    private CuotaRepository cuotas;

    @Autowired
    private MultaRepository multas;

    @Autowired
    private AsistenciaRepository asistencias;

    @Autowired
    private ConvocatoriaRepository convocatorias;

    @Autowired
    private ActaRepository actas;

    @Autowired
    private ArchivoService archivos;

    public Convocatoria convocatoria(Junta junta, TipoConvocatoria tipo, LocalDate fechaEvento,
            EstadoConvocatoria estado) {
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setJunta(junta);
        convocatoria.setTitulo(tipo.getEtiqueta() + " " + SECUENCIA.incrementAndGet());
        convocatoria.setTipo(tipo);
        convocatoria.setFechaEvento(fechaEvento);
        convocatoria.setHora(LocalTime.of(9, 0));
        convocatoria.setLugar("Casa comunal");
        convocatoria.setContenido("Orden del día");
        convocatoria.setPrioridad(estado == EstadoConvocatoria.PUBLICADA
                ? PrioridadConvocatoria.URGENTE : PrioridadConvocatoria.NORMAL);
        convocatoria.setEstado(estado);
        convocatoria.setFechaPublicacion(estado == EstadoConvocatoria.PUBLICADA
                ? java.time.LocalDateTime.now().minusHours(1) : java.time.LocalDateTime.now().plusHours(1));
        convocatoria.setAutor(secretario(junta));
        return convocatorias.save(convocatoria);
    }

    public Acta acta(Junta junta, String titulo, boolean publica) {
        Acta acta = new Acta();
        acta.setJunta(junta);
        acta.setTitulo(titulo);
        acta.setFecha(LocalDate.now().minusDays(3));
        acta.setTipoEvento(TipoEvento.ASAMBLEA);
        acta.setContenido("Resumen de la sesión");
        acta.setPublicaSocios(publica);
        acta.setDocumentoNombre(archivos.guardar(ArchivoService.ACTAS, "acta-" + SECUENCIA.incrementAndGet(),
                new MockMultipartFile("documento", "acta.pdf", "application/pdf", PDF_MINIMO), "pdf", null));
        return actas.save(acta);
    }

    public static final byte[] PDF_MINIMO = "%PDF-1.4 %%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    public static final byte[] PNG_MINIMO = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    public Usuario tesorero(Junta junta) {
        return usuario(Rol.TESORERO, junta);
    }

    public Cuota cuota(Socio socio, String concepto, String monto, LocalDate vencimiento) {
        Cuota cuota = new Cuota();
        cuota.setJunta(socio.getJunta());
        cuota.setSocio(socio);
        cuota.setConcepto(concepto);
        cuota.setMonto(new BigDecimal(monto));
        cuota.setSaldoPendiente(new BigDecimal(monto));
        cuota.setFecha(LocalDate.now());
        cuota.setFechaVencimiento(vencimiento);
        return cuotas.save(cuota);
    }

    public Multa multa(Socio socio, String monto) {
        return multa(socio, monto, monto);
    }

    public Multa multa(Socio socio, String monto, String saldo) {
        Multa multa = new Multa();
        multa.setJunta(socio.getJunta());
        multa.setSocio(socio);
        multa.setAsistenciaId(SECUENCIA.incrementAndGet());
        multa.setEventoId(0L);
        multa.setTipoEvento(TipoEvento.MINGA);
        multa.setFechaEvento(LocalDate.now().minusDays(3));
        multa.setMotivo("Inasistencia a Minga");
        multa.setMonto(new BigDecimal(monto));
        multa.setFecha(LocalDate.now().minusDays(1));
        multa.setSaldoPendiente(new BigDecimal(monto));
        if (!saldo.equals(monto)) {
            multa.aplicar(new BigDecimal(monto).subtract(new BigDecimal(saldo)));
        }
        return multas.save(multa);
    }

    public Map<Socio, Asistencia> cerrarAsistencia(Evento evento, Map<Socio, EstadoAsistencia> estados) {
        Map<Socio, Asistencia> resultado = new java.util.LinkedHashMap<>();
        estados.forEach((socio, estado) -> {
            Asistencia asistencia = new Asistencia();
            asistencia.setEvento(evento);
            asistencia.setSocio(socio);
            asistencia.setFecha(evento.getFecha());
            asistencia.setEstado(estado);
            resultado.put(socio, asistencias.save(asistencia));
        });
        evento.setAsistenciaCerrada(true);
        eventos.save(evento);
        return resultado;
    }

    public Evento evento(Junta junta, TipoEvento tipo, LocalDate fecha) {
        Evento evento = new Evento();
        evento.setJunta(junta);
        evento.setTipo(tipo);
        evento.setFecha(fecha);
        evento.setHora(LocalTime.of(9, 0));
        evento.setLugar("Casa comunal");
        return eventos.save(evento);
    }

    public Recurso recurso(Junta junta, Sector sector, String nombre) {
        Recurso recurso = new Recurso();
        recurso.setJunta(junta);
        recurso.setSector(sector);
        recurso.setNombre(nombre);
        recurso.setTipo(TipoRecurso.COMPUERTA);
        recurso.setCaudal(new BigDecimal("30.00"));
        return recursos.save(recurso);
    }

    public Turno turno(Junta junta, Sector sector, Recurso recurso, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        Turno turno = new Turno();
        turno.setJunta(junta);
        turno.setSector(sector);
        turno.setRecurso(recurso);
        turno.setFecha(fecha);
        turno.setHoraInicio(inicio);
        turno.setHoraFin(fin);
        turno.setResponsable("Responsable");
        return turnos.save(turno);
    }

    public Predio predio(Socio socio, Sector sector) {
        Predio predio = new Predio();
        predio.setJunta(socio.getJunta());
        predio.setSocio(socio);
        predio.setSector(sector);
        predio.setCodigo("P-" + SECUENCIA.incrementAndGet());
        predio.setArea(new BigDecimal("1200.00"));
        predio.setLatitud(new BigDecimal("-1.241700"));
        predio.setLongitud(new BigDecimal("-78.619700"));
        return predios.save(predio);
    }

    public Usuario secretario(Junta junta) {
        return usuario(Rol.SECRETARIO, junta);
    }

    public static LocalDate proximoDiaLaboral(LocalDate desde) {
        LocalDate fecha = desde;
        while (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            fecha = fecha.plusDays(1);
        }
        return fecha;
    }

    public Junta junta(String nombre) {
        Junta junta = new Junta();
        junta.setNombre(nombre);
        junta.setTipo(TipoJunta.RIEGO);
        junta.setRuc(String.format("%013d", SECUENCIA.incrementAndGet()));
        junta.setProvincia("Tungurahua");
        junta.setCanton("Ambato");
        return juntas.save(junta);
    }

    public Sector sector(Junta junta) {
        Sector sector = new Sector();
        sector.setJunta(junta);
        sector.setNombre("Sector " + SECUENCIA.incrementAndGet());
        return sectores.save(sector);
    }

    public Usuario usuario(Rol rol, Junta junta) {
        if (rol == Rol.SOCIO) {
            return socio(junta, sector(junta));
        }
        Usuario usuario = new Usuario();
        completar(usuario, junta);
        usuario.setRol(rol);
        return usuarios.save(usuario);
    }

    public Socio socio(Junta junta, Sector sector) {
        Socio socio = new Socio();
        completar(socio, junta);
        socio.setUsername(socio.getCedula());
        socio.setSector(sector);
        socio.setCodigoSocio("S-" + socio.getCedula());
        socio.setFechaIngreso(LocalDate.of(2021, 3, 1));
        socio.setTieneCuenta(true);
        return usuarios.save(socio);
    }

    public Junta juntaConCatalogo(String nombre) {
        return junta(nombre);
    }

    public Socio socioSinCuenta(Junta junta, Sector sector, EstadoSocio estado) {
        Socio socio = socio(junta, sector);
        socio.setTieneCuenta(false);
        socio.setActivo(false);
        socio.setEstado(estado);
        return usuarios.save(socio);
    }

    public Usuario directiva(Rol rol, Junta junta, String email) {
        Usuario usuario = usuario(rol, junta);
        usuario.setEmail(email);
        return usuarios.save(usuario);
    }

    public Usuario superAdmin() {
        return usuario(Rol.SUPER_ADMIN, null);
    }

    public Usuario guardar(Usuario usuario) {
        return usuarios.save(usuario);
    }

    public Junta guardar(Junta junta) {
        return juntas.save(junta);
    }

    public RequestPostProcessor sesion(Usuario usuario) {
        return user(UsuarioAutenticado.desde(usuario));
    }

    private void completar(Usuario usuario, Junta junta) {
        String cedula = String.valueOf(SECUENCIA.incrementAndGet());
        usuario.setCedula(cedula);
        usuario.setUsername("usuario" + cedula);
        usuario.setNombres("Nombre" + cedula);
        usuario.setApellidos("Apellido");
        usuario.setJunta(junta);
        usuario.setPassword(clave());
        usuario.setActivo(true);
        usuario.setDebeCambiarPassword(false);
    }

    private String clave() {
        if (claveCodificada == null) {
            claveCodificada = codificador.encode(CLAVE);
        }
        return claveCodificada;
    }
}
