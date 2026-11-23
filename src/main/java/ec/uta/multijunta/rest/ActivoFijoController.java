package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.CategoriaActivo;
import ec.uta.multijunta.model.EstadoActivo;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.rest.formulario.ActivoFormulario;
import ec.uta.multijunta.service.ActivoFijoService;
import ec.uta.multijunta.service.ArchivoService;
import ec.uta.multijunta.service.RecursoNoEncontradoException;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.List;
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
@RequestMapping("/activos")
public class ActivoFijoController {

    private final ActivoFijoService activos;
    private final UsuarioActual usuarioActual;

    public ActivoFijoController(ActivoFijoService activos, UsuarioActual usuarioActual) {
        this.activos = activos;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String lista(Model modelo) {
        return mostrar(modelo, null);
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute("activoFormulario") ActivoFormulario formulario,
            BindingResult resultado, @RequestParam(required = false) MultipartFile archivo, Model modelo,
            RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                activos.registrar(formulario.datos(), archivo);
                redireccion.addFlashAttribute("exito", "activo.registrado");
                return "redirect:/activos";
            } catch (ReglaNegocioException excepcion) {
                if ("archivo".equals(excepcion.getCampo()) || excepcion.getCampo() == null) {
                    resultado.reject(excepcion.getClave());
                } else {
                    resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
                }
            }
        }
        return mostrar(modelo, "nuevo");
    }

    @PostMapping("/{id}/baja")
    public String darDeBaja(@PathVariable Long id, @RequestParam(required = false) String motivo,
            RedirectAttributes redireccion) {
        try {
            activos.darDeBaja(id, motivo);
            redireccion.addFlashAttribute("exito", "activo.baja");
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        }
        return "redirect:/activos";
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> archivo(@PathVariable Long id) {
        Path ruta = activos.archivo(id).orElseThrow(RecursoNoEncontradoException::new);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ArchivoService.tipoContenido(ruta.getFileName().toString())))
                .body(new FileSystemResource(ruta));
    }

    private String mostrar(Model modelo, String abrir) {
        modelo.addAttribute("activos", activos.listar());
        modelo.addAttribute("patrimonio", activos.patrimonio());
        modelo.addAttribute("categorias", CategoriaActivo.values());
        modelo.addAttribute("estados", List.of(EstadoActivo.NUEVO, EstadoActivo.USADO));
        modelo.addAttribute("puedeEditar", usuarioActual.rol() == Rol.TESORERO);
        modelo.addAttribute("abrir", abrir);
        if (!modelo.containsAttribute("activoFormulario")) {
            modelo.addAttribute("activoFormulario", new ActivoFormulario());
        }
        return "activos/lista";
    }
}
