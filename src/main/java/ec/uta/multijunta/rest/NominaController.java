package ec.uta.multijunta.rest;

import ec.uta.multijunta.config.MatrizPermisos;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.rest.formulario.SocioFormulario;
import ec.uta.multijunta.service.NominaService;
import ec.uta.multijunta.service.PredioService;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
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
@RequestMapping("/socios")
public class NominaController {

    private static final String FORMULARIO = "socios/formulario";

    private final NominaService nomina;
    private final PredioService predios;
    private final UsuarioActual usuarioActual;

    public NominaController(NominaService nomina, PredioService predios, UsuarioActual usuarioActual) {
        this.nomina = nomina;
        this.predios = predios;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String padron(@RequestParam(defaultValue = "") String q, Model modelo) {
        modelo.addAttribute("socios", nomina.buscar(q));
        modelo.addAttribute("q", q);
        modelo.addAttribute("puedeEditar", esSecretario());
        return "socios/padron";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model modelo) {
        exigirSecretario();
        modelo.addAttribute("socioFormulario", new SocioFormulario());
        return mostrarFormulario(modelo, null);
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute("socioFormulario") SocioFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                nomina.registrar(formulario.datos());
                redireccion.addFlashAttribute("exito", "socio.registrado");
                return "redirect:/socios";
            } catch (ReglaNegocioException excepcion) {
                resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
            }
        }
        return mostrarFormulario(modelo, null);
    }

    @GetMapping("/{id}")
    public String perfil(@PathVariable Long id, Model modelo) {
        Socio socio = nomina.obtener(id);
        boolean verPredios = MatrizPermisos.puedeVer(usuarioActual.rol(), "/predios");
        modelo.addAttribute("socio", socio);
        modelo.addAttribute("puedeEditar", esSecretario());
        modelo.addAttribute("verPredios", verPredios);
        modelo.addAttribute("predios", verPredios ? predios.delSocio(id) : List.of());
        modelo.addAttribute("estados", EstadoSocio.values());
        return "socios/perfil";
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable Long id, Model modelo) {
        exigirSecretario();
        modelo.addAttribute("socioFormulario", SocioFormulario.desde(nomina.obtener(id)));
        return mostrarFormulario(modelo, id);
    }

    @PostMapping("/{id}")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("socioFormulario") SocioFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        Socio socio = nomina.obtener(id);
        formulario.setCedula(socio.getCedula());
        if (resultado.getErrorCount() == resultado.getFieldErrorCount("cedula")) {
            try {
                nomina.editar(id, formulario.datos());
                redireccion.addFlashAttribute("exito", "socio.actualizado");
                return "redirect:/socios/" + id;
            } catch (ReglaNegocioException excepcion) {
                resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
            }
        }
        return mostrarFormulario(modelo, id);
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam EstadoSocio estado,
            RedirectAttributes redireccion) {
        nomina.cambiarEstado(id, estado);
        redireccion.addFlashAttribute("exito", "socio.estadoActualizado");
        return "redirect:/socios/" + id;
    }

    private String mostrarFormulario(Model modelo, Long socioId) {
        modelo.addAttribute("sectores", nomina.sectores());
        modelo.addAttribute("socioId", socioId);
        return FORMULARIO;
    }

    private boolean esSecretario() {
        return usuarioActual.rol() == Rol.SECRETARIO;
    }

    private void exigirSecretario() {
        if (!esSecretario()) {
            throw new AccessDeniedException("Solo el Secretario/a modifica la nómina");
        }
    }
}
