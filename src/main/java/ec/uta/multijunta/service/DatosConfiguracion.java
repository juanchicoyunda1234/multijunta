package ec.uta.multijunta.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public record DatosConfiguracion(String telefono, String direccion, BigDecimal montoMultaInasistencia,
                                 MultipartFile logo, List<String> sectoresNuevos, List<Long> sectoresQuitados) {
}
