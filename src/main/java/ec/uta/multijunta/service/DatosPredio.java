package ec.uta.multijunta.service;

import java.math.BigDecimal;
import org.springframework.web.multipart.MultipartFile;

public record DatosPredio(Long sectorId, BigDecimal area, String ubicacion, BigDecimal latitud, BigDecimal longitud,
                          MultipartFile foto) {
}
