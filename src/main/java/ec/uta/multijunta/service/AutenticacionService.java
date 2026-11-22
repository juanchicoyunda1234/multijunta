package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacionService {

    public static final int MAXIMO_INTENTOS = 5;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private final UsuarioRepository usuarios;

    public AutenticacionService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional
    public void registrarFallo(String username) {
        usuarios.findByUsername(Usuario.normalizarUsername(username)).ifPresent(usuario -> {
            int intentos = usuario.getIntentosFallidos() + 1;
            if (intentos >= MAXIMO_INTENTOS) {
                usuario.setBloqueadoHasta(LocalDateTime.now().plus(DURACION_BLOQUEO));
                usuario.setIntentosFallidos(0);
            } else {
                usuario.setIntentosFallidos(intentos);
            }
            usuarios.save(usuario);
        });
    }

    @Transactional
    public void registrarExito(String username) {
        usuarios.findByUsername(Usuario.normalizarUsername(username)).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuarios.save(usuario);
        });
    }

    @Transactional(readOnly = true)
    public boolean estaBloqueado(String username) {
        return usuarios.findByUsername(Usuario.normalizarUsername(username))
                .map(usuario -> usuario.estaBloqueado(LocalDateTime.now()))
                .orElse(false);
    }
}
