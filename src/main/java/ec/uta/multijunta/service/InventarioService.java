package ec.uta.multijunta.service;

import ec.uta.multijunta.model.EstadoItem;
import ec.uta.multijunta.model.ItemInventario;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.MovimientoInventario;
import ec.uta.multijunta.model.TipoMovimiento;
import ec.uta.multijunta.repository.ItemInventarioRepository;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.MovimientoInventarioRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventarioService {

    private final ItemInventarioRepository items;
    private final MovimientoInventarioRepository movimientos;
    private final JuntaRepository juntas;
    private final UsuarioActual usuarioActual;

    public InventarioService(ItemInventarioRepository items, MovimientoInventarioRepository movimientos,
            JuntaRepository juntas, UsuarioActual usuarioActual) {
        this.items = items;
        this.movimientos = movimientos;
        this.juntas = juntas;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<ItemInventario> listar() {
        return items.findByJuntaIdOrderByNombreAsc(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public ItemInventario obtener(Long id) {
        return items.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventario> kardex(Long id) {
        return movimientos.findByItemIdOrderByFechaAscIdAsc(obtener(id).getId());
    }

    @Transactional(readOnly = true)
    public long itemsConStockBajo() {
        return items.contarStockBajo(usuarioActual.juntaId());
    }

    @Transactional
    public ItemInventario crearItem(DatosItem datos) {
        Long juntaId = usuarioActual.juntaId();
        String nombre = datos.nombre().trim();
        if (items.existsByJuntaIdAndNombreIgnoreCase(juntaId, nombre)) {
            throw new ReglaNegocioException("inventario.duplicado", "nombre");
        }
        if (datos.stockMinimo() == null || datos.stockMinimo().signum() < 0) {
            throw new ReglaNegocioException("inventario.stockMinimo", "stockMinimo");
        }
        Junta junta = juntas.findById(juntaId).orElseThrow(RecursoNoEncontradoException::new);
        ItemInventario item = new ItemInventario();
        item.setJunta(junta);
        item.setCodigo(String.format("INV-%04d", siguienteNumero(juntaId)));
        item.setNombre(nombre);
        item.setUnidadMedida(datos.unidadMedida().trim());
        item.setStock(BigDecimal.ZERO.setScale(2));
        item.setStockMinimo(datos.stockMinimo().setScale(2));
        item.setEstado(EstadoItem.ACTIVO);
        return items.save(item);
    }

    @Transactional
    public MovimientoInventario registrarMovimiento(DatosMovimiento datos) {
        ItemInventario item = obtener(datos.itemId());
        if (!item.isActivo()) {
            throw new ReglaNegocioException("inventario.inactivo");
        }
        BigDecimal cantidad = datos.cantidad();
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaNegocioException("inventario.cantidad");
        }
        BigDecimal nuevoStock = datos.tipo() == TipoMovimiento.ENTRADA
                ? item.getStock().add(cantidad) : item.getStock().subtract(cantidad);
        if (nuevoStock.signum() < 0) {
            throw new ReglaNegocioException("inventario.stockInsuficiente");
        }
        item.setStock(nuevoStock.setScale(2));
        items.save(item);
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setItem(item);
        movimiento.setTipo(datos.tipo());
        movimiento.setCantidad(cantidad.setScale(2));
        movimiento.setMotivo(datos.motivo().trim());
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setStockResultante(item.getStock());
        movimiento.setUsuario(usuarioActual.entidad());
        return movimientos.save(movimiento);
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo) {
        ItemInventario item = obtener(id);
        item.setEstado(activo ? EstadoItem.ACTIVO : EstadoItem.INACTIVO);
        items.save(item);
    }

    @Transactional
    public void eliminar(Long id) {
        ItemInventario item = obtener(id);
        if (movimientos.existsByItemId(item.getId())) {
            throw new ReglaNegocioException("inventario.conMovimientos");
        }
        items.delete(item);
    }

    private int siguienteNumero(Long juntaId) {
        return items.findByJuntaIdOrderByNombreAsc(juntaId).stream()
                .map(ItemInventario::getCodigo)
                .filter(codigo -> codigo.matches("INV-\\d+"))
                .mapToInt(codigo -> Integer.parseInt(codigo.substring(4)))
                .max().orElse(0) + 1;
    }
}
