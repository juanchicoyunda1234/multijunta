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
    public static final String LOGOS = "logos";
    public static final String PREDIOS = "predios";
    public static final String EGRESOS = "egresos";
    public static final String ACTIVOS = "activos";

    private static final byte[] FIRMA_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] FIRMA_JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PDF = {0x25, 0x50, 0x44, 0x46};
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[a-z]+-\\d+-[0-9a-f\\-]{36}\\.(png|jpg|pdf)");

    private final Path raiz;

    public ArchivoService(@Value("${multijunta.uploads:./uploads}") String carpeta) {
        this.raiz = Paths.get(carpeta).toAbsolutePath().normalize();
    }

    public String validarImagen(MultipartFile archivo, String claveTamano, String claveTipo, String campo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException(claveTipo, campo);
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new ReglaNegocioException(claveTamano, campo);
        }
        byte[] cabecera = leerCabecera(archivo, claveTipo, campo);
        if (empiezaCon(cabecera, FIRMA_PNG)) {
            return "png";
        }
        if (empiezaCon(cabecera, FIRMA_JPG)) {
            return "jpg";
        }
        throw new ReglaNegocioException(claveTipo, campo);
    }

    public String validarDocumento(MultipartFile archivo, String campo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("archivo.documentoTipo", campo);
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new ReglaNegocioException("archivo.grande", campo);
        }
        byte[] cabecera = leerCabecera(archivo, "archivo.documentoTipo", campo);
        if (empiezaCon(cabecera, FIRMA_PDF)) {
            return "pdf";
        }
        if (empiezaCon(cabecera, FIRMA_PNG)) {
            return "png";
        }
        if (empiezaCon(cabecera, FIRMA_JPG)) {
            return "jpg";
        }
        throw new ReglaNegocioException("archivo.documentoTipo", campo);
    }

    public static boolean tieneArchivo(MultipartFile archivo) {
        return archivo != null && !archivo.isEmpty();
    }

    public static String tipoContenido(String nombre) {
        if (nombre.endsWith(".pdf")) {
            return "application/pdf";
        }
        return nombre.endsWith(".png") ? "image/png" : "image/jpeg";
    }

    public String extensionDeLogo(MultipartFile archivo) {
        return validarImagen(archivo, "configuracion.logoInvalido", "configuracion.logoInvalido", "logo");
    }

    public String guardar(String subcarpeta, String prefijo, MultipartFile archivo, String extension, String anterior) {
        Path carpeta = carpeta(subcarpeta);
        String nombre = prefijo + "-" + UUID.randomUUID() + "." + extension;
        try (InputStream entrada = archivo.getInputStream()) {
            Files.createDirectories(carpeta);
            Files.copy(entrada, carpeta.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
            if (anterior != null && NOMBRE_VALIDO.matcher(anterior).matches()) {
                Files.deleteIfExists(carpeta.resolve(anterior));
            }
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
        return nombre;
    }

    public Optional<Path> leer(String subcarpeta, String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            return Optional.empty();
        }
        Path carpeta = carpeta(subcarpeta);
        Path ruta = carpeta.resolve(nombre).normalize();
        return ruta.startsWith(carpeta) && Files.isRegularFile(ruta) ? Optional.of(ruta) : Optional.empty();
    }

    public String guardarLogo(Long juntaId, MultipartFile archivo, String extension, String anterior) {
        return guardar(LOGOS, "junta-" + juntaId, archivo, extension, anterior);
    }

    public Optional<Path> logo(String nombre) {
        return leer(LOGOS, nombre);
    }

    private Path carpeta(String subcarpeta) {
        return raiz.resolve(subcarpeta).normalize();
    }

    private byte[] leerCabecera(MultipartFile archivo, String claveTipo, String campo) {
        try (InputStream entrada = archivo.getInputStream()) {
            return entrada.readNBytes(4);
        } catch (IOException excepcion) {
            throw new ReglaNegocioException(claveTipo, campo);
        }
    }

    private boolean empiezaCon(byte[] datos, byte[] firma) {
        return datos.length >= firma.length && Arrays.equals(Arrays.copyOf(datos, firma.length), firma);
    }
}
