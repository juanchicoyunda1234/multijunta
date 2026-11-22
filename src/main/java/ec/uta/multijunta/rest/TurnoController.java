package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.rest.formulario.TurnoFormulario;
import ec.uta.multijunta.service.NominaService;
import ec.uta.multijunta.service.RecursoService;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.TurnoService;
import ec.uta.multijunta.service.UsuarioActual;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/turnos")
public class TurnoController {

    private final TurnoService turnos;
    private final RecursoService recursos;
    private final NominaService nomina;
    private final UsuarioActual usuarioActual;

    public TurnoController(TurnoService turnos, RecursoService recursos, NominaService nomina,
            UsuarioActual usuarioActual) {
        this.turnos = turnos;
        this.recursos = recursos;
        this.nomina = nomina;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String semana(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
            Model modelo) {
        return mostrar(modelo, semana == null ? LocalDate.now() : semana, null, null);
    }

    @PostMapping
    public String agendar(@Valid @ModelAttribute("turnoFormulario") TurnoFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        LocalDate fecha = formulario.getFecha() == null ? LocalDate.now() : formulario.getFecha();
        if (!resultado.hasErrors()) {
            try {
                turnos.agendar(formulario.datos());
                redireccion.addFlashAttribute("exito", "turno.agendado");
                return "redirect:/turnos?semana=" + TurnoService.lunesDe(fecha);
            } catch (ReglaNegocioException excepcion) {
                resultado.reject(excepcion.getClave());
            }
        }
        return mostrar(modelo, fecha, "agendar", null);
    }

    @PostMapping("/{id}")
    public String reprogramar(@PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime horaInicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime horaFin,
            Model modelo, RedirectAttributes redireccion) {
        turnos.obtener(id);
        try {
            turnos.reprogramar(id, fecha, horaInicio, horaFin);
            redireccion.addFlashAttribute("exito", "turno.reprogramado");
            return "redirect:/turnos?semana=" + TurnoService.lunesDe(fecha);
        } catch (ReglaNegocioException excepcion) {
            modelo.addAttribute("error", excepcion.getClave());
            return mostrar(modelo, fecha == null ? LocalDate.now() : fecha, "reprogramar", id);
        }
    }

    private String mostrar(Model modelo, LocalDate fecha, String abrir, Long reprogramarId) {
        LocalDate lunes = TurnoService.lunesDe(fecha);
        modelo.addAttribute("dias", turnos.semana(lunes));
        modelo.addAttribute("lunes", lunes);
        modelo.addAttribute("semanaAnterior", lunes.minusWeeks(1));
        modelo.addAttribute("semanaSiguiente", lunes.plusWeeks(1));
        modelo.addAttribute("hoy", LocalDate.now());
        modelo.addAttribute("sectores", nomina.sectores());
        modelo.addAttribute("recursos", recursos.activos());
        modelo.addAttribute("puedeEditar", usuarioActual.rol() == Rol.SECRETARIO);
        modelo.addAttribute("abrir", abrir);
        modelo.addAttribute("reprogramarId", reprogramarId);
        if (!modelo.containsAttribute("turnoFormulario")) {
            modelo.addAttribute("turnoFormulario", new TurnoFormulario());
        }
        return "turnos/semana";
    }
}
