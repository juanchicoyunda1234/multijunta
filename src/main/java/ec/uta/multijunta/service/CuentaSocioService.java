package ec.uta.multijunta.service;

import ec.uta.multijunta.model.EstadoCuenta;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.repository.SocioRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuentaSocioService {

    private final SocioRepository socios;
    private final PasswordService passwords;
    private final UsuarioActual usuarioActual;

    public CuentaSocioService(SocioRepository socios, PasswordService passwords, UsuarioActual usuarioActual) {
        this.socios = socios;
        this.passwords = passwords;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Socio> listar() {
        return socios.findAllByJuntaIdOrderByApellidosAscNombresAsc(usuarioActual.juntaId());
    }

    @Transactional
    public List<Credencial> crearCredenciales(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ReglaNegocioException("socios.seleccionVacia");
        }
        List<Socio> seleccionados = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(ids)) {
            seleccionados.add(buscar(id));
        }
        List<Credencial> credenciales = new ArrayList<>();
        for (Socio socio : seleccionados) {
            if (socio.getEstado() != EstadoSocio.ACTIVO || socio.getEstadoCuenta() != EstadoCuenta.SIN_CUENTA) {
                continue;
            }
            String temporal = passwords.generarTemporal();
            socio.setUsername(socio.getCedula());
            socio.setPassword(passwords.codificar(temporal));
            socio.setTieneCuenta(true);
            socio.setActivo(true);
            socio.setDebeCambiarPassword(true);
            socio.setIntentosFallidos(0);
            socio.setBloqueadoHasta(null);
            socios.save(socio);
            credenciales.add(new Credencial(socio.getNombreCompleto(), socio.getUsername(), temporal));
        }
        return credenciales;
    }

    @Transactional
    public void desactivar(Long id) {
        Socio socio = buscar(id);
        if (socio.getEstadoCuenta() == EstadoCuenta.ACTIVA) {
            socio.setActivo(false);
            socios.save(socio);
        }
    }

    @Transactional
    public void reactivar(Long id) {
        Socio socio = buscar(id);
        if (socio.getEstadoCuenta() == EstadoCuenta.DESACTIVADA) {
            socio.setActivo(true);
            socios.save(socio);
        }
    }

    private Socio buscar(Long id) {
        return socios.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }
}
