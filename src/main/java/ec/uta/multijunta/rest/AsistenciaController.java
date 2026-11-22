package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.EstadoAsistencia;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.service.AsistenciaService;
import ec.uta.multijunta.service.CorreccionAsistenciaService;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/asistencia")
public class AsistenciaController {

    private static final Pattern PARAMETRO_ESTADO = Pattern.compile("estado\\[(\\d+)]");

    private final AsistenciaService asistencia;
    private final CorreccionAsistenciaService correcciones;
    private final UsuarioActual usuarioActual;

    public AsistenciaController(AsistenciaService asistencia, CorreccionAsistenciaService correcciones,
            UsuarioActual usuarioActual) {
        this.asistencia = asistencia;
        this.correcciones = correcciones;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String lista(Model modelo) {
        modelo.addAttribute("filas", asistencia.eventosConAsistencia());
        modelo.addAttribute("puedeTomar", esSecretario());
        return "asistencia/lista";
    }

    @GetMapping("/eventos/{id}")
    public String evento(@PathVariable Long id, Model modelo, RedirectAttributes redireccion) {
        Evento evento = asistencia.evento(id);
        modelo.addAttribute("evento", evento);
        if (evento.isAsistenciaCerrada()) {
            modelo.addAttribute("asistencias", asistencia.asistencias(id));
            modelo.addAttribute("resumen", asistencia.resumen(id));
            modelo.addAttribute("correcciones", correcciones.delEvento(id));
            modelo.addAttribute("puedeSolicitar", esSecretario());
            modelo.addAttribute("estados", EstadoAsistencia.values());
            return "asistencia/detalle";
        }
        if (!esSecretario()) {
            redireccion.addFlashAttribute("error", "asistencia.noAdmite");
            return "redirect:/asistencia";
        }
        try {
            modelo.addAttribute("socios", asistencia.planilla(id));
            modelo.addAttribute("estados", EstadoAsistencia.values());
            return "asistencia/toma";
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
            return "redirect:/asistencia";
        }
    }

    @PostMapping("/eventos/{id}/cerrar")
    public String cerrar(@PathVariable Long id, @RequestParam Map<String, String> parametros,
            RedirectAttributes redireccion) {
        Map<Long, EstadoAsistencia> estados = new HashMap<>();
        parametros.forEach((clave, valor) -> {
            Matcher coincidencia = PARAMETRO_ESTADO.matcher(clave);
            if (coincidencia.matches()) {
                estados.put(Long.valueOf(coincidencia.group(1)), EstadoAsistencia.valueOf(valor));
            }
        });
        try {
            asistencia.cerrar(id, estados);
            redireccion.addFlashAttribute("exito", "asistencia.cerrada");
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        }
        return "redirect:/asistencia/eventos/" + id;
    }

    private boolean esSecretario() {
        return usuarioActual.rol() == Rol.SECRETARIO;
    }
}
