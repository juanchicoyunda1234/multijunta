package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.TipoMovimiento;
import ec.uta.multijunta.rest.formulario.ItemFormulario;
import ec.uta.multijunta.rest.formulario.MovimientoFormulario;
import ec.uta.multijunta.service.InventarioService;
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
@RequestMapping("/inventario")
public class InventarioController {

    private static final String LISTA = "redirect:/inventario";

    private final InventarioService inventario;
    private final UsuarioActual usuarioActual;

    public InventarioController(InventarioService inventario, UsuarioActual usuarioActual) {
        this.inventario = inventario;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String lista(Model modelo) {
        return mostrar(modelo, null);
    }

    @PostMapping("/items")
    public String crearItem(@Valid @ModelAttribute("itemFormulario") ItemFormulario formulario, BindingResult resultado,
            Model modelo, RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                inventario.crearItem(formulario.datos());
                redireccion.addFlashAttribute("exito", "inventario.itemCreado");
                return LISTA;
            } catch (ReglaNegocioException excepcion) {
                resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
            }
        }
        return mostrar(modelo, "item");
    }

    @PostMapping("/movimientos")
    public String registrarMovimiento(@Valid @ModelAttribute("movimientoFormulario") MovimientoFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        if (formulario.getItemId() != null) {
            inventario.obtener(formulario.getItemId());
        }
        if (!resultado.hasErrors()) {
            try {
                inventario.registrarMovimiento(formulario.datos());
                redireccion.addFlashAttribute("exito", "inventario.movimiento");
                return LISTA;
            } catch (ReglaNegocioException excepcion) {
                resultado.reject(excepcion.getClave());
            }
        }
        return mostrar(modelo, "movimiento");
    }

    @GetMapping("/items/{id}")
    public String kardex(@PathVariable Long id, Model modelo) {
        modelo.addAttribute("item", inventario.obtener(id));
        modelo.addAttribute("movimientos", inventario.kardex(id));
        return "inventario/kardex";
    }

    @PostMapping("/items/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam boolean activo, RedirectAttributes redireccion) {
        inventario.cambiarEstado(id, activo);
        redireccion.addFlashAttribute("exito", "inventario.estado");
        return LISTA;
    }

    @PostMapping("/items/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redireccion) {
        try {
            inventario.eliminar(id);
            redireccion.addFlashAttribute("exito", "inventario.eliminado");
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        }
        return LISTA;
    }

    private String mostrar(Model modelo, String abrir) {
        modelo.addAttribute("items", inventario.listar());
        modelo.addAttribute("tipos", TipoMovimiento.values());
        modelo.addAttribute("puedeEditar", usuarioActual.rol() == Rol.TESORERO);
        modelo.addAttribute("abrir", abrir);
        if (!modelo.containsAttribute("itemFormulario")) {
            modelo.addAttribute("itemFormulario", new ItemFormulario());
        }
        if (!modelo.containsAttribute("movimientoFormulario")) {
            modelo.addAttribute("movimientoFormulario", new MovimientoFormulario());
        }
        return "inventario/lista";
    }
}
