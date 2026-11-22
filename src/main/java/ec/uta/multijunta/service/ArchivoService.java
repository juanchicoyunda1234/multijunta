package ec.uta.multijunta.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ArchivoService {

    public static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

    private static final byte[] FIRMA_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] FIRMA_JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("junta-\\d+-[0-9a-f\\-]{36}\\.(png|jpg)");

    private final Path carpetaLogos;

    public ArchivoService(@Value("${multijunta.uploads:./uploads}") String carpeta) {
        this.carpetaLogos = Paths.get(carpeta, "logos").toAbsolutePath().normalize();
    }

    public String extensionDeLogo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > TAMANO_MAXIMO) {
            throw new ReglaNegocioException("configuracion.logoInvalido", "logo");
        }
        byte[] cabecera = leerCabecera(archivo);
        if (empiezaCon(cabecera, FIRMA_PNG)) {
            return "png";
        }
        if (empiezaCon(cabecera, FIRMA_JPG)) {
            return "jpg";
        }
        throw new ReglaNegocioException("configuracion.logoInvalido", "logo");
    }

    public String guardarLogo(Long juntaId, MultipartFile archivo, String extension, String anterior) {
        String nombre = "junta-" + juntaId + "-" + UUID.randomUUID() + "." + extension;
        try (InputStream entrada = archivo.getInputStream()) {
            Files.createDirectories(carpetaLogos);
            Files.copy(entrada, carpetaLogos.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
            if (anterior != null && NOMBRE_VALIDO.matcher(anterior).matches()) {
                Files.deleteIfExists(carpetaLogos.resolve(anterior));
            }
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
        return nombre;
    }

    public Optional<Path> logo(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            return Optional.empty();
        }
        Path ruta = carpetaLogos.resolve(nombre).normalize();
        return ruta.startsWith(carpetaLogos) && Files.isRegularFile(ruta) ? Optional.of(ruta) : Optional.empty();
    }

    private byte[] leerCabecera(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            return entrada.readNBytes(4);
        } catch (IOException excepcion) {
            throw new ReglaNegocioException("configuracion.logoInvalido", "logo");
        }
    }

    private boolean empiezaCon(byte[] datos, byte[] firma) {
        return datos.length >= firma.length && Arrays.equals(Arrays.copyOf(datos, firma.length), firma);
    }
}
