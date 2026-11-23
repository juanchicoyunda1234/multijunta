package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.Convocatoria;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.PrioridadConvocatoria;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.TipoConvocatoria;
import ec.uta.multijunta.rest.formulario.ConvocatoriaFormulario;
import ec.uta.multijunta.service.ConvocatoriaService;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import jakarta.validation.Valid;
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
@RequestMapping("/convocatorias")
public class ConvocatoriaController {

    private static final String LISTA = "redirect:/convocatorias";

    private final ConvocatoriaService convocatorias;
    private final UsuarioActual usuarioActual;

    public ConvocatoriaController(ConvocatoriaService convocatorias, UsuarioActual usuarioActual) {
        this.convocatorias = convocatorias;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String lista(Model modelo) {
        modelo.addAttribute("convocatorias", convocatorias.listar());
        modelo.addAttribute("puedeEditar", usuarioActual.rol() == Rol.SECRETARIO);
        return "convocatorias/lista";
    }

    @GetMapping("/nueva")
    public String nueva(@RequestParam(required = false) Long eventoId, Model modelo) {
        ConvocatoriaFormulario formulario = new ConvocatoriaFormulario();
        if (eventoId != null) {
            Evento evento = convocatorias.evento(eventoId);
            formulario.setTipo(ConvocatoriaService.tipoDe(evento));
            formulario.setFechaEvento(evento.getFecha());
            formulario.setHora(evento.getHora());
            formulario.setLugar(evento.getLugar());
            formulario.setTitulo(formulario.getTipo().getEtiqueta() + " del " + evento.getFecha());
        }
        modelo.addAttribute("convocatoriaFormulario", formulario);
        return formulario(modelo, null);
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("convocatoriaFormulario") ConvocatoriaFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                Convocatoria convocatoria = convocatorias.crear(formulario.datos());
                redireccion.addFlashAttribute("exito", convocatoria.isBorrador()
                        ? "convocatoria.programada" : "convocatoria.publicada");
                return LISTA;
            } catch (ReglaNegocioException excepcion) {
                resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
            }
        }
        return formulario(modelo, null);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model modelo, RedirectAttributes redireccion) {
        try {
            modelo.addAttribute("convocatoriaFormulario", ConvocatoriaFormulario.desde(convocatorias.borrador(id)));
            return formulario(modelo, id);
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
            return LISTA;
        }
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
            @Valid @ModelAttribute("convocatoriaFormulario") ConvocatoriaFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        convocatorias.obtener(id);
        if (!resultado.hasErrors()) {
            try {
                Convocatoria convocatoria = convocatorias.editar(id, formulario.datos());
                redireccion.addFlashAttribute("exito", convocatoria.isBorrador()
                        ? "convocatoria.actualizada" : "convocatoria.publicada");
                return LISTA;
            } catch (ReglaNegocioException excepcion) {
                if (excepcion.getCampo() == null) {
                    redireccion.addFlashAttribute("error", excepcion.getClave());
                    return LISTA;
                }
                resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
            }
        }
        return formulario(modelo, id);
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redireccion) {
        try {
            convocatorias.eliminar(id);
            redireccion.addFlashAttribute("exito", "convocatoria.eliminada");
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        }
        return LISTA;
    }

    private String formulario(Model modelo, Long id) {
        modelo.addAttribute("tipos", TipoConvocatoria.values());
        modelo.addAttribute("prioridades", PrioridadConvocatoria.values());
        modelo.addAttribute("convocatoriaId", id);
        return "convocatorias/formulario";
    }
}
