package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Egreso;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.repository.EgresoRepository;
import ec.uta.multijunta.repository.JuntaRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EgresoService {

    private final EgresoRepository egresos;
    private final JuntaRepository juntas;
    private final ArchivoService archivos;
    private final FinanzasService finanzas;
    private final UsuarioActual usuarioActual;

    public EgresoService(EgresoRepository egresos, JuntaRepository juntas, ArchivoService archivos,
            FinanzasService finanzas, UsuarioActual usuarioActual) {
        this.egresos = egresos;
        this.juntas = juntas;
        this.archivos = archivos;
        this.finanzas = finanzas;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Egreso> listar() {
        return egresos.findByJuntaIdOrderByFechaDescIdDesc(usuarioActual.juntaId());
    }

    @Transactional
    public Egreso registrar(DatosEgreso datos, MultipartFile comprobante) {
        Montos.validar(datos.monto(), "monto");
        String extension = ArchivoService.tieneArchivo(comprobante)
                ? archivos.validarDocumento(comprobante, "comprobante") : null;
        if (datos.monto().compareTo(finanzas.saldoActual()) > 0 && !datos.confirmarNegativo()) {
            throw new ReglaNegocioException("egreso.fondos");
        }
        Junta junta = juntas.findById(usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
        Egreso egreso = new Egreso();
        egreso.setJunta(junta);
        egreso.setCategoria(datos.categoria());
        egreso.setConcepto(datos.concepto().trim());
        egreso.setMonto(Montos.normalizar(datos.monto()));
        egreso.setFecha(datos.fecha());
        egreso.setProveedor(datos.proveedor() == null || datos.proveedor().isBlank() ? null : datos.proveedor().trim());
        egreso.setRegistradoPor(usuarioActual.entidad());
        Egreso guardado = egresos.save(egreso);
        if (extension != null) {
            guardado.setComprobanteNombre(archivos.guardar(ArchivoService.EGRESOS, "egreso-" + guardado.getId(),
                    comprobante, extension, null));
        }
        return guardado;
    }

    @Transactional
    public void adjuntar(Long id, MultipartFile comprobante) {
        Egreso egreso = obtener(id);
        String extension = archivos.validarDocumento(comprobante, "comprobante");
        egreso.setComprobanteNombre(archivos.guardar(ArchivoService.EGRESOS, "egreso-" + egreso.getId(), comprobante,
                extension, egreso.getComprobanteNombre()));
        egresos.save(egreso);
    }

    @Transactional(readOnly = true)
    public Optional<Path> comprobante(Long id) {
        return archivos.leer(ArchivoService.EGRESOS, obtener(id).getComprobanteNombre());
    }

    private Egreso obtener(Long id) {
        return egresos.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }
}
