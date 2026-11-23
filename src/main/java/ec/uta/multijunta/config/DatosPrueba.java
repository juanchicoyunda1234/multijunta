package ec.uta.multijunta.config;

import ec.uta.multijunta.model.Acta;
import ec.uta.multijunta.model.ActivoFijo;
import ec.uta.multijunta.model.ComunicacionExterna;
import ec.uta.multijunta.model.Conversacion;
import ec.uta.multijunta.model.Convocatoria;
import ec.uta.multijunta.model.EstadoConvocatoria;
import ec.uta.multijunta.model.Mensaje;
import ec.uta.multijunta.model.PrioridadConvocatoria;
import ec.uta.multijunta.model.SecuenciaIngreso;
import ec.uta.multijunta.model.TipoConvocatoria;
import ec.uta.multijunta.model.TipoDocumentoExterno;
import ec.uta.multijunta.repository.ActaRepository;
import ec.uta.multijunta.repository.ComunicacionExternaRepository;
import ec.uta.multijunta.repository.ConversacionRepository;
import ec.uta.multijunta.repository.ConvocatoriaRepository;
import ec.uta.multijunta.repository.MensajeRepository;
import ec.uta.multijunta.repository.SecuenciaIngresoRepository;
import ec.uta.multijunta.service.ArchivoService;
import java.nio.charset.StandardCharsets;
import ec.uta.multijunta.model.Asistencia;
import ec.uta.multijunta.model.CategoriaActivo;
import ec.uta.multijunta.model.CategoriaEgreso;
import ec.uta.multijunta.model.Cuota;
import ec.uta.multijunta.model.Egreso;
import ec.uta.multijunta.model.EstadoActivo;
import ec.uta.multijunta.model.EstadoAsistencia;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.ItemInventario;
import ec.uta.multijunta.model.MetodoPago;
import ec.uta.multijunta.model.MovimientoInventario;
import ec.uta.multijunta.model.Pago;
import ec.uta.multijunta.model.TipoMovimiento;
import ec.uta.multijunta.repository.ActivoFijoRepository;
import ec.uta.multijunta.repository.AsistenciaRepository;
import ec.uta.multijunta.repository.CuotaRepository;
import ec.uta.multijunta.repository.EgresoRepository;
import ec.uta.multijunta.repository.ItemInventarioRepository;
import ec.uta.multijunta.repository.MovimientoInventarioRepository;
import ec.uta.multijunta.repository.PagoRepository;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AsistenciaRepository asistencias;

    @Autowired
    private CuotaRepository cuotas;

    @Autowired
    private PagoRepository pagos;

    @Autowired
    private EgresoRepository egresos;

    @Autowired
    private ItemInventarioRepository items;

    @Autowired
    private MovimientoInventarioRepository movimientos;

    @Autowired
    private ActivoFijoRepository activosFijos;

    @Autowired
    private ConvocatoriaRepository convocatorias;

    @Autowired
    private ActaRepository actas;

    @Autowired
    private ComunicacionExternaRepository comunicaciones;

    @Autowired
    private SecuenciaIngresoRepository secuencias;

    @Autowired
    private ConversacionRepository conversaciones;

    @Autowired
    private MensajeRepository mensajes;

    @Autowired
    private ArchivoService archivos;

    private static final byte[] PDF_DEMOSTRACION = ("%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
            + "2 0 obj<</Type/Pages/Kids[]/Count 0>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF\n")
            .getBytes(StandardCharsets.US_ASCII);

    private void comunicacionesDePrueba(Junta junta, Usuario secretario, Usuario admin, Socio socio) {
        LocalDate hoy = LocalDate.now();
        convocatoria(junta, secretario, "Asamblea General Ordinaria", TipoConvocatoria.ASAMBLEA, hoy.plusDays(7),
                PrioridadConvocatoria.URGENTE, LocalDateTime.now().minusHours(2), EstadoConvocatoria.PUBLICADA);
        convocatoria(junta, secretario, "Minga de limpieza de la acequia", TipoConvocatoria.MINGA, hoy.plusDays(12),
                PrioridadConvocatoria.NORMAL, LocalDateTime.now().plusDays(1), EstadoConvocatoria.BORRADOR);
        acta(junta, "Acta de Asamblea Ordinaria Nº 01", hoy.minusDays(20), true);
        acta(junta, "Acta de reunión de directiva", hoy.minusDays(6), false);
        SecuenciaIngreso secuencia = new SecuenciaIngreso();
        secuencia.setJunta(junta);
        secuencia.setAnio(hoy.getYear());
        comunicacion(junta, secretario, secuencia, "Ministerio de Ambiente", "Inspección de canales de riego",
                hoy.minusDays(8), TipoDocumentoExterno.OFICIO, true);
        comunicacion(junta, secretario, secuencia, "Prefectura de Tungurahua", "Convocatoria a taller de riego",
                hoy.minusDays(2), TipoDocumentoExterno.NOTIFICACION, false);
        secuencias.save(secuencia);
        Conversacion conversacion = new Conversacion();
        conversacion.setJunta(junta);
        conversacion.setSocio(socio);
        conversacion = conversaciones.save(conversacion);
        Mensaje bienvenida = mensaje(conversacion, admin, "Bienvenido al canal de mensajes de la junta.",
                LocalDateTime.now().minusDays(1));
        Mensaje consulta = mensaje(conversacion, socio, "Buenos días, ¿cuándo es mi próximo turno de riego?",
                LocalDateTime.now().minusHours(3));
        conversacion.setUltimoLeidoA(bienvenida.getId());
        conversacion.setUltimoLeidoB(bienvenida.getId());
        conversacion.setActualizada(consulta.getFechaEnvio());
        conversaciones.save(conversacion);
    }

    private void convocatoria(Junta junta, Usuario autor, String titulo, TipoConvocatoria tipo, LocalDate fecha,
            PrioridadConvocatoria prioridad, LocalDateTime publicacion, EstadoConvocatoria estado) {
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setJunta(junta);
        convocatoria.setAutor(autor);
        convocatoria.setTitulo(titulo);
        convocatoria.setTipo(tipo);
        convocatoria.setFechaEvento(fecha);
        convocatoria.setHora(LocalTime.of(9, 0));
        convocatoria.setLugar("Casa comunal");
        convocatoria.setContenido("1. Constatación del quórum\n2. Informe de la directiva\n3. Varios");
        convocatoria.setPrioridad(prioridad);
        convocatoria.setFechaPublicacion(publicacion);
        convocatoria.setEstado(estado);
        convocatorias.save(convocatoria);
    }

    private void acta(Junta junta, String titulo, LocalDate fecha, boolean publica) {
        Acta acta = new Acta();
        acta.setJunta(junta);
        acta.setTitulo(titulo);
        acta.setFecha(fecha);
        acta.setTipoEvento(TipoEvento.ASAMBLEA);
        acta.setContenido("Resumen de los puntos tratados");
        acta.setPublicaSocios(publica);
        acta.setDocumentoNombre("pendiente");
        acta = actas.save(acta);
        acta.setDocumentoNombre(archivos.guardarBytes(ArchivoService.ACTAS, "acta-" + acta.getId(), PDF_DEMOSTRACION, "pdf"));
        actas.save(acta);
    }

    private void comunicacion(Junta junta, Usuario secretario, SecuenciaIngreso secuencia, String remitente,
            String asunto, LocalDate fecha, TipoDocumentoExterno tipo, boolean conDocumento) {
        ComunicacionExterna comunicacion = new ComunicacionExterna();
        comunicacion.setJunta(junta);
        comunicacion.setNumeroIngreso(String.format("CE-%d-%04d", secuencia.getAnio(), secuencia.siguiente()));
        comunicacion.setRemitente(remitente);
        comunicacion.setAsunto(asunto);
        comunicacion.setFecha(fecha);
        comunicacion.setTipoDocumento(tipo);
        comunicacion.setRegistradoPor(secretario);
        comunicacion = comunicaciones.save(comunicacion);
        if (conDocumento) {
            comunicacion.setDocumentoNombre(archivos.guardarBytes(ArchivoService.COMUNICACIONES,
                    "comunicacion-" + comunicacion.getId(), PDF_DEMOSTRACION, "pdf"));
            comunicaciones.save(comunicacion);
        }
    }

    private Mensaje mensaje(Conversacion conversacion, Usuario remitente, String contenido, LocalDateTime fecha) {
        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setRemitente(remitente);
        mensaje.setContenido(contenido);
        mensaje.setFechaEnvio(fecha);
        return mensajes.save(mensaje);
    }

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

    private void financieros(Junta junta, Usuario tesorero, List<Socio> activos) {
        LocalDate hoy = LocalDate.now();
        Evento minga = evento(junta, TipoEvento.MINGA, hoy.minusDays(10), LocalTime.of(7, 0), "Acequia matriz",
                "Minga de limpieza");
        asistencia(minga, activos.get(0), EstadoAsistencia.JUSTIFICADO);
        asistencia(minga, activos.get(1), EstadoAsistencia.AUSENTE);
        minga.setAsistenciaCerrada(true);
        eventos.save(minga);
        Cuota primera = null;
        for (Socio socio : activos) {
            Cuota cuota = new Cuota();
            cuota.setJunta(junta);
            cuota.setSocio(socio);
            cuota.setConcepto("Tarifa anual 2026");
            cuota.setMonto(new BigDecimal("25.00"));
            cuota.setSaldoPendiente(new BigDecimal("25.00"));
            cuota.setFecha(hoy.minusDays(20));
            cuota.setFechaVencimiento(hoy.minusDays(5));
            cuota = cuotas.save(cuota);
            if (primera == null) {
                primera = cuota;
            }
        }
        Pago pago = new Pago();
        pago.setJunta(junta);
        pago.setSocio(primera.getSocio());
        pago.setNumero("TX-000001");
        pago.setMonto(new BigDecimal("10.00"));
        pago.setFecha(LocalDateTime.now().minusDays(2));
        pago.setConcepto(primera.getConcepto());
        pago.setMetodo(MetodoPago.EFECTIVO);
        pago.setRegistradoPor(tesorero);
        pago.agregar(primera, new BigDecimal("10.00"));
        cuotas.save(primera);
        pagos.save(pago);
        egreso(junta, tesorero, CategoriaEgreso.MATERIALES, "Tubería PVC para ramal 3", "4.00", "Ferretería Central");
        egreso(junta, tesorero, CategoriaEgreso.SERVICIOS_BASICOS, "Energía de la bomba", "2.50", null);
        item(junta, tesorero, "INV-0001", "Tubería PVC 2 pulg. (6 m)", "unid", "10", "50");
        item(junta, tesorero, "INV-0002", "Válvula de paso 2 pulg.", "unid", "15", "12");
        item(junta, tesorero, "INV-0003", "Medidor de caudal 1/2 pulg.", "unid", "5", "28");
        activo(junta, "AF-" + hoy.getYear() + "-0001", "Bomba sumergible centrífuga 5HP", CategoriaActivo.MAQUINARIA,
                "1450.00", hoy.minusMonths(18));
        activo(junta, "AF-" + hoy.getYear() + "-0002", "Computadora de tesorería", CategoriaActivo.EQUIPO_OFICINA,
                "650.00", hoy.minusMonths(6));
    }

    private void asistencia(Evento evento, Socio socio, EstadoAsistencia estado) {
        Asistencia asistencia = new Asistencia();
        asistencia.setEvento(evento);
        asistencia.setSocio(socio);
        asistencia.setFecha(evento.getFecha());
        asistencia.setEstado(estado);
        asistencias.save(asistencia);
    }

    private void egreso(Junta junta, Usuario tesorero, CategoriaEgreso categoria, String concepto, String monto,
            String proveedor) {
        Egreso egreso = new Egreso();
        egreso.setJunta(junta);
        egreso.setCategoria(categoria);
        egreso.setConcepto(concepto);
        egreso.setMonto(new BigDecimal(monto));
        egreso.setFecha(LocalDate.now().minusDays(1));
        egreso.setProveedor(proveedor);
        egreso.setRegistradoPor(tesorero);
        egresos.save(egreso);
    }

    private void item(Junta junta, Usuario tesorero, String codigo, String nombre, String unidad, String minimo,
            String stock) {
        ItemInventario item = new ItemInventario();
        item.setJunta(junta);
        item.setCodigo(codigo);
        item.setNombre(nombre);
        item.setUnidadMedida(unidad);
        item.setStockMinimo(new BigDecimal(minimo));
        item.setStock(new BigDecimal(stock));
        item = items.save(item);
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setItem(item);
        movimiento.setTipo(TipoMovimiento.ENTRADA);
        movimiento.setCantidad(new BigDecimal(stock));
        movimiento.setMotivo("Compra");
        movimiento.setFecha(LocalDateTime.now().minusDays(3));
        movimiento.setStockResultante(new BigDecimal(stock));
        movimiento.setUsuario(tesorero);
        movimientos.save(movimiento);
    }

    private void activo(Junta junta, String codigo, String nombre, CategoriaActivo categoria, String valor,
            LocalDate fecha) {
        ActivoFijo activo = new ActivoFijo();
        activo.setJunta(junta);
        activo.setCodigo(codigo);
        activo.setNombre(nombre);
        activo.setCategoria(categoria);
        activo.setValorAdquisicion(new BigDecimal(valor));
        activo.setFechaAdquisicion(fecha);
        activo.setEstado(EstadoActivo.NUEVO);
        activosFijos.save(activo);
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

    private Evento evento(Junta junta, TipoEvento tipo, LocalDate fecha, LocalTime hora, String lugar,
            String descripcion) {
        Evento evento = new Evento();
        evento.setJunta(junta);
        evento.setTipo(tipo);
        evento.setFecha(fecha);
        evento.setHora(hora);
        evento.setLugar(lugar);
        evento.setDescripcion(descripcion);
        return eventos.save(evento);
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
        Usuario adminRiego = usuario("admin.sanpedro", "1800000011", "Luis", "Paredes", Rol.ADMIN_JUNTA, riego);
        Usuario secretariaRiego = usuario("secretaria.sanpedro", "1800000012", "María", "Chicaiza", Rol.SECRETARIO, riego);
        Usuario tesoreroRiego = usuario("tesorero.sanpedro", "1800000013", "Jorge", "Villacís", Rol.TESORERO, riego);
        Usuario desactivado = usuario("secretario.anterior", "1800000014", "Ramiro", "Sánchez", Rol.SECRETARIO, riego);
        desactivado.setActivo(false);
        usuarios.save(desactivado);
        Socio rosa = socio(riego, norte, "1800000101", "Rosa", "Masaquiza", "SP-001", true, false, EstadoSocio.ACTIVO);
        Socio manuel = socio(riego, sur, "1800000102", "Manuel", "Toaquiza", "SP-002", false, false, EstadoSocio.ACTIVO);
        socio(riego, norte, "1800000103", "Carmen", "Pilla", "SP-003", false, false, EstadoSocio.INACTIVO);
        administrativos(riego, norte, sur, List.of(rosa, manuel), "-1.241700", "-78.619700");
        financieros(riego, tesoreroRiego, List.of(rosa, manuel));
        comunicacionesDePrueba(riego, secretariaRiego, adminRiego, rosa);

        Junta rosario = junta("Junta Comunitaria El Rosario", TipoJunta.COMUNITARIA, "1890000002001", "San Pedro de Pelileo", true);
        Sector alto = sector(rosario, "Sector Alto");
        Sector bajo = sector(rosario, "Sector Bajo");
        Usuario adminRosario = usuario("admin.rosario", "1800000021", "Diana", "Guevara", Rol.ADMIN_JUNTA, rosario);
        Usuario secretarioRosario = usuario("secretario.rosario", "1800000022", "Pablo", "Freire", Rol.SECRETARIO, rosario);
        Usuario tesoreraRosario = usuario("tesorera.rosario", "1800000023", "Lucía", "Medina", Rol.TESORERO, rosario);
        Socio segundo = socio(rosario, alto, "1800000201", "Segundo", "Caiza", "ER-001", true, true, EstadoSocio.ACTIVO);
        Socio blanca = socio(rosario, bajo, "1800000202", "Blanca", "Tisalema", "ER-002", false, false, EstadoSocio.ACTIVO);
        socio(rosario, alto, "1800000203", "Hugo", "Llerena", "ER-003", false, false, EstadoSocio.INACTIVO);
        administrativos(rosario, alto, bajo, List.of(segundo, blanca), "-1.329600", "-78.543100");
        financieros(rosario, tesoreraRosario, List.of(segundo, blanca));
        comunicacionesDePrueba(rosario, secretarioRosario, adminRosario, segundo);

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
