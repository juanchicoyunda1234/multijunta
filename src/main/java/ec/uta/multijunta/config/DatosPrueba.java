package ec.uta.multijunta.config;

import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Predio;
import ec.uta.multijunta.model.Recurso;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.model.TipoRecurso;
import ec.uta.multijunta.model.Turno;
import ec.uta.multijunta.repository.EventoRepository;
import ec.uta.multijunta.repository.PredioRepository;
import ec.uta.multijunta.repository.RecursoRepository;
import ec.uta.multijunta.repository.TurnoRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoJunta;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.SectorRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import ec.uta.multijunta.service.SocioService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatosPrueba implements CommandLineRunner {

    public static final String CLAVE_COMUN = "Multijunta2026";

    private final JuntaRepository juntas;
    private final SectorRepository sectores;
    private final UsuarioRepository usuarios;
    private final SocioService socioService;
    private final PasswordEncoder codificador;
    private final RecursoRepository recursos;
    private final EventoRepository eventos;
    private final TurnoRepository turnos;
    private final PredioRepository predios;
    private final boolean habilitado;
    private String claveCodificada;

    public DatosPrueba(JuntaRepository juntas, SectorRepository sectores, UsuarioRepository usuarios,
            SocioService socioService, PasswordEncoder codificador, RecursoRepository recursos,
            EventoRepository eventos, TurnoRepository turnos, PredioRepository predios,
            @Value("${multijunta.datos-prueba:true}") boolean habilitado) {
        this.juntas = juntas;
        this.sectores = sectores;
        this.usuarios = usuarios;
        this.socioService = socioService;
        this.codificador = codificador;
        this.recursos = recursos;
        this.eventos = eventos;
        this.turnos = turnos;
        this.predios = predios;
        this.habilitado = habilitado;
    }

    private void administrativos(Junta junta, Sector sectorA, Sector sectorB, List<Socio> activos, String latitud,
            String longitud) {
        Recurso compuerta = recurso(junta, sectorA, "Compuerta #3", TipoRecurso.COMPUERTA, "30.00");
        Recurso canal = recurso(junta, sectorB, "Canal principal", TipoRecurso.CANAL, "120.00");
        LocalDate hoy = LocalDate.now();
        evento(junta, TipoEvento.ASAMBLEA, hoy.minusDays(1), LocalTime.of(9, 0), "Casa comunal", "Asamblea ordinaria");
        evento(junta, TipoEvento.MINGA, hoy.plusDays(5), LocalTime.of(7, 0), "Canal principal", "Limpieza del canal");
        evento(junta, TipoEvento.REUNION, hoy.plusDays(2), LocalTime.of(18, 0), "Casa comunal", "Reunión de directiva");
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);
        turno(junta, sectorA, compuerta, lunes.plusDays(1), LocalTime.of(8, 0), LocalTime.of(10, 0));
        turno(junta, sectorB, canal, lunes.plusDays(3), LocalTime.of(14, 0), LocalTime.of(16, 0));
        int numero = 1;
        for (Socio socio : activos) {
            Predio predio = new Predio();
            predio.setJunta(junta);
            predio.setSocio(socio);
            predio.setSector(socio.getSector());
            predio.setCodigo(String.format("P-%04d", numero));
            predio.setArea(new BigDecimal("1500.00").multiply(BigDecimal.valueOf(numero)));
            predio.setLatitud(new BigDecimal(latitud).add(new BigDecimal("0.002").multiply(BigDecimal.valueOf(numero))));
            predio.setLongitud(new BigDecimal(longitud));
            predios.save(predio);
            numero++;
        }
    }

    private Recurso recurso(Junta junta, Sector sector, String nombre, TipoRecurso tipo, String caudal) {
        Recurso recurso = new Recurso();
        recurso.setJunta(junta);
        recurso.setSector(sector);
        recurso.setNombre(nombre);
        recurso.setTipo(tipo);
        recurso.setCaudal(new BigDecimal(caudal));
        return recursos.save(recurso);
    }

    private void evento(Junta junta, TipoEvento tipo, LocalDate fecha, LocalTime hora, String lugar,
            String descripcion) {
        Evento evento = new Evento();
        evento.setJunta(junta);
        evento.setTipo(tipo);
        evento.setFecha(fecha);
        evento.setHora(hora);
        evento.setLugar(lugar);
        evento.setDescripcion(descripcion);
        eventos.save(evento);
    }

    private void turno(Junta junta, Sector sector, Recurso recurso, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        Turno turno = new Turno();
        turno.setJunta(junta);
        turno.setSector(sector);
        turno.setRecurso(recurso);
        turno.setFecha(fecha);
        turno.setHoraInicio(inicio);
        turno.setHoraFin(fin);
        turno.setResponsable("Juez de aguas");
        turnos.save(turno);
    }

    @Override
    public void run(String... argumentos) {
        if (habilitado) {
            cargar();
        }
    }

    public void cargar() {
        if (juntas.count() > 0) {
            return;
        }
        claveCodificada = codificador.encode(CLAVE_COMUN);
        usuario("superadmin", "1700000001", "Paola", "Andrade", Rol.SUPER_ADMIN, null);

        Junta riego = junta("Junta de Riego San Pedro", TipoJunta.RIEGO, "1890000001001", "Ambato", true);
        Sector norte = sector(riego, "Sector Norte");
        Sector sur = sector(riego, "Sector Sur");
        usuario("admin.sanpedro", "1800000011", "Luis", "Paredes", Rol.ADMIN_JUNTA, riego);
        usuario("secretaria.sanpedro", "1800000012", "María", "Chicaiza", Rol.SECRETARIO, riego);
        usuario("tesorero.sanpedro", "1800000013", "Jorge", "Villacís", Rol.TESORERO, riego);
        Usuario desactivado = usuario("secretario.anterior", "1800000014", "Ramiro", "Sánchez", Rol.SECRETARIO, riego);
        desactivado.setActivo(false);
        usuarios.save(desactivado);
        Socio rosa = socio(riego, norte, "1800000101", "Rosa", "Masaquiza", "SP-001", true, false, EstadoSocio.ACTIVO);
        Socio manuel = socio(riego, sur, "1800000102", "Manuel", "Toaquiza", "SP-002", false, false, EstadoSocio.ACTIVO);
        socio(riego, norte, "1800000103", "Carmen", "Pilla", "SP-003", false, false, EstadoSocio.INACTIVO);
        administrativos(riego, norte, sur, List.of(rosa, manuel), "-1.241700", "-78.619700");

        Junta rosario = junta("Junta Comunitaria El Rosario", TipoJunta.COMUNITARIA, "1890000002001", "San Pedro de Pelileo", true);
        Sector alto = sector(rosario, "Sector Alto");
        Sector bajo = sector(rosario, "Sector Bajo");
        usuario("admin.rosario", "1800000021", "Diana", "Guevara", Rol.ADMIN_JUNTA, rosario);
        usuario("secretario.rosario", "1800000022", "Pablo", "Freire", Rol.SECRETARIO, rosario);
        usuario("tesorera.rosario", "1800000023", "Lucía", "Medina", Rol.TESORERO, rosario);
        Socio segundo = socio(rosario, alto, "1800000201", "Segundo", "Caiza", "ER-001", true, true, EstadoSocio.ACTIVO);
        Socio blanca = socio(rosario, bajo, "1800000202", "Blanca", "Tisalema", "ER-002", false, false, EstadoSocio.ACTIVO);
        socio(rosario, alto, "1800000203", "Hugo", "Llerena", "ER-003", false, false, EstadoSocio.INACTIVO);
        administrativos(rosario, alto, bajo, List.of(segundo, blanca), "-1.329600", "-78.543100");

        Junta santaRosa = junta("Junta Parroquial Santa Rosa", TipoJunta.PARROQUIAL, "1890000003001", "Ambato", false);
        usuario("admin.santarosa", "1800000031", "Fernando", "Ortiz", Rol.ADMIN_JUNTA, santaRosa);
    }

    private Junta junta(String nombre, TipoJunta tipo, String ruc, String canton, boolean activa) {
        Junta junta = new Junta();
        junta.setNombre(nombre);
        junta.setTipo(tipo);
        junta.setRuc(ruc);
        junta.setProvincia("Tungurahua");
        junta.setCanton(canton);
        junta.setDireccion("Calle principal s/n");
        junta.setTelefono("032000000");
        junta.setMontoMultaInasistencia(new BigDecimal("5.00"));
        junta.setActiva(activa);
        if (!activa) {
            junta.setMotivoSuspension("Falta de pago del servicio de la plataforma");
        }
        return juntas.save(junta);
    }

    private Sector sector(Junta junta, String nombre) {
        Sector sector = new Sector();
        sector.setJunta(junta);
        sector.setNombre(nombre);
        return sectores.save(sector);
    }

    private Usuario usuario(String username, String cedula, String nombres, String apellidos, Rol rol, Junta junta) {
        Usuario usuario = new Usuario();
        completar(usuario, username, cedula, nombres, apellidos, junta);
        usuario.setRol(rol);
        usuario.setEmail(username + "@multijunta.test");
        usuario.setActivo(true);
        return usuarios.save(usuario);
    }

    private Socio socio(Junta junta, Sector sector, String cedula, String nombres, String apellidos, String codigo,
            boolean conCuenta, boolean debeCambiar, EstadoSocio estado) {
        Socio socio = new Socio();
        completar(socio, cedula, cedula, nombres, apellidos, junta);
        socio.setSector(sector);
        socio.setCodigoSocio(codigo);
        socio.setFechaIngreso(LocalDate.of(2020, 1, 15));
        socio.setEstado(estado);
        socio.setTieneCuenta(conCuenta);
        socio.setActivo(conCuenta);
        socio.setDebeCambiarPassword(debeCambiar);
        return socioService.registrar(socio);
    }

    private void completar(Usuario usuario, String username, String cedula, String nombres, String apellidos,
            Junta junta) {
        usuario.setUsername(username);
        usuario.setCedula(cedula);
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setJunta(junta);
        usuario.setPassword(claveCodificada);
        usuario.setDebeCambiarPassword(false);
    }
}
