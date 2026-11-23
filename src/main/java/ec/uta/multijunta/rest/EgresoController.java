package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.CategoriaEgreso;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.rest.formulario.EgresoFormulario;
import ec.uta.multijunta.service.ArchivoService;
import ec.uta.multijunta.service.EgresoService;
import ec.uta.multijunta.service.FinanzasService;
import ec.uta.multijunta.service.RecursoNoEncontradoException;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import jakarta.validation.Valid;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/egresos")
public class EgresoController {

    private final EgresoService egresos;
    private final FinanzasService finanzas;
    private final UsuarioActual usuarioActual;

    public EgresoController(EgresoService egresos, FinanzasService finanzas, UsuarioActual usuarioActual) {
        this.egresos = egresos;
        this.finanzas = finanzas;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String lista(Model modelo) {
        return mostrar(modelo, null);
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute("egresoFormulario") EgresoFormulario formulario,
            BindingResult resultado, @RequestParam(required = false) MultipartFile comprobante, Model modelo,
            RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                egresos.registrar(formulario.datos(), comprobante);
                redireccion.addFlashAttribute("exito", "egreso.registrado");
                return "redirect:/egresos";
            } catch (ReglaNegocioException excepcion) {
                if ("egreso.fondos".equals(excepcion.getClave())) {
                    modelo.addAttribute("confirmarNegativo", true);
                } else if (excepcion.getCampo() != null && !"comprobante".equals(excepcion.getCampo())) {
                    resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
                } else {
                    resultado.reject(excepcion.getClave());
                }
            }
        }
        return mostrar(modelo, "nuevo");
    }

    @PostMapping("/{id}/comprobante")
    public String adjuntar(@PathVariable Long id, @RequestParam(required = false) MultipartFile comprobante,
            RedirectAttributes redireccion) {
        try {
            egresos.adjuntar(id, comprobante);
            redireccion.addFlashAttribute("exito", "egreso.soporteAdjuntado");
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        }
        return "redirect:/egresos";
    }

    @GetMapping("/{id}/comprobante")
    public ResponseEntity<Resource> comprobante(@PathVariable Long id) {
        Path ruta = egresos.comprobante(id).orElseThrow(RecursoNoEncontradoException::new);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ArchivoService.tipoContenido(ruta.getFileName().toString())))
                .body(new FileSystemResource(ruta));
    }

    private String mostrar(Model modelo, String abrir) {
        modelo.addAttribute("egresos", egresos.listar());
        modelo.addAttribute("saldo", finanzas.saldoActual());
        modelo.addAttribute("categorias", CategoriaEgreso.values());
        modelo.addAttribute("puedeEditar", usuarioActual.rol() == Rol.TESORERO);
        modelo.addAttribute("abrir", abrir);
        if (!modelo.containsAttribute("egresoFormulario")) {
            modelo.addAttribute("egresoFormulario", new EgresoFormulario());
        }
        return "egresos/lista";
    }
}
