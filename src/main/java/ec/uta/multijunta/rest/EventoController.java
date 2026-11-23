package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.rest.formulario.EventoFormulario;
import ec.uta.multijunta.service.EventoService;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
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
@RequestMapping("/eventos")
public class EventoController {

    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    private final EventoService eventos;
    private final UsuarioActual usuarioActual;

    public EventoController(EventoService eventos, UsuarioActual usuarioActual) {
        this.eventos = eventos;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String calendario(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            Model modelo) {
        return mostrar(modelo, mes == null ? YearMonth.now() : mes, null, null);
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("eventoFormulario") EventoFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        if (resultado.hasErrors()) {
            return mostrar(modelo, mesDe(formulario), "nuevo", null);
        }
        Evento evento = eventos.crear(formulario.datos());
        redireccion.addFlashAttribute("exito", "evento.creado");
        if (evento.getTipo() == TipoEvento.ASAMBLEA || evento.getTipo() == TipoEvento.MINGA) {
            redireccion.addFlashAttribute("ofrecerConvocatoria", evento.getId());
        }
        return "redirect:/eventos?mes=" + YearMonth.from(evento.getFecha());
    }

    @PostMapping("/{id}")
    public String editar(@PathVariable Long id,
            @Valid @ModelAttribute("edicionFormulario") EventoFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        Evento evento = eventos.obtener(id);
        if (resultado.hasErrors()) {
            return mostrar(modelo, YearMonth.from(evento.getFecha()), "edicion", id);
        }
        try {
            Evento editado = eventos.editar(id, formulario.datos());
            redireccion.addFlashAttribute("exito", "evento.actualizado");
            return "redirect:/eventos?mes=" + YearMonth.from(editado.getFecha());
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
            return "redirect:/eventos?mes=" + YearMonth.from(evento.getFecha());
        }
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes redireccion) {
        Evento evento = eventos.obtener(id);
        try {
            eventos.cancelar(id);
            redireccion.addFlashAttribute("exito", "evento.cancelado");
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        }
        return "redirect:/eventos?mes=" + YearMonth.from(evento.getFecha());
    }

    private YearMonth mesDe(EventoFormulario formulario) {
        return formulario.getFecha() == null ? YearMonth.now() : YearMonth.from(formulario.getFecha());
    }

    private String mostrar(Model modelo, YearMonth mes, String abrir, Long edicionId) {
        List<Evento> delMes = eventos.delMes(mes);
        Map<Long, Boolean> tomables = delMes.stream()
                .collect(Collectors.toMap(Evento::getId, eventos::admiteToma));
        modelo.addAttribute("eventos", delMes);
        modelo.addAttribute("tomables", tomables);
        modelo.addAttribute("mes", mes);
        modelo.addAttribute("mesTexto", mes.format(DateTimeFormatter.ofPattern("MMMM yyyy", ESPANOL)));
        modelo.addAttribute("mesAnterior", mes.minusMonths(1));
        modelo.addAttribute("mesSiguiente", mes.plusMonths(1));
        modelo.addAttribute("tipos", TipoEvento.values());
        modelo.addAttribute("puedeEditar", usuarioActual.rol() == Rol.SECRETARIO);
        modelo.addAttribute("hoy", LocalDate.now());
        modelo.addAttribute("abrir", abrir);
        modelo.addAttribute("edicionId", edicionId);
        if (!modelo.containsAttribute("eventoFormulario")) {
            modelo.addAttribute("eventoFormulario", new EventoFormulario());
        }
        if (!modelo.containsAttribute("edicionFormulario")) {
            modelo.addAttribute("edicionFormulario", new EventoFormulario());
        }
        return "eventos/calendario";
    }
}
