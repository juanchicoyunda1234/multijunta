package ec.uta.multijunta.rest;

import ec.uta.multijunta.service.ArchivoService;
import ec.uta.multijunta.service.MensajeService;
import ec.uta.multijunta.service.ReglaNegocioException;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/mensajes")
public class MensajeController {

    private final MensajeService mensajes;

    public MensajeController(MensajeService mensajes) {
        this.mensajes = mensajes;
    }

    @GetMapping
    public String vista(@RequestParam(required = false) String q, Model modelo) {
        modelo.addAttribute("contactos", mensajes.contactos(q));
        modelo.addAttribute("q", q);
        return "mensajes/vista";
    }

    @GetMapping("/{id}")
    public String conversacion(@PathVariable Long id, @RequestParam(required = false) String q, Model modelo) {
        modelo.addAttribute("vista", mensajes.abrir(id));
        modelo.addAttribute("contactos", mensajes.contactos(q));
        modelo.addAttribute("q", q);
        return "mensajes/vista";
    }

    @GetMapping("/{id}/burbujas")
    public String burbujas(@PathVariable Long id, Model modelo) {
        modelo.addAttribute("vista", mensajes.burbujas(id));
        return "mensajes/burbujas :: burbujas";
    }

    @PostMapping("/{id}")
    public String enviar(@PathVariable Long id, @RequestParam(required = false) String contenido,
            @RequestParam(required = false) MultipartFile imagen, Model modelo) {
        try {
            mensajes.enviar(id, contenido, imagen);
            return "redirect:/mensajes/" + id;
        } catch (ReglaNegocioException excepcion) {
            modelo.addAttribute("error", excepcion.getClave());
            modelo.addAttribute("contenido", contenido);
            return conversacion(id, null, modelo);
        }
    }

    @PostMapping("/nueva")
    public String nueva(@RequestParam Long usuarioId) {
        return "redirect:/mensajes/" + mensajes.iniciar(usuarioId);
    }

    @GetMapping("/imagen/{mensajeId}")
    public ResponseEntity<Resource> imagen(@PathVariable Long mensajeId) {
        Path ruta = mensajes.imagen(mensajeId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ArchivoService.tipoContenido(ruta.getFileName().toString())))
                .body(new FileSystemResource(ruta));
    }
}
