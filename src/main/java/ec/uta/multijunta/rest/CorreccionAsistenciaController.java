package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.CorreccionAsistencia;
import ec.uta.multijunta.model.EstadoAsistencia;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.service.CorreccionAsistenciaService;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(CorreccionAsistenciaService.ENLACE)
public class CorreccionAsistenciaController {

    private final CorreccionAsistenciaService correcciones;
    private final UsuarioActual usuarioActual;

    public CorreccionAsistenciaController(CorreccionAsistenciaService correcciones, UsuarioActual usuarioActual) {
        this.correcciones = correcciones;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String lista(Model modelo) {
        modelo.addAttribute("correcciones", correcciones.listar());
        modelo.addAttribute("puedeResolver", usuarioActual.rol() == Rol.ADMIN_JUNTA);
        return "asistencia/correcciones";
    }

    @PostMapping
    public String solicitar(@RequestParam Long asistenciaId, @RequestParam(required = false) EstadoAsistencia estadoNuevo,
            @RequestParam(defaultValue = "") String motivo, @RequestParam Long eventoId,
            RedirectAttributes redireccion) {
        try {
            correcciones.solicitar(asistenciaId, estadoNuevo, motivo);
            redireccion.addFlashAttribute("exito", "correccion.solicitada");
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        }
        return "redirect:/asistencia/eventos/" + eventoId;
    }

    @PostMapping("/{id}/aprobar")
    public String aprobar(@PathVariable Long id, RedirectAttributes redireccion) {
        return resolver(() -> correcciones.aprobar(id), "correccion.aprobada", redireccion);
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable Long id, RedirectAttributes redireccion) {
        return resolver(() -> correcciones.rechazar(id), "correccion.rechazada", redireccion);
    }

    private String resolver(java.util.function.Supplier<CorreccionAsistencia> accion, String exito,
            RedirectAttributes redireccion) {
        try {
            accion.get();
            redireccion.addFlashAttribute("exito", exito);
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        }
        return "redirect:" + CorreccionAsistenciaService.ENLACE;
    }
}
