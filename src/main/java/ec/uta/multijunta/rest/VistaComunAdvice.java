package ec.uta.multijunta.rest;

import ec.uta.multijunta.config.MatrizPermisos;
import ec.uta.multijunta.config.UsuarioAutenticado;
import ec.uta.multijunta.service.ConfiguracionJuntaService;
import ec.uta.multijunta.service.MenuService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.support.RequestContextUtils;
import ec.uta.multijunta.service.NotificacionService;
import ec.uta.multijunta.service.RecursoNoEncontradoException;
import ec.uta.multijunta.service.UsuarioActual;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class VistaComunAdvice {

    private final UsuarioActual usuarioActual;
    private final MenuService menuService;
    private final NotificacionService notificacionService;
    private final ConfiguracionJuntaService configuracion;

    public VistaComunAdvice(UsuarioActual usuarioActual, MenuService menuService,
            NotificacionService notificacionService, ConfiguracionJuntaService configuracion) {
        this.usuarioActual = usuarioActual;
        this.menuService = menuService;
        this.notificacionService = notificacionService;
        this.configuracion = configuracion;
    }

    @ModelAttribute("sesion")
    public UsuarioAutenticado sesion() {
        return usuarioActual.sesion().orElse(null);
    }

    @ModelAttribute("menu")
    public List<MatrizPermisos.Entrada> menu() {
        return usuarioActual.sesion().map(sesion -> menuService.opciones(sesion.getRol())).orElse(List.of());
    }

    @ModelAttribute("notificacionesNoLeidas")
    public long notificacionesNoLeidas() {
        return notificacionService.contarNoLeidas();
    }

    @ModelAttribute("logoJunta")
    public boolean logoJunta() {
        return configuracion.tieneLogo();
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado() {
        return "error/404";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoMuyGrande(HttpServletRequest request) {
        Object original = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String uri = original != null ? original.toString() : request.getRequestURI();
        String ruta = uri.substring(request.getContextPath().length());
        boolean esPredio = ruta.startsWith("/predios");
        RequestContextUtils.getOutputFlashMap(request)
                .put("error", esPredio ? "predio.fotoGrande" : "configuracion.logoInvalido");
        return esPredio ? "redirect:/socios" : "redirect:/junta/configuracion";
    }
}
