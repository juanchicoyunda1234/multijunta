package ec.uta.multijunta.service;

import ec.uta.multijunta.model.TipoJunta;

public record DatosJunta(String nombre, String ruc, TipoJunta tipo, String provincia, String canton) {
}
