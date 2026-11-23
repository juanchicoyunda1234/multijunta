package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.EstadoObligacion;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.rest.formulario.CuotaFormulario;
import ec.uta.multijunta.service.CuotaService;
import ec.uta.multijunta.service.DestinoCuota;
import ec.uta.multijunta.service.Montos;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.context.MessageSource;
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
@RequestMapping("/cuotas")
public class CuotaController {

    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    private final CuotaService cuotas;
    private final UsuarioActual usuarioActual;
    private final MessageSource mensajes;

    public CuotaController(CuotaService cuotas, UsuarioActual usuarioActual, MessageSource mensajes) {
        this.cuotas = cuotas;
        this.usuarioActual = usuarioActual;
        this.mensajes = mensajes;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String q, @RequestParam(required = false) EstadoObligacion estado,
            Model modelo) {
        modelo.addAttribute("cuotas", cuotas.buscar(q, estado));
        modelo.addAttribute("q", q);
        modelo.addAttribute("estado", estado);
        modelo.addAttribute("estados", EstadoObligacion.values());
        modelo.addAttribute("puedeEditar", usuarioActual.rol() == Rol.TESORERO);
        return "cuotas/lista";
    }

    @GetMapping("/emitir")
    public String nueva(Model modelo) {
        modelo.addAttribute("cuotaFormulario", new CuotaFormulario());
        return formulario(modelo);
    }

    @PostMapping("/previa")
    public String previa(@Valid @ModelAttribute("cuotaFormulario") CuotaFormulario formulario, BindingResult resultado,
            Model modelo) {
        if (!resultado.hasErrors()) {
            try {
                CuotaService.Previa previa = cuotas.previa(formulario.datos());
                modelo.addAttribute("resumen", texto("cuota.previa", previa.destinatarios().size(),
                        Montos.texto(previa.total())));
                if (previa.omitidos() > 0) {
                    modelo.addAttribute("omitidos", texto("cuota.omitidos", previa.omitidos()));
                }
            } catch (ReglaNegocioException excepcion) {
                rechazar(resultado, excepcion);
            }
        }
        return formulario(modelo);
    }

    @PostMapping
    public String emitir(@Valid @ModelAttribute("cuotaFormulario") CuotaFormulario formulario, BindingResult resultado,
            Model modelo, RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                cuotas.emitir(formulario.datos());
                redireccion.addFlashAttribute("exito", "cuota.emitidas");
                return "redirect:/cuotas";
            } catch (ReglaNegocioException excepcion) {
                rechazar(resultado, excepcion);
            }
        }
        return formulario(modelo);
    }

    @PostMapping("/{id}/anular")
    public String anular(@PathVariable Long id, @RequestParam(required = false) String motivo,
            RedirectAttributes redireccion) {
        try {
            cuotas.anular(id, motivo);
            redireccion.addFlashAttribute("exito", "cuota.anulada");
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        }
        return "redirect:/cuotas";
    }

    private void rechazar(BindingResult resultado, ReglaNegocioException excepcion) {
        if (excepcion.getCampo() == null) {
            resultado.reject(excepcion.getClave());
        } else {
            resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
        }
    }

    private String formulario(Model modelo) {
        modelo.addAttribute("destinos", DestinoCuota.values());
        modelo.addAttribute("sectores", cuotas.sectores());
        modelo.addAttribute("socios", cuotas.socios());
        return "cuotas/emitir";
    }

    private String texto(String clave, Object... argumentos) {
        return mensajes.getMessage(clave, argumentos, ESPANOL);
    }
}
