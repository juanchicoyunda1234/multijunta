package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.rest.formulario.ActaFormulario;
import ec.uta.multijunta.service.ActaService;
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
@RequestMapping("/actas")
public class ActaController {

    private final ActaService actas;
    private final UsuarioActual usuarioActual;

    public ActaController(ActaService actas, UsuarioActual usuarioActual) {
        this.actas = actas;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String lista(Model modelo) {
        return mostrar(modelo, null);
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("actaFormulario") ActaFormulario formulario, BindingResult resultado,
            @RequestParam(required = false) MultipartFile documento, Model modelo, RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                actas.guardar(formulario.datos(), documento);
                redireccion.addFlashAttribute("exito", "acta.guardada");
                return "redirect:/actas";
            } catch (ReglaNegocioException excepcion) {
                if ("documento".equals(excepcion.getCampo())) {
                    resultado.reject(excepcion.getClave());
                } else {
                    resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
                }
            }
        }
        return mostrar(modelo, "nuevo");
    }

    @PostMapping("/{id}/visibilidad")
    public String visibilidad(@PathVariable Long id, @RequestParam boolean publicaSocios, RedirectAttributes redireccion) {
        actas.cambiarVisibilidad(id, publicaSocios);
        redireccion.addFlashAttribute("exito", "acta.visibilidad");
        return "redirect:/actas";
    }

    @GetMapping("/{id}/documento")
    public ResponseEntity<Resource> documento(@PathVariable Long id) {
        Path ruta = actas.documento(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(new FileSystemResource(ruta));
    }

    private String mostrar(Model modelo, String abrir) {
        modelo.addAttribute("actas", actas.listar());
        modelo.addAttribute("tipos", TipoEvento.values());
        modelo.addAttribute("puedeEditar", usuarioActual.rol() == Rol.SECRETARIO);
        modelo.addAttribute("abrir", abrir);
        if (!modelo.containsAttribute("actaFormulario")) {
            modelo.addAttribute("actaFormulario", new ActaFormulario());
        }
        return "actas/lista";
    }
}
