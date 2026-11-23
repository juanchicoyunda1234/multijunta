package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Conversacion;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Mensaje;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.ConversacionRepository;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.MensajeRepository;
import ec.uta.multijunta.repository.SocioRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MensajeService {

    public record Contacto(Long conversacionId, Long usuarioId, String nombre, String detalle, String ultimo,
                           long noLeidos) {
    }

    public record Burbuja(Mensaje mensaje, boolean propia, String autor) {
    }

    public record Vista(Conversacion conversacion, String titulo, List<Burbuja> burbujas) {
    }

    public static final Set<Rol> DIRECTIVA = Set.of(Rol.ADMIN_JUNTA, Rol.SECRETARIO, Rol.TESORERO);

    private static final Locale ESPANOL = Locale.forLanguageTag("es");
    private static final int VISTA_PREVIA = 60;

    private final ConversacionRepository conversaciones;
    private final MensajeRepository mensajes;
    private final UsuarioRepository usuarios;
    private final SocioRepository socios;
    private final JuntaRepository juntas;
    private final ArchivoService archivos;
    private final NotificacionService notificaciones;
    private final MessageSource textos;
    private final UsuarioActual usuarioActual;

    public MensajeService(ConversacionRepository conversaciones, MensajeRepository mensajes, UsuarioRepository usuarios,
            SocioRepository socios, JuntaRepository juntas, ArchivoService archivos, NotificacionService notificaciones,
            MessageSource textos, UsuarioActual usuarioActual) {
        this.conversaciones = conversaciones;
        this.mensajes = mensajes;
        this.usuarios = usuarios;
        this.socios = socios;
        this.juntas = juntas;
        this.archivos = archivos;
        this.notificaciones = notificaciones;
        this.textos = textos;
        this.usuarioActual = usuarioActual;
    }

    public static boolean esDirectiva(Rol rol) {
        return DIRECTIVA.contains(rol);
    }

    @Transactional
    public List<Contacto> contactos(String texto) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        Long usuarioId = usuarioActual.id();
        if (usuarioActual.rol() == Rol.SOCIO) {
            Conversacion propia = conversacionDeSocio(socioActual());
            return List.of(contacto(propia, null, directiva(), "", usuarioId));
        }
        exigirDirectiva();
        Long juntaId = usuarioActual.juntaId();
        List<Contacto> lista = new ArrayList<>();
        Set<Long> conConversacion = new HashSet<>();
        for (Conversacion conversacion : conversaciones.findByJuntaIdAndSocioIsNotNullOrderByActualizadaDesc(juntaId)) {
            Socio socio = conversacion.getSocio();
            conConversacion.add(socio.getId());
            lista.add(contacto(conversacion, socio.getId(), socio.getNombreCompleto(), "Socio · " + socio.getCedula(),
                    usuarioId));
        }
        for (Conversacion conversacion : conversaciones.internasDe(usuarioId)) {
            Usuario otro = otroMiembro(conversacion, usuarioId);
            if (otro.isActivo()) {
                conConversacion.add(otro.getId());
                lista.add(contacto(conversacion, otro.getId(), otro.getNombreCompleto(), otro.getRol().getEtiqueta(),
                        usuarioId));
            }
        }
        socios.findAllByJuntaIdOrderByApellidosAscNombresAsc(juntaId).stream()
                .filter(socio -> socio.isNotificable() && !conConversacion.contains(socio.getId()))
                .forEach(socio -> lista.add(new Contacto(null, socio.getId(), socio.getNombreCompleto(),
                        "Socio · " + socio.getCedula(), "", 0)));
        miembrosActivos(juntaId).stream()
                .filter(miembro -> !miembro.getId().equals(usuarioId) && !conConversacion.contains(miembro.getId()))
                .forEach(miembro -> lista.add(new Contacto(null, miembro.getId(), miembro.getNombreCompleto(),
                        miembro.getRol().getEtiqueta(), "", 0)));
        return lista.stream()
                .filter(contacto -> filtro.isEmpty() || (contacto.nombre() + " " + contacto.detalle())
                        .toLowerCase(Locale.ROOT).contains(filtro))
                .toList();
    }

    @Transactional
    public Vista abrir(Long conversacionId) {
        Conversacion conversacion = participante(conversacionId);
        Long usuarioId = usuarioActual.id();
        marcarLeido(conversacion, usuarioId, mensajes.ultimoId(conversacion.getId()));
        return vista(conversacion, usuarioId);
    }

    @Transactional
    public Vista burbujas(Long conversacionId) {
        return abrir(conversacionId);
    }

    @Transactional
    public Mensaje enviar(Long conversacionId, String contenido, MultipartFile imagen) {
        Conversacion conversacion = participante(conversacionId);
        String texto = contenido == null ? "" : contenido.trim();
        boolean conImagen = ArchivoService.tieneArchivo(imagen);
        if (texto.isEmpty() && !conImagen) {
            throw new ReglaNegocioException("mensaje.vacio");
        }
        String extension = conImagen ? archivos.validarImagen(imagen, "archivo.grande", "mensaje.imagenTipo", "imagen") : null;
        Usuario remitente = usuarioActual.entidad();
        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setRemitente(remitente);
        mensaje.setContenido(texto.isEmpty() ? null : texto);
        mensaje.setFechaEnvio(LocalDateTime.now());
        Mensaje guardado = mensajes.save(mensaje);
        if (extension != null) {
            guardado.setImagenNombre(archivos.guardar(ArchivoService.MENSAJES, "mensaje-" + guardado.getId(), imagen,
                    extension, null));
        }
        conversacion.setActualizada(guardado.getFechaEnvio());
        marcarLeido(conversacion, remitente.getId(), guardado.getId());
        notificar(conversacion, remitente, guardado);
        return guardado;
    }

    @Transactional
    public Long iniciar(Long usuarioId) {
        if (usuarioActual.rol() == Rol.SOCIO) {
            if (!usuarioId.equals(usuarioActual.id())) {
                Usuario destino = usuarios.findById(usuarioId).orElseThrow(RecursoNoEncontradoException::new);
                if (destino.getRol() == Rol.SOCIO) {
                    throw new AccessDeniedException("Un socio solo puede escribir a la directiva");
                }
            }
            return conversacionDeSocio(socioActual()).getId();
        }
        exigirDirectiva();
        Long juntaId = usuarioActual.juntaId();
        Usuario destino = usuarios.findByIdAndJuntaId(usuarioId, juntaId).orElseThrow(RecursoNoEncontradoException::new);
        if (destino.getId().equals(usuarioActual.id()) || !destino.isActivo()) {
            throw new RecursoNoEncontradoException();
        }
        if (destino.getRol() == Rol.SOCIO) {
            Socio socio = socios.findByIdAndJuntaId(destino.getId(), juntaId).orElseThrow(RecursoNoEncontradoException::new);
            return conversacionDeSocio(socio).getId();
        }
        if (!esDirectiva(destino.getRol())) {
            throw new RecursoNoEncontradoException();
        }
        Long menor = Math.min(destino.getId(), usuarioActual.id());
        Long mayor = Math.max(destino.getId(), usuarioActual.id());
        return conversaciones.findByMiembroAIdAndMiembroBId(menor, mayor).orElseGet(() -> {
            Conversacion nueva = new Conversacion();
            nueva.setJunta(junta(juntaId));
            nueva.setMiembroA(usuarios.findById(menor).orElseThrow(RecursoNoEncontradoException::new));
            nueva.setMiembroB(usuarios.findById(mayor).orElseThrow(RecursoNoEncontradoException::new));
            return conversaciones.save(nueva);
        }).getId();
    }

    @Transactional(readOnly = true)
    public Path imagen(Long mensajeId) {
        Mensaje mensaje = mensajes.findById(mensajeId).orElseThrow(RecursoNoEncontradoException::new);
        participante(mensaje.getConversacion().getId());
        return archivos.leer(ArchivoService.MENSAJES, mensaje.getImagenNombre())
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public long noLeidos() {
        return usuarioActual.sesion().map(sesion -> {
            if (sesion.getRol() == Rol.SOCIO) {
                return mensajes.noLeidosDelSocio(sesion.getId());
            }
            if (esDirectiva(sesion.getRol()) && sesion.getJuntaId() != null) {
                return mensajes.noLeidosDeSociosParaDirectiva(sesion.getJuntaId())
                        + mensajes.noLeidosInternos(sesion.getId());
            }
            return 0L;
        }).orElse(0L);
    }

    private Conversacion participante(Long conversacionId) {
        Conversacion conversacion = conversaciones.findById(conversacionId).orElseThrow(RecursoNoEncontradoException::new);
        Long usuarioId = usuarioActual.id();
        Long juntaId = usuarioActual.juntaId();
        if (juntaId == null || !conversacion.getJunta().getId().equals(juntaId)) {
            throw new RecursoNoEncontradoException();
        }
        boolean permitido;
        if (conversacion.isConSocio()) {
            permitido = conversacion.getSocio().getId().equals(usuarioId) || esDirectiva(usuarioActual.rol());
        } else {
            permitido = conversacion.getMiembroA().getId().equals(usuarioId)
                    || conversacion.getMiembroB().getId().equals(usuarioId);
        }
        if (!permitido) {
            throw new RecursoNoEncontradoException();
        }
        return conversacion;
    }

    private void marcarLeido(Conversacion conversacion, Long usuarioId, long hasta) {
        boolean ladoA = conversacion.isConSocio()
                ? conversacion.getSocio().getId().equals(usuarioId)
                : conversacion.getMiembroA().getId().equals(usuarioId);
        if (ladoA) {
            conversacion.setUltimoLeidoA(Math.max(conversacion.getUltimoLeidoA(), hasta));
        } else {
            conversacion.setUltimoLeidoB(Math.max(conversacion.getUltimoLeidoB(), hasta));
        }
        conversaciones.save(conversacion);
    }

    private Vista vista(Conversacion conversacion, Long usuarioId) {
        boolean soySocio = conversacion.isConSocio() && conversacion.getSocio().getId().equals(usuarioId);
        String titulo = conversacion.isConSocio()
                ? (soySocio ? directiva() : conversacion.getSocio().getNombreCompleto())
                : otroMiembro(conversacion, usuarioId).getNombreCompleto();
        List<Burbuja> burbujas = mensajes.findByConversacionIdOrderByIdAsc(conversacion.getId()).stream()
                .map(mensaje -> {
                    Usuario remitente = mensaje.getRemitente();
                    boolean propia = remitente.getId().equals(usuarioId);
                    String autor = remitente.getNombreCompleto();
                    if (soySocio && !propia) {
                        autor = directiva() + " · " + remitente.getNombreCompleto();
                    }
                    return new Burbuja(mensaje, propia, autor);
                })
                .toList();
        return new Vista(conversacion, titulo, burbujas);
    }

    private Contacto contacto(Conversacion conversacion, Long usuarioId, String nombre, String detalle, Long lector) {
        String ultimo = mensajes.findFirstByConversacionIdOrderByIdDesc(conversacion.getId())
                .map(mensaje -> mensaje.getContenido() == null ? "Imagen" : recortar(mensaje.getContenido()))
                .orElse("");
        long noLeidos;
        if (conversacion.isConSocio()) {
            noLeidos = conversacion.getSocio().getId().equals(lector)
                    ? mensajes.noLeidosEn(conversacion.getId(), conversacion.getUltimoLeidoA(), lector)
                    : mensajes.noLeidosDe(conversacion.getId(), conversacion.getUltimoLeidoB(), conversacion.getSocio().getId());
        } else {
            long desde = conversacion.getMiembroA().getId().equals(lector)
                    ? conversacion.getUltimoLeidoA() : conversacion.getUltimoLeidoB();
            noLeidos = mensajes.noLeidosEn(conversacion.getId(), desde, lector);
        }
        return new Contacto(conversacion.getId(), usuarioId, nombre, detalle, ultimo, noLeidos);
    }

    private void notificar(Conversacion conversacion, Usuario remitente, Mensaje mensaje) {
        String titulo = textos.getMessage("notificacion.mensaje.titulo", null, ESPANOL);
        String detalle = "De " + remitente.getNombreCompleto() + ": "
                + (mensaje.getContenido() == null ? "Imagen" : recortar(mensaje.getContenido()));
        String enlace = "/mensajes/" + conversacion.getId();
        Stream<Usuario> destinatarios;
        if (conversacion.isConSocio()) {
            Socio socio = conversacion.getSocio();
            destinatarios = socio.getId().equals(remitente.getId())
                    ? miembrosActivos(conversacion.getJunta().getId()).stream()
                    : Stream.of(socio).filter(Socio::isNotificable).map(Usuario.class::cast);
        } else {
            destinatarios = Stream.of(otroMiembro(conversacion, remitente.getId())).filter(Usuario::isActivo);
        }
        destinatarios.forEach(destino -> notificaciones.crear(destino, titulo, detalle, enlace));
    }

    private List<Usuario> miembrosActivos(Long juntaId) {
        List<Usuario> miembros = new ArrayList<>();
        for (Rol rol : List.of(Rol.ADMIN_JUNTA, Rol.SECRETARIO, Rol.TESORERO)) {
            miembros.addAll(usuarios.findByJuntaIdAndRolAndActivoTrue(juntaId, rol));
        }
        return miembros;
    }

    private Conversacion conversacionDeSocio(Socio socio) {
        return conversaciones.findBySocioId(socio.getId()).orElseGet(() -> {
            Conversacion nueva = new Conversacion();
            nueva.setJunta(socio.getJunta());
            nueva.setSocio(socio);
            return conversaciones.save(nueva);
        });
    }

    private Socio socioActual() {
        return socios.findByIdAndJuntaId(usuarioActual.id(), usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    private Usuario otroMiembro(Conversacion conversacion, Long usuarioId) {
        return conversacion.getMiembroA().getId().equals(usuarioId) ? conversacion.getMiembroB() : conversacion.getMiembroA();
    }

    private Junta junta(Long juntaId) {
        return juntas.findById(juntaId).orElseThrow(RecursoNoEncontradoException::new);
    }

    private void exigirDirectiva() {
        if (!esDirectiva(usuarioActual.rol())) {
            throw new AccessDeniedException("Operación no permitida para el rol");
        }
    }

    private String directiva() {
        return textos.getMessage("mensaje.directiva", null, ESPANOL);
    }

    private static String recortar(String texto) {
        return texto.length() > VISTA_PREVIA ? texto.substring(0, VISTA_PREVIA) + "…" : texto;
    }
}
