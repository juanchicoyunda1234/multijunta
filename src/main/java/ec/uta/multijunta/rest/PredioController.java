package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.Predio;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.rest.formulario.PredioFormulario;
import ec.uta.multijunta.service.PredioService;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.Set;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/predios")
public class PredioController {

    private static final String FORMULARIO = "predios/formulario";
    private static final Set<String> CAMPOS = Set.of("area", "sectorId", "ubicacion", "latitud", "longitud", "foto");

    private final PredioService predios;
    private final UsuarioActual usuarioActual;

    public PredioController(PredioService predios, UsuarioActual usuarioActual) {
        this.predios = predios;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public String inicio() {
        return "redirect:/socios";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam Long socioId, Model modelo) {
        exigirSecretario();
        modelo.addAttribute("predioFormulario", new PredioFormulario());
        return mostrar(modelo, socioId, null);
    }

    @PostMapping
    public String crear(@RequestParam Long socioId,
            @Valid @ModelAttribute("predioFormulario") PredioFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                predios.crear(socioId, formulario.datos());
                redireccion.addFlashAttribute("exito", "predio.guardado");
                return "redirect:/socios/" + socioId;
            } catch (ReglaNegocioException excepcion) {
                rechazar(resultado, excepcion);
            }
        }
        return mostrar(modelo, socioId, null);
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable Long id, Model modelo) {
        exigirSecretario();
        Predio predio = predios.obtener(id);
        modelo.addAttribute("predioFormulario", PredioFormulario.desde(predio));
        return mostrar(modelo, predio.getSocio().getId(), predio);
    }

    @PostMapping("/{id}")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("predioFormulario") PredioFormulario formulario,
            BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
        Predio predio = predios.obtener(id);
        if (!resultado.hasErrors()) {
            try {
                predios.editar(id, formulario.datos());
                redireccion.addFlashAttribute("exito", "predio.guardado");
                return "redirect:/socios/" + predio.getSocio().getId();
            } catch (ReglaNegocioException excepcion) {
                rechazar(resultado, excepcion);
            }
        }
        return mostrar(modelo, predio.getSocio().getId(), predio);
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> foto(@PathVariable Long id) {
        Path ruta = predios.foto(id);
        MediaType tipo = ruta.toString().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(tipo).body(new FileSystemResource(ruta));
    }

    private void rechazar(BindingResult resultado, ReglaNegocioException excepcion) {
        if (CAMPOS.contains(excepcion.getCampo())) {
            resultado.rejectValue(excepcion.getCampo(), excepcion.getClave());
        } else {
            resultado.reject(excepcion.getClave());
        }
    }

    private String mostrar(Model modelo, Long socioId, Predio predio) {
        modelo.addAttribute("socio", predios.socio(socioId));
        modelo.addAttribute("sectores", predios.sectores());
        modelo.addAttribute("predio", predio);
        return FORMULARIO;
    }

    private void exigirSecretario() {
        if (usuarioActual.rol() != Rol.SECRETARIO) {
            throw new AccessDeniedException("Solo el Secretario/a registra predios");
        }
    }
}
