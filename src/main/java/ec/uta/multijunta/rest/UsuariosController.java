package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.rest.formulario.DirectivaFormulario;
import ec.uta.multijunta.service.Credencial;
import ec.uta.multijunta.service.CuentaSocioService;
import ec.uta.multijunta.service.DirectivaService;
import ec.uta.multijunta.service.ReglaNegocioException;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/usuarios")
public class UsuariosController {

    private static final String SOCIOS = "redirect:/usuarios/socios";
    private static final String DIRECTIVA = "redirect:/usuarios/directiva";

    private final CuentaSocioService cuentasSocios;
    private final DirectivaService directiva;

    public UsuariosController(CuentaSocioService cuentasSocios, DirectivaService directiva) {
        this.cuentasSocios = cuentasSocios;
        this.directiva = directiva;
    }

    @GetMapping
    public String inicio() {
        return SOCIOS;
    }

    @GetMapping("/socios")
    public String socios(Model modelo) {
        modelo.addAttribute("socios", cuentasSocios.listar());
        return "usuarios/socios";
    }

    @PostMapping("/socios/credenciales")
    public String crearCredenciales(@RequestParam(name = "socioIds", required = false) List<Long> ids,
            RedirectAttributes redireccion) {
        try {
            redireccion.addFlashAttribute("credenciales", cuentasSocios.crearCredenciales(ids));
            return "redirect:/usuarios/credenciales";
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
            return SOCIOS;
        }
    }

    @GetMapping("/credenciales")
    public String credenciales(Model modelo) {
        return modelo.containsAttribute("credenciales") ? "usuarios/credenciales" : SOCIOS;
    }

    @PostMapping("/socios/{id}/desactivar")
    public String desactivarSocio(@PathVariable Long id, RedirectAttributes redireccion) {
        cuentasSocios.desactivar(id);
        redireccion.addFlashAttribute("exito", "socios.desactivada");
        return SOCIOS;
    }

    @PostMapping("/socios/{id}/reactivar")
    public String reactivarSocio(@PathVariable Long id, RedirectAttributes redireccion) {
        cuentasSocios.reactivar(id);
        redireccion.addFlashAttribute("exito", "socios.reactivada");
        return SOCIOS;
    }

    @GetMapping("/directiva")
    public String directiva(Model modelo) {
        return mostrarDirectiva(modelo, null, null);
    }

    @PostMapping("/directiva")
    public String crear(@Valid @ModelAttribute("directivaFormulario") DirectivaFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                Credencial credencial = directiva.crear(formulario.datos(formulario.getRol()));
                redireccion.addFlashAttribute("credencial", credencial);
                redireccion.addFlashAttribute("exito", "directiva.creada");
                return DIRECTIVA;
            } catch (ReglaNegocioException excepcion) {
                resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
            }
        }
        return mostrarDirectiva(modelo, "nueva", null);
    }

    @PostMapping("/directiva/{id}")
    public String editar(@PathVariable Long id,
            @Valid @ModelAttribute("edicionFormulario") DirectivaFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        Usuario usuario = directiva.buscarGestionable(id);
        formulario.setCedula(usuario.getCedula());
        if (!resultado.hasErrors() || soloErrorDeCedula(resultado)) {
            try {
                directiva.editar(id, formulario.datos(formulario.getRol()));
                redireccion.addFlashAttribute("exito", "directiva.actualizada");
                return DIRECTIVA;
            } catch (ReglaNegocioException excepcion) {
                resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
            }
        }
        return mostrarDirectiva(modelo, "edicion", id);
    }

    @PostMapping("/directiva/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes redireccion) {
        directiva.cambiarEstado(id, false);
        redireccion.addFlashAttribute("exito", "directiva.desactivada");
        return DIRECTIVA;
    }

    @PostMapping("/directiva/{id}/reactivar")
    public String reactivar(@PathVariable Long id, RedirectAttributes redireccion) {
        directiva.cambiarEstado(id, true);
        redireccion.addFlashAttribute("exito", "directiva.reactivada");
        return DIRECTIVA;
    }

    private boolean soloErrorDeCedula(BindingResult resultado) {
        return resultado.getErrorCount() == resultado.getFieldErrorCount("cedula");
    }

    private String mostrarDirectiva(Model modelo, String abrir, Long edicionId) {
        modelo.addAttribute("cuentas", directiva.listar());
        modelo.addAttribute("roles", DirectivaService.ROLES_GESTIONABLES);
        modelo.addAttribute("gestionables", DirectivaService.ROLES_GESTIONABLES);
        modelo.addAttribute("abrir", abrir);
        modelo.addAttribute("edicionId", edicionId);
        if (!modelo.containsAttribute("directivaFormulario")) {
            modelo.addAttribute("directivaFormulario", new DirectivaFormulario());
        }
        if (!modelo.containsAttribute("edicionFormulario")) {
            modelo.addAttribute("edicionFormulario", new DirectivaFormulario());
        }
        return "usuarios/directiva";
    }
}
