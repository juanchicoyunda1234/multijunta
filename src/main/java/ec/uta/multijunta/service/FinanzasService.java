package ec.uta.multijunta.service;

import ec.uta.multijunta.repository.CuotaRepository;
import ec.uta.multijunta.repository.EgresoRepository;
import ec.uta.multijunta.repository.MultaRepository;
import ec.uta.multijunta.repository.PagoRepository;
import ec.uta.multijunta.repository.ReversoPagoRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanzasService {

    private final MultaRepository multas;
    private final CuotaRepository cuotas;
    private final PagoRepository pagos;
    private final ReversoPagoRepository reversos;
    private final EgresoRepository egresos;
    private final UsuarioActual usuarioActual;

    public FinanzasService(MultaRepository multas, CuotaRepository cuotas, PagoRepository pagos,
            ReversoPagoRepository reversos, EgresoRepository egresos, UsuarioActual usuarioActual) {
        this.multas = multas;
        this.cuotas = cuotas;
        this.pagos = pagos;
        this.reversos = reversos;
        this.egresos = egresos;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public BigDecimal deudaDeSocio(Long socioId) {
        return multas.deudaDeSocio(socioId).add(cuotas.deudaDeSocio(socioId));
    }

    @Transactional(readOnly = true)
    public BigDecimal deudaPorCobrar() {
        Long juntaId = usuarioActual.juntaId();
        return multas.deudaDeJunta(juntaId).add(cuotas.deudaDeJunta(juntaId));
    }

    @Transactional(readOnly = true)
    public BigDecimal saldoActual() {
        Long juntaId = usuarioActual.juntaId();
        return pagos.ingresosVigentes(juntaId).subtract(egresos.totalDeJunta(juntaId));
    }

    public String siguienteNumero(Long juntaId) {
        long siguiente = pagos.countByJuntaId(juntaId) + reversos.countByJuntaId(juntaId) + 1;
        return String.format("TX-%06d", siguiente);
    }
}
