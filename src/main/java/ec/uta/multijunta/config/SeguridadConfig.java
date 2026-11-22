package ec.uta.multijunta.config;

import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UsuarioDetailsService detalles, PasswordEncoder codificador,
            ApplicationEventPublisher publicador) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(detalles);
        proveedor.setPasswordEncoder(codificador);
        proveedor.setPreAuthenticationChecks(this::verificarBloqueo);
        proveedor.setPostAuthenticationChecks(this::verificarEstado);
        ProviderManager administrador = new ProviderManager(proveedor);
        administrador.setAuthenticationEventPublisher(new DefaultAuthenticationEventPublisher(publicador));
        return administrador;
    }

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http, UsuarioRepository usuarios,
            ManejadorFalloLogin manejadorFallo, Environment entorno, AuthenticationManager autenticacion)
            throws Exception {
        boolean desarrollo = entorno.acceptsProfiles(Profiles.of("dev"));
        http.authenticationManager(autenticacion);
        PathPatternRequestMatcher.Builder rutas = PathPatternRequestMatcher.withDefaults();
        http.authorizeHttpRequests(reglas -> {
            reglas.requestMatchers(rutas.matcher("/login"), rutas.matcher("/recuperar"),
                    rutas.matcher("/css/**"), rutas.matcher("/error")).permitAll();
            if (desarrollo) {
                reglas.requestMatchers(PathRequest.toH2Console()).permitAll();
            }
            for (MatrizPermisos.Entrada entrada : MatrizPermisos.ordenadasPorEspecificidad()) {
                for (String patron : entrada.patrones()) {
                    reglas.requestMatchers(rutas.matcher(HttpMethod.GET, patron))
                            .hasAnyRole(nombres(entrada.conLectura()));
                    reglas.requestMatchers(rutas.matcher(patron)).hasAnyRole(nombres(entrada.completo()));
                }
            }
            reglas.anyRequest().authenticated();
        });
        http.formLogin(login -> login
                .loginPage("/login")
                .defaultSuccessUrl("/inicio", true)
                .failureHandler(manejadorFallo)
                .permitAll());
        http.logout(salida -> salida
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?salio")
                .deleteCookies("JSESSIONID")
                .permitAll());
        http.sessionManagement(sesion -> sesion.invalidSessionUrl("/login?expirada"));
        http.exceptionHandling(errores -> errores.accessDeniedHandler(new ManejadorAccesoDenegado()));
        if (desarrollo) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers(PathRequest.toH2Console()));
            http.headers(cabeceras -> cabeceras.frameOptions(marcos -> marcos.sameOrigin()));
        }
        http.addFilterBefore(new EstadoCuentaFilter(usuarios), AuthorizationFilter.class);
        http.addFilterBefore(new CambioPasswordFilter(usuarios), AuthorizationFilter.class);
        return http.build();
    }

    private void verificarBloqueo(UserDetails detalles) {
        if (detalles instanceof UsuarioAutenticado usuario && usuario.estaBloqueado(LocalDateTime.now())) {
            throw new LockedException("Cuenta bloqueada temporalmente");
        }
    }

    private void verificarEstado(UserDetails detalles) {
        if (detalles instanceof UsuarioAutenticado usuario) {
            if (!usuario.isActivo()) {
                throw new DisabledException("Cuenta desactivada");
            }
            if (!usuario.isJuntaActiva()) {
                throw new JuntaSuspendidaException("Junta suspendida");
            }
        }
    }

    private static String[] nombres(Set<Rol> roles) {
        return roles.stream().map(Rol::name).toArray(String[]::new);
    }
}
