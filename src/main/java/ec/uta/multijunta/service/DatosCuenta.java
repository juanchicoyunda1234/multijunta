package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Rol;

public record DatosCuenta(String cedula, String nombres, String apellidos, String email, Rol rol) {
}
