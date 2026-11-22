package ec.uta.multijunta.config;

import ec.uta.multijunta.service.AutenticacionService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class EventosAutenticacion {

    private final AutenticacionService autenticacion;

    public EventosAutenticacion(AutenticacionService autenticacion) {
        this.autenticacion = autenticacion;
    }

    @EventListener
    public void alFallar(AuthenticationFailureBadCredentialsEvent evento) {
        autenticacion.registrarFallo(evento.getAuthentication().getName());
    }

    @EventListener
    public void alIngresar(AuthenticationSuccessEvent evento) {
        autenticacion.registrarExito(evento.getAuthentication().getName());
    }
}
