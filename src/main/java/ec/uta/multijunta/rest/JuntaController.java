package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.TipoJunta;
import ec.uta.multijunta.rest.formulario.AdministradorFormulario;
import ec.uta.multijunta.rest.formulario.JuntaFormulario;
import ec.uta.multijunta.service.AdministradorService;
import ec.uta.multijunta.service.CatalogoUbicacionService;
import ec.uta.multijunta.service.Credencial;
import ec.uta.multijunta.service.JuntaService;
import ec.uta.multijunta.service.ReglaNegocioException;
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
@RequestMapping("/plataforma/juntas")
public class JuntaController {

    private static final String VISTA = "plataforma/juntas";
    private static final String LISTADO = "redirect:/plataforma/juntas";

    private final JuntaService juntas;
    private final AdministradorService administradores;
    private final CatalogoUbicacionService catalogo;

    public JuntaController(JuntaService juntas, AdministradorService administradores,
            CatalogoUbicacionService catalogo) {
        this.juntas = juntas;
        this.administradores = administradores;
        this.catalogo = catalogo;
    }

    @GetMapping
    public String listado(Model modelo) {
        return mostrar(modelo, null);
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute("juntaFormulario") JuntaFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                juntas.registrar(formulario.datos());
                redireccion.addFlashAttribute("exito", "junta.registrada");
                return LISTADO;
            } catch (ReglaNegocioException excepcion) {
                resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
            }
        }
        return mostrar(modelo, "registro");
    }

    @PostMapping("/{id}")
    public String editar(@PathVariable Long id,
            @Valid @ModelAttribute("edicionFormulario") JuntaFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                juntas.editar(id, formulario.datos());
                redireccion.addFlashAttribute("exito", "junta.actualizada");
                return LISTADO;
            } catch (ReglaNegocioException excepcion) {
                resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
            }
        }
        modelo.addAttribute("edicionId", juntas.obtener(id).getId());
        return mostrar(modelo, "edicion");
    }

    @PostMapping("/{id}/suspender")
    public String suspender(@PathVariable Long id, @RequestParam(defaultValue = "") String motivo,
            RedirectAttributes redireccion) {
        try {
            juntas.suspender(id, motivo);
            redireccion.addFlashAttribute("exito", "junta.suspendida");
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        }
        return LISTADO;
    }

    @PostMapping("/{id}/reactivar")
    public String reactivar(@PathVariable Long id, RedirectAttributes redireccion) {
        juntas.reactivar(id);
        redireccion.addFlashAttribute("exito", "junta.reactivada");
        return LISTADO;
    }

    @PostMapping("/{id}/administradores")
    public String asignarAdministrador(@PathVariable Long id,
            @Valid @ModelAttribute("administradorFormulario") AdministradorFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        Junta junta = juntas.obtener(id);
        if (!resultado.hasErrors()) {
            try {
                Credencial credencial = administradores.asignar(id, formulario.datos(null),
                        formulario.getReemplazadoId());
                redireccion.addFlashAttribute("credencial", credencial);
                redireccion.addFlashAttribute("credencialJunta", junta.getNombre() + " (RUC: " + junta.getRuc() + ")");
                return LISTADO;
            } catch (ReglaNegocioException excepcion) {
                if (excepcion.getCampo() == null) {
                    resultado.reject(excepcion.getClave());
                } else {
                    resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
                }
            }
        }
        modelo.addAttribute("administradorJunta", junta);
        return mostrar(modelo, "administrador");
    }

    private String mostrar(Model modelo, String abrir) {
        modelo.addAttribute("filas", juntas.listar());
        modelo.addAttribute("tipos", TipoJunta.values());
        modelo.addAttribute("provincias", catalogo.provincias());
        modelo.addAttribute("catalogo", catalogo.mapa());
        modelo.addAttribute("abrir", abrir);
        if (!modelo.containsAttribute("juntaFormulario")) {
            modelo.addAttribute("juntaFormulario", new JuntaFormulario());
        }
        if (!modelo.containsAttribute("edicionFormulario")) {
            modelo.addAttribute("edicionFormulario", new JuntaFormulario());
        }
        if (!modelo.containsAttribute("administradorFormulario")) {
            modelo.addAttribute("administradorFormulario", new AdministradorFormulario());
        }
        return VISTA;
    }
}
