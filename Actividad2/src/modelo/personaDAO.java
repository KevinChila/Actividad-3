package modelo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Kevin
 * 
 */
public class personaDAO {
    
    private File archivo;
    private persona persona;
    private static final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();
    private static final Logger logger = Logger.getLogger(personaDAO.class.getName());

    public personaDAO(persona persona) {
        this.persona = persona;
        archivo = new File("c:/gestionContactos");
        prepararArchivo();
    }
    
    private void prepararArchivo() {
        fileLock.writeLock().lock();
        try {
            if (!archivo.exists()) {
                archivo.mkdir();
                logger.info("Directorio creado: " + archivo.getAbsolutePath());
            }
            
            archivo = new File(archivo.getAbsolutePath(), "datosContactos.csv");
            if (!archivo.exists()) {
                archivo.createNewFile();
                String encabezado = String.format("%s;%s;%s;%s;%s", 
                    "NOMBRE", "TELEFONO", "EMAIL", "CATEGORIA", "FAVORITO");
                escribir(encabezado, false);
                logger.info("Archivo de contactos creado: " + archivo.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al preparar archivo", e);
            throw new RuntimeException("No se pudo inicializar el archivo de contactos", e);
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    private void escribir(String texto, boolean append) {
        fileLock.writeLock().lock();
        try (BufferedWriter escribir = new BufferedWriter(new FileWriter(archivo.getAbsolutePath(), append))) {
            escribir.write(texto);
            escribir.newLine();
            logger.fine("Texto escrito en archivo: " + texto.substring(0, Math.min(50, texto.length())));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al escribir en archivo", e);
            throw new RuntimeException("Error de E/S al escribir contacto", e);
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    public boolean escribirArchivo() {
        logger.info("Escribiendo contacto individual en archivo");
        escribir(persona.datosContacto(), true);
        return true;
    }
    
    public List<persona> leerArchivo() throws IOException {
        fileLock.readLock().lock();
        try {
            logger.info("Leyendo contactos desde archivo");
            List<persona> personas = new ArrayList<>();
            
            if (!archivo.exists() || archivo.length() == 0) {
                logger.info("Archivo no existe o está vacío");
                return personas;
            }
            
            try (BufferedReader leer = new BufferedReader(new FileReader(archivo.getAbsolutePath()))) {
                String linea;
                boolean primeraLinea = true;
                int contador = 0;
                
                while ((linea = leer.readLine()) != null) {
                    if (primeraLinea) {
                        primeraLinea = false;
                        continue; // Saltar encabezado
                    }
                    
                    if(linea.trim().isEmpty()) continue;
                    
                    String[] campos = linea.split(";");
                    if(campos.length >= 5) {
                        persona p = new persona();
                        p.setNombre(campos[0]);
                        p.setTelefono(campos[1]);
                        p.setEmail(campos[2]);
                        p.setCategoria(campos[3]);
                        p.setFavorito(Boolean.parseBoolean(campos[4]));
                        personas.add(p);
                        contador++;
                    } else {
                        logger.warning("Línea con formato inválido: " + linea);
                    }
                }
                logger.info("Contactos leídos: " + contador);
            }
            return personas;
        } finally {
            fileLock.readLock().unlock();
        }
    }
    
    public void actualizarContactos(List<persona> personas) throws IOException {
        fileLock.writeLock().lock();
        logger.info("Actualizando archivo con " + personas.size() + " contactos");
        
        try {
            // Crear archivo temporal
            File tempFile = new File(archivo.getAbsolutePath() + ".tmp");
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                // Escribir encabezado
                String encabezado = String.format("%s;%s;%s;%s;%s", 
                    "NOMBRE", "TELEFONO", "EMAIL", "CATEGORIA", "FAVORITO");
                writer.write(encabezado);
                writer.newLine();
                
                // Escribir todos los contactos
                for (persona p : personas) {
                    writer.write(p.datosContacto());
                    writer.newLine();
                }
            }
            
            // Reemplazar archivo original con el temporal
            if (archivo.delete()) {
                if (!tempFile.renameTo(archivo)) {
                    throw new IOException("No se pudo renombrar el archivo temporal");
                }
                logger.info("Archivo actualizado exitosamente");
            } else {
                throw new IOException("No se pudo eliminar el archivo original");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al actualizar contactos", e);
            throw e;
        } finally {
            fileLock.writeLock().unlock();
        }
    }
    
    // Método para limpiar el archivo (usado en pruebas)
    public void limpiarArchivo() throws IOException {
        fileLock.writeLock().lock();
        try (FileWriter writer = new FileWriter(archivo.getAbsolutePath(), false)) {
            String encabezado = String.format("%s;%s;%s;%s;%s", 
                "NOMBRE", "TELEFONO", "EMAIL", "CATEGORIA", "FAVORITO");
            writer.write(encabezado + "\n");
            logger.info("Archivo limpiado");
        } finally {
            fileLock.writeLock().unlock();
        }
    }
    
    /**
     * Obtiene estadísticas del archivo (para debugging)
     */
    public String obtenerEstadisticas() throws IOException {
        fileLock.readLock().lock();
        try {
            if (!archivo.exists()) {
                return "Archivo no existe";
            }
            
            long tamaño = archivo.length();
            int lineas = 0;
            
            try (BufferedReader leer = new BufferedReader(new FileReader(archivo.getAbsolutePath()))) {
                while (leer.readLine() != null) {
                    lineas++;
                }
            }
            
            return String.format("Archivo: %s, Tamaño: %d bytes, Líneas: %d", 
                archivo.getName(), tamaño, lineas);
        } finally {
            fileLock.readLock().unlock();
        }
    }
}