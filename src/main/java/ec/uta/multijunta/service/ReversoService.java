package ec.uta.multijunta.service;

import ec.uta.multijunta.model.AplicacionPago;
import ec.uta.multijunta.model.Pago;
import ec.uta.multijunta.model.ReversoPago;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.repository.PagoRepository;
import ec.uta.multijunta.repository.ReversoPagoRepository;
import java.time.LocalDateTime;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReversoService {

    private final PagoRepository pagos;
    private final ReversoPagoRepository reversos;
    private final FinanzasService finanzas;
    private final UsuarioActual usuarioActual;

    public ReversoService(PagoRepository pagos, ReversoPagoRepository reversos, FinanzasService finanzas,
            UsuarioActual usuarioActual) {
        this.pagos = pagos;
        this.reversos = reversos;
        this.finanzas = finanzas;
        this.usuarioActual = usuarioActual;
    }

    @Transactional
    public ReversoPago reversar(Long pagoId, String motivo) {
        Long juntaId = usuarioActual.juntaId();
        Pago pago = pagos.findByIdAndJuntaId(pagoId, juntaId).orElseThrow(RecursoNoEncontradoException::new);
        if (usuarioActual.rol() != Rol.TESORERO) {
            throw new AccessDeniedException("Solo el tesorero puede reversar pagos");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("motivo.obligatorio");
        }
        if (pago.isReversado() || reversos.findByPagoId(pago.getId()).isPresent()) {
            throw new ReglaNegocioException("pago.yaReversado");
        }
        for (AplicacionPago aplicacion : pago.getAplicaciones()) {
            aplicacion.getObligacion().restituir(aplicacion.getMonto());
        }
        ReversoPago reverso = new ReversoPago();
        reverso.setJunta(pago.getJunta());
        reverso.setPago(pago);
        reverso.setNumero(finanzas.siguienteNumero(juntaId));
        reverso.setMonto(pago.getMonto());
        reverso.setFecha(LocalDateTime.now());
        reverso.setMotivo(motivo.trim());
        reverso.setRegistradoPor(usuarioActual.entidad());
        pago.setReversado(true);
        pago.setReverso(reverso);
        ReversoPago guardado = reversos.save(reverso);
        pagos.saveAndFlush(pago);
        return guardado;
    }
}
