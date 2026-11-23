package ec.uta.multijunta.config;

import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UsuarioAutenticado implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Rol rol;
    private final String nombreCompleto;
    private final Long juntaId;
    private final String juntaNombre;
    private final boolean activo;
    private final boolean juntaActiva;
    private final LocalDateTime bloqueadoHasta;

    private UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.username = usuario.getUsername();
        this.password = usuario.getPassword();
        this.rol = usuario.getRol();
        this.nombreCompleto = usuario.getNombreCompleto();
        this.juntaId = usuario.getJunta() == null ? null : usuario.getJunta().getId();
        this.juntaNombre = usuario.getJunta() == null ? null : usuario.getJunta().getNombre();
        this.activo = usuario.isPuedeIngresar();
        this.juntaActiva = usuario.getJunta() == null || usuario.getJunta().isActiva();
        this.bloqueadoHasta = usuario.getBloqueadoHasta();
    }

    public static UsuarioAutenticado desde(Usuario usuario) {
        return new UsuarioAutenticado(usuario);
    }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public Long getId() {
        return id;
    }

    public Rol getRol() {
        return rol;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public Long getJuntaId() {
        return juntaId;
    }

    public String getJuntaNombre() {
        return juntaNombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public boolean isJuntaActiva() {
        return juntaActiva;
    }
}
