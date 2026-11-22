package ec.uta.multijunta.config;

import org.springframework.security.authentication.AccountStatusException;

public class JuntaSuspendidaException extends AccountStatusException {

    public JuntaSuspendidaException(String mensaje) {
        super(mensaje);
    }
}
