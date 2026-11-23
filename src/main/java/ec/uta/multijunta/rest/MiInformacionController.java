package ec.uta.multijunta.rest;

import ec.uta.multijunta.service.ArchivoService;
import ec.uta.multijunta.service.MiInformacionService;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mi-informacion")
public class MiInformacionController {

    private final MiInformacionService informacion;

    public MiInformacionController(MiInformacionService informacion) {
        this.informacion = informacion;
    }

    @GetMapping
    public String resumen(Model modelo) {
        modelo.addAttribute("resumen", informacion.resumen());
        modelo.addAttribute("pestana", "resumen");
        return "socio/resumen";
    }

    @GetMapping("/estado-cuenta")
    public String estadoCuenta(Model modelo) {
        modelo.addAttribute("estado", informacion.estadoCuenta());
        modelo.addAttribute("pestana", "estado-cuenta");
        return "socio/estado-cuenta";
    }

    @GetMapping("/predio")
    public String predio(Model modelo) {
        modelo.addAttribute("predios", informacion.predios());
        modelo.addAttribute("pestana", "predio");
        return "socio/predio";
    }

    @GetMapping("/predio/{id}/foto")
    public ResponseEntity<Resource> foto(@PathVariable Long id) {
        Path ruta = informacion.foto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ArchivoService.tipoContenido(ruta.getFileName().toString())))
                .body(new FileSystemResource(ruta));
    }

    @GetMapping("/calendario")
    public String calendario(Model modelo) {
        modelo.addAttribute("turnos", informacion.calendario());
        modelo.addAttribute("pestana", "calendario");
        return "socio/calendario";
    }
}
