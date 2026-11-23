package ec.uta.multijunta.rest;

import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.service.Montos;
import ec.uta.multijunta.service.MultaService;
import ec.uta.multijunta.service.ReglaNegocioException;
import ec.uta.multijunta.service.UsuarioActual;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/multas")
public class MultaController {

    private static final Pattern PARAMETRO_MONTO = Pattern.compile("monto\\[(\\d+)]");
    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    private final MultaService multas;
    private final UsuarioActual usuarioActual;
    private final MessageSource mensajes;

    public MultaController(MultaService multas, UsuarioActual usuarioActual, MessageSource mensajes) {
        this.multas = multas;
        this.usuarioActual = usuarioActual;
        this.mensajes = mensajes;
    }

    @GetMapping
    public String lista(Model modelo) {
        modelo.addAttribute("multas", multas.listar());
        modelo.addAttribute("puedeSincronizar", esTesorero());
        return "multas/lista";
    }

    @GetMapping("/sincronizar")
    public String sincronizar(Model modelo) {
        return mostrar(modelo, Map.of(), null);
    }

    @PostMapping("/sincronizar")
    public String aprobar(@RequestParam(required = false) List<Long> seleccion,
            @RequestParam Map<String, String> parametros, Model modelo, RedirectAttributes redireccion) {
        Map<Long, String> textos = new HashMap<>();
        Map<Long, BigDecimal> montos = new HashMap<>();
        parametros.forEach((nombre, valor) -> {
            Matcher coincidencia = PARAMETRO_MONTO.matcher(nombre);
            if (coincidencia.matches()) {
                Long asistenciaId = Long.valueOf(coincidencia.group(1));
                textos.put(asistenciaId, valor);
                try {
                    montos.put(asistenciaId, new BigDecimal(valor.trim()));
                } catch (NumberFormatException excepcion) {
                    montos.put(asistenciaId, null);
                }
            }
        });
        try {
            MultaService.Resultado resultado = multas.aprobar(seleccion, montos);
            redireccion.addFlashAttribute("exitoTexto", mensajes.getMessage("multa.generadas",
                    new Object[] {resultado.cantidad(), Montos.texto(resultado.total())}, ESPANOL));
            return "redirect:/multas";
        } catch (ReglaNegocioException excepcion) {
            modelo.addAttribute("error", excepcion.getClave());
            return mostrar(modelo, textos, seleccion);
        }
    }

    private String mostrar(Model modelo, Map<Long, String> textos, List<Long> seleccion) {
        modelo.addAttribute("pendientes", multas.pendientes());
        modelo.addAttribute("eventosSinCerrar", multas.eventosSinCerrar());
        modelo.addAttribute("montoEstandar", multas.montoEstandar().toPlainString());
        modelo.addAttribute("montos", textos);
        modelo.addAttribute("seleccion", seleccion);
        modelo.addAttribute("puedeAprobar", esTesorero());
        return "multas/sincronizar";
    }

    private boolean esTesorero() {
        return usuarioActual.rol() == Rol.TESORERO;
    }
}
