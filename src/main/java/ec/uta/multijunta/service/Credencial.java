package ec.uta.multijunta.service;

import java.io.Serializable;

public record Credencial(String nombreCompleto, String username, String passwordTemporal) implements Serializable {
}
