package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.MetodoPago;
import ec.uta.multijunta.model.Obligacion;
import ec.uta.multijunta.model.Pago;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.rest.formulario.CobroFormulario;
import ec.uta.multijunta.service.CobranzaService;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.ReversoService;
import ec.uta.multijunta.service.UsuarioActual;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
@RequestMapping("/pagos")
public class CobranzaController {

    private final CobranzaService cobranza;
    private final ReversoService reversos;
    private final UsuarioActual usuarioActual;

    public CobranzaController(CobranzaService cobranza, ReversoService reversos, UsuarioActual usuarioActual) {
        this.cobranza = cobranza;
        this.reversos = reversos;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String cobranzas(@RequestParam(required = false) String q, Model modelo) {
        modelo.addAttribute("filas", cobranza.socios(q));
        modelo.addAttribute("q", q);
        modelo.addAttribute("puedeCobrar", esTesorero());
        return "pagos/cobranzas";
    }

    @GetMapping("/cobrar/{socioId}")
    public String formulario(@PathVariable Long socioId, Model modelo) {
        List<Obligacion> obligaciones = cobranza.obligaciones(socioId);
        CobroFormulario formulario = new CobroFormulario();
        formulario.setObligaciones(obligaciones.stream().map(Obligacion::getClave).toList());
        formulario.setMonto(total(obligaciones));
        modelo.addAttribute("cobroFormulario", formulario);
        return mostrarCobro(modelo, socioId, obligaciones);
    }

    @PostMapping("/cobrar/{socioId}")
    public String cobrar(@PathVariable Long socioId,
            @Valid @ModelAttribute("cobroFormulario") CobroFormulario formulario, BindingResult resultado,
            Model modelo, RedirectAttributes redireccion) {
        List<Obligacion> obligaciones = cobranza.obligaciones(socioId);
        if (!resultado.hasErrors()) {
            try {
                Pago pago = cobranza.cobrar(socioId, formulario.datos());
                redireccion.addFlashAttribute("exito", "pago.registrado");
                return "redirect:/pagos/" + pago.getId();
            } catch (ReglaNegocioException excepcion) {
                if ("pago.confirmarParcial".equals(excepcion.getClave())) {
                    modelo.addAttribute("confirmarParcial", true);
                } else if (excepcion.getCampo() != null) {
                    resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
                } else {
                    resultado.reject(excepcion.getClave());
                }
            } catch (ObjectOptimisticLockingFailureException excepcion) {
                resultado.reject("pago.cambio");
            }
        }
        return mostrarCobro(modelo, socioId, obligaciones);
    }

    @GetMapping("/historial")
    public String historial(@RequestParam(required = false) String q, Model modelo) {
        modelo.addAttribute("pagos", cobranza.historial(q));
        modelo.addAttribute("q", q);
        return "pagos/historial";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model modelo) {
        Pago pago = cobranza.pago(id);
        modelo.addAttribute("pago", pago);
        modelo.addAttribute("deuda", cobranza.deuda(pago.getSocio().getId()));
        modelo.addAttribute("puedeReversar", esTesorero() && !pago.isReversado());
        return "pagos/detalle";
    }

    @PostMapping("/{id}/reversar")
    public String reversar(@PathVariable Long id, @RequestParam(required = false) String motivo,
            RedirectAttributes redireccion) {
        try {
            reversos.reversar(id, motivo);
            redireccion.addFlashAttribute("exito", "pago.reversado");
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        } catch (ObjectOptimisticLockingFailureException excepcion) {
            redireccion.addFlashAttribute("error", "pago.cambio");
        }
        return "redirect:/pagos/" + id;
    }

    @GetMapping("/{id}/recibo")
    public String recibo(@PathVariable Long id, Model modelo) {
        modelo.addAttribute("pago", cobranza.pago(id));
        modelo.addAttribute("junta", cobranza.junta());
        return "pagos/recibo";
    }

    private String mostrarCobro(Model modelo, Long socioId, List<Obligacion> obligaciones) {
        modelo.addAttribute("socio", cobranza.socio(socioId));
        modelo.addAttribute("obligaciones", obligaciones);
        modelo.addAttribute("total", total(obligaciones));
        modelo.addAttribute("metodos", MetodoPago.values());
        return "pagos/cobro";
    }

    private BigDecimal total(List<Obligacion> obligaciones) {
        return obligaciones.stream().map(Obligacion::getSaldoPendiente).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean esTesorero() {
        return usuarioActual.rol() == Rol.TESORERO;
    }
}
