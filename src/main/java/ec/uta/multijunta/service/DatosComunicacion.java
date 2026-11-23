package ec.uta.multijunta.service;

import ec.uta.multijunta.model.TipoDocumentoExterno;
import java.time.LocalDate;

public record DatosComunicacion(String remitente, String asunto, LocalDate fecha, TipoDocumentoExterno tipoDocumento) {
}
