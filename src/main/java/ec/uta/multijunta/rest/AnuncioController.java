package ec.uta.multijunta.rest;

import ec.uta.multijunta.service.AnuncioService;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/anuncios")
public class AnuncioController {

    private final AnuncioService anuncios;

    public AnuncioController(AnuncioService anuncios) {
        this.anuncios = anuncios;
    }

    @GetMapping
    public String anuncios(Model modelo) {
        modelo.addAttribute("convocatorias", anuncios.anuncios());
        modelo.addAttribute("pestana", "anuncios");
        return "socio/anuncios";
    }

    @GetMapping("/repositorio")
    public String repositorio(Model modelo) {
        modelo.addAttribute("actas", anuncios.repositorio());
        modelo.addAttribute("pestana", "repositorio");
        return "socio/repositorio";
    }

    @GetMapping("/repositorio/{id}/documento")
    public ResponseEntity<Resource> documento(@PathVariable Long id) {
        Path ruta = anuncios.documento(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("acta-" + id + ".pdf").build().toString())
                .body(new FileSystemResource(ruta));
    }
}
