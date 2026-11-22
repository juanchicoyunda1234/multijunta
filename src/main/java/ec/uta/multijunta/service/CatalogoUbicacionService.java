package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Provincia;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class CatalogoUbicacionService {

    private static final String ARCHIVO = "catalogos/ecuador-dpa.csv";

    private final Map<String, List<String>> catalogo;

    public CatalogoUbicacionService() {
        this.catalogo = Collections.unmodifiableMap(cargar());
    }

    public List<Provincia> provincias() {
        return catalogo.entrySet().stream()
                .map(entrada -> new Provincia(entrada.getKey(), entrada.getValue()))
                .toList();
    }

    public List<String> cantones(String provincia) {
        return catalogo.getOrDefault(provincia, List.of());
    }

    public boolean esValido(String provincia, String canton) {
        return provincia != null && canton != null && cantones(provincia).contains(canton);
    }

    public Map<String, List<String>> mapa() {
        return catalogo;
    }

    private Map<String, List<String>> cargar() {
        Map<String, List<String>> resultado = new LinkedHashMap<>();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(new ClassPathResource(ARCHIVO).getInputStream(), StandardCharsets.UTF_8))) {
            lector.lines().skip(1).filter(linea -> !linea.isBlank()).forEach(linea -> {
                String[] partes = linea.split(";", 2);
                resultado.computeIfAbsent(partes[0].trim(), clave -> new ArrayList<>()).add(partes[1].trim());
            });
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
        resultado.replaceAll((provincia, cantones) -> List.copyOf(cantones));
        return resultado;
    }
}
