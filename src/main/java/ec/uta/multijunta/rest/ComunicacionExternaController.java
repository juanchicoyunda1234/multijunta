package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.ComunicacionExterna;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.TipoDocumentoExterno;
import ec.uta.multijunta.rest.formulario.ComunicacionFormulario;
import ec.uta.multijunta.service.ArchivoService;
import ec.uta.multijunta.service.ComunicacionExternaService;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
@RequestMapping("/comunicaciones")
public class ComunicacionExternaController {

    private static final String LISTA = "redirect:/comunicaciones";
    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    private final ComunicacionExternaService comunicaciones;
    private final UsuarioActual usuarioActual;
    private final MessageSource mensajes;

    public ComunicacionExternaController(ComunicacionExternaService comunicaciones, UsuarioActual usuarioActual,
            MessageSource mensajes) {
        this.comunicaciones = comunicaciones;
        this.usuarioActual = usuarioActual;
        this.mensajes = mensajes;
    }

    @GetMapping
    public String lista(Model modelo) {
        return mostrar(modelo, null);
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute("comunicacionFormulario") ComunicacionFormulario formulario,
            BindingResult resultado, @RequestParam(required = false) MultipartFile documento, Model modelo,
            RedirectAttributes redireccion) {
        if (!resultado.hasErrors()) {
            try {
                ComunicacionExterna comunicacion = comunicaciones.registrar(formulario.datos(), documento);
                redireccion.addFlashAttribute("exitoTexto", mensajes.getMessage("comunicacion.registrada",
                        new Object[] {comunicacion.getNumeroIngreso()}, ESPANOL));
                return LISTA;
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

    @PostMapping("/{id}/documento")
    public String adjuntar(@PathVariable Long id, @RequestParam(required = false) MultipartFile documento,
            RedirectAttributes redireccion) {
        try {
            comunicaciones.adjuntar(id, documento);
            redireccion.addFlashAttribute("exito", "comunicacion.adjuntada");
        } catch (ReglaNegocioException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getClave());
        }
        return LISTA;
    }

    @GetMapping("/{id}/documento")
    public ResponseEntity<Resource> documento(@PathVariable Long id) {
        Path ruta = comunicaciones.documento(id);
        String nombre = ruta.getFileName().toString();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ArchivoService.tipoContenido(nombre)))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("comunicacion-" + id + nombre.substring(nombre.lastIndexOf('.'))).build().toString())
                .body(new FileSystemResource(ruta));
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redireccion) {
        comunicaciones.eliminar(id);
        redireccion.addFlashAttribute("exito", "comunicacion.eliminada");
        return LISTA;
    }

    private String mostrar(Model modelo, String abrir) {
        modelo.addAttribute("comunicaciones", comunicaciones.listar());
        modelo.addAttribute("tipos", TipoDocumentoExterno.values());
        modelo.addAttribute("puedeEditar", usuarioActual.rol() == Rol.SECRETARIO);
        modelo.addAttribute("abrir", abrir);
        if (!modelo.containsAttribute("comunicacionFormulario")) {
            modelo.addAttribute("comunicacionFormulario", new ComunicacionFormulario());
        }
        return "comunicaciones/lista";
    }
}
