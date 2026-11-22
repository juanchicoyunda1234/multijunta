package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordService {

    private static final Pattern FORMATO = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private static final String LETRAS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITOS = "23456789";
    private static final int LONGITUD_TEMPORAL = 10;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final SecureRandom aleatorio = new SecureRandom();

    public PasswordService(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    public boolean formatoValido(String password) {
        return password != null && FORMATO.matcher(password).matches();
    }

    @Transactional
    public void cambiar(Long usuarioId, String actual, String nueva, String confirmacion) {
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(RecursoNoEncontradoException::new);
        if (actual == null || !codificador.matches(actual, usuario.getPassword())) {
            throw new ReglaNegocioException("password.actualIncorrecta");
        }
        if (nueva == null || !nueva.equals(confirmacion)) {
            throw new ReglaNegocioException("password.noCoincide");
        }
        if (!formatoValido(nueva)) {
            throw new ReglaNegocioException("password.formato");
        }
        if (codificador.matches(nueva, usuario.getPassword())) {
            throw new ReglaNegocioException("password.igualAnterior");
        }
        usuario.setPassword(codificador.encode(nueva));
        usuario.setDebeCambiarPassword(false);
        usuarios.save(usuario);
    }

    public String generarTemporal() {
        String todos = LETRAS + DIGITOS;
        List<Character> caracteres = new ArrayList<>();
        caracteres.add(LETRAS.charAt(aleatorio.nextInt(LETRAS.length())));
        caracteres.add(DIGITOS.charAt(aleatorio.nextInt(DIGITOS.length())));
        while (caracteres.size() < LONGITUD_TEMPORAL) {
            caracteres.add(todos.charAt(aleatorio.nextInt(todos.length())));
        }
        Collections.shuffle(caracteres, aleatorio);
        StringBuilder resultado = new StringBuilder();
        caracteres.forEach(resultado::append);
        return resultado.toString();
    }

    public String codificar(String password) {
        return codificador.encode(password);
    }
}
