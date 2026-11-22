package ec.uta.multijunta.rest;

import ec.uta.multijunta.service.ConfiguracionJuntaService;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class LogoController {

    private final ConfiguracionJuntaService configuracion;

    public LogoController(ConfiguracionJuntaService configuracion) {
        this.configuracion = configuracion;
    }

    @GetMapping("/logo/{juntaId}")
    public ResponseEntity<Resource> logo(@PathVariable Long juntaId) {
        Path ruta = configuracion.logo(juntaId);
        MediaType tipo = ruta.toString().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(tipo).body(new FileSystemResource(ruta));
    }
}
