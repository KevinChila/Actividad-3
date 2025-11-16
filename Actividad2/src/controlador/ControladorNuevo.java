package controlador;

import vista.ventana;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador principal que implementa todas las funcionalidades de concurrencia
 * requeridas en la Unidad 3.
 * 
 *  REQUISITOS IMPLEMENTADOS:
 *  Validación de contactos en segundo plano
 *  Búsqueda concurrente sin bloquear la UI
 *  Exportación con sincronización de archivos
 *  Notificaciones en tiempo real
 *  Sincronización robusta con locks y mecanismos de bloqueo
 *  Manejo seguro de condiciones de carrera
 * 
 * @author Kevin
 * 
 */
public class ControladorNuevo {

    private ventana vista;
    private List<modelo.persona> contactos = new ArrayList<>();
    private javax.swing.Timer busquedaTimer;
    
    // REQUISITO: Sincronización para exportación - evita corrupción cuando múltiples exportaciones se ejecutan simultáneamente
    private final Object exportLock = new Object();
    
    // REQUISITO: Sincronización para operaciones críticas en la lista de contactos
    private final Object contactosLock = new Object();
    
    // REQUISITO: Mecanismo de bloqueo por contacto - garantiza que solo un usuario pueda editar un contacto a la vez
    private final Map<String, ReentrantLock> locksContactos = new ConcurrentHashMap<>();
    
    // REQUISITO: ExecutorService para gestión eficiente de hilos - evita crear nuevos hilos constantemente
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    private int contactoSeleccionadoIndex = -1;
    private static final Logger logger = Logger.getLogger(ControladorNuevo.class.getName());
    
    // Timeout para evitar bloqueos permanentes en edición (5 segundos)
    private final long TIMEOUT_EDICION = 5000;

    public ControladorNuevo(ventana vista) {
        this.vista = vista;
        configurarLogger();
    }

    /**
     * Configura el sistema de logging para depuración de operaciones concurrentes
     */
    private void configurarLogger() {
        logger.setLevel(Level.INFO);
    }

    /**
     * Inicializa todos los componentes del controlador
     * Configura eventos, atajos de teclado y mecanismos de concurrencia
     */
    public void inicializar() {
        logger.info("Inicializando controlador con soporte de concurrencia");
        
        // Cargar contactos existentes al iniciar
        cargarContactosExistentes();
        
        // Atajos de teclado
        configurarAtajosTeclado();
        
        // Eventos de botones
        configurarEventosBotones();

        // REQUISITO: Configurar búsqueda con timer para ejecución en segundo plano
        configurarBusqueda();
        
        // Menú contextual
        inicializarMenuContextual();
        
        // Configurar combo box de filtrado
        vista.cmbFiltrarCat.addActionListener(e -> filtrarCategoria());
        
        // Inicialmente ocultar el botón Confirmar
        if (vista.btn_confirmar != null) {
            vista.btn_confirmar.setVisible(false);
        }
        
        logger.info("Controlador inicializado exitosamente");
    }

    private void configurarAtajosTeclado() {
        InputMap im = vista.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = vista.getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.CTRL_DOWN_MASK), "tab1");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_DOWN_MASK), "tab2");
        am.put("tab1", new AbstractAction() { 
            public void actionPerformed(ActionEvent e) { 
                vista.tabbedPane.setSelectedIndex(0); 
            }
        });
        am.put("tab2", new AbstractAction() { 
            public void actionPerformed(ActionEvent e) { 
                vista.tabbedPane.setSelectedIndex(1); 
            }
        });
    }

    private void configurarEventosBotones() {
        vista.btn_add.addActionListener(e -> agregarContacto());
        vista.btn_modificar.addActionListener(e -> iniciarModificacionContacto());
        vista.btn_eliminar.addActionListener(e -> eliminarContacto());
        vista.btnImportar.addActionListener(e -> importarContactos());
        vista.btnExportar.addActionListener(e -> exportarCSV());
        
        // Evento para el botón Confirmar
        if (vista.btn_confirmar != null) {
            vista.btn_confirmar.addActionListener(e -> modificarContacto());
        }
    }

    /**
     * REQUISITO: Configuración de búsqueda en segundo plano
     * Implementa DocumentListener y Timer para búsquedas asíncronas
     */
    private void configurarBusqueda() {
        // DocumentListener detecta cambios en el campo de búsqueda
        vista.txt_buscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { programarBusqueda(); }
            public void removeUpdate(DocumentEvent e) { programarBusqueda(); }
            public void changedUpdate(DocumentEvent e) { programarBusqueda(); }
        });
        
        // Timer de 300ms - espera a que el usuario deje de escribir para realizar la búsqueda
        busquedaTimer = new javax.swing.Timer(300, e -> realizarBusqueda());
        busquedaTimer.setRepeats(false);
    }

    // REQUISITO 1: VALIDACIÓN DE CONTACTOS EN SEGUNDO PLANO ===
    /**
     * Valida y agrega un nuevo contacto usando SwingWorker para no bloquear la UI
     * REQUISITO: Implementar un thread que permita validar si el contacto ya esta 
     * registrado antes de que este se envie a guardar y evitar datos duplicados.
     */
    public void agregarContacto() {
        String nombre = vista.txt_nombres.getText().trim();
        String telefono = vista.txt_telefono.getText().trim();
        String email = vista.txt_email.getText().trim();
        String categoria = (String) vista.cmb_categoria.getSelectedItem();
        boolean favorito = vista.chb_favorito.isSelected();

        if(nombre.isEmpty() || telefono.isEmpty() || email.isEmpty() || "Elija una Categoria".equals(categoria)) {
            mostrarNotificacion("Todos los campos deben ser llenados y debe seleccionar una categoría");
            return;
        }

        if(!validarEmail(email)) {
            mostrarNotificacion("Formato de email inválido");
            return;
        }

        // Deshabilitar botón mientras se valida para evitar múltiples envíos
        vista.btn_add.setEnabled(false);
        vista.btn_add.setText("Validando...");

        // REQUISITO: SwingWorker para validación en segundo plano
        // Este thread se ejecuta en background sin bloquear la interfaz de usuario
        SwingWorker<Boolean, String> validacionWorker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                publish("Iniciando validación de contacto...");
                Thread.sleep(500); // Simula validación en servidor
                
                // REQUISITO: Validar si el contacto ya existe con sincronización
                // Uso de synchronized para evitar condiciones de carrera al acceder a la lista
                synchronized(contactosLock) {
                    for(modelo.persona contacto : contactos) {
                        if(contacto.getEmail().equalsIgnoreCase(email)) {
                            publish("Validación fallida: Email ya existe");
                            return false;
                        }
                        if(contacto.getTelefono().equals(telefono)) {
                            publish("Validación fallida: Teléfono ya existe");
                            return false;
                        }
                    }
                }
                publish("Validación exitosa: Contacto único");
                return true;
            }

            @Override
            protected void process(List<String> chunks) {
            
                for (String message : chunks) {
                    logger.info(message);
                }
            }

            @Override
            protected void done() {
                try {
                    boolean esValido = get();
                    // REQUISITO: SwingUtilities.invokeLater para actualizaciones seguras de la UI
                    SwingUtilities.invokeLater(() -> {
                        if(esValido) {
                            modelo.persona p = new modelo.persona(nombre, telefono, email, categoria, favorito);
                            synchronized(contactosLock) {
                                contactos.add(p);
                            }
                            // ✅ CORRECCIÓN: Solo guardar una vez - eliminada la llamada duplicada
                            guardarContactosEnArchivo();
                            actualizarListaContactos();
                            limpiarCampos();
                            mostrarNotificacion("Contacto guardado con éxito");
                            logger.info("Contacto agregado exitosamente: " + email);
                        } else {
                            mostrarNotificacion("El contacto ya existe (email o teléfono duplicado)");
                        }
                        vista.btn_add.setEnabled(true);
                        vista.btn_add.setText("AGREGAR");
                    });
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Error en validación de contacto", ex);
                    SwingUtilities.invokeLater(() -> {
                        vista.btn_add.setEnabled(true);
                        vista.btn_add.setText("AGREGAR");
                        mostrarNotificacion("Error al validar contacto: " + ex.getMessage());
                    });
                }
            }
        };
        validacionWorker.execute(); // Ejecutar el worker en segundo plano
    }

    private boolean validarEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    // === REQUISITO 2: BÚSQUEDA EN SEGUNDO PLANO ===
    private void programarBusqueda() {
        if (busquedaTimer.isRunning()) {
            busquedaTimer.restart();
        } else {
            busquedaTimer.start();
        }
    }

    /**
     * Realiza búsqueda asíncrona sin bloquear la interfaz de usuario
     * REQUISITO: Implementar un thread independiente para realizar la búsqueda 
     * de contactos mientras el usuario sigue interactuando con la aplicación.
     * REQUISITO: Utilizar SwingWorker para evitar que la interfaz gráfica se 
     * congele al buscar en grandes volúmenes de datos.
     */
    private void realizarBusqueda() {
        final String texto = vista.txt_buscar.getText().toLowerCase();
        
        if (texto.isEmpty()) {
            actualizarListaContactos();
            return;
        }
        
        // REQUISITO: SwingWorker para búsqueda en segundo plano
        SwingWorker<DefaultListModel<String>, Void> busquedaWorker = 
            new SwingWorker<DefaultListModel<String>, Void>() {
            
            @Override
            protected DefaultListModel<String> doInBackground() throws Exception {
                logger.info("Iniciando búsqueda asíncrona: " + texto);
                DefaultListModel<String> model = new DefaultListModel<>();
                
                // Sincronización para acceso seguro a la lista durante la búsqueda
                synchronized(contactos) {
                    for(modelo.persona c : contactos) {
                        if(Thread.currentThread().isInterrupted()) {
                            logger.info("Búsqueda interrumpida");
                            return model;
                        }
                        
                        if(c.getNombre().toLowerCase().contains(texto) ||
                           c.getEmail().toLowerCase().contains(texto) ||
                           c.getTelefono().contains(texto)) {
                            model.addElement(c.formatoLista());
                        }
                    }
                }
                logger.info("Búsqueda completada: " + model.size() + " resultados");
                return model;
            }

            @Override
            protected void done() {
                try {
                    DefaultListModel<String> model = get();
                    // REQUISITO: Actualizar UI en el hilo de eventos
                    SwingUtilities.invokeLater(() -> {
                        vista.lst_contactos.setModel(model);
                        mostrarNotificacionTransitoria("Búsqueda completada: " + model.size() + " contactos encontrados");
                    });
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error en búsqueda asíncrona", ex);
                    SwingUtilities.invokeLater(() -> {
                        mostrarNotificacion("Error en búsqueda: " + ex.getMessage());
                    });
                }
            }
        };
        busquedaWorker.execute(); // Ejecutar búsqueda en background
    }

    //  REQUISITO 5: MECANISMO DE BLOQUEO CON ReentrantLock 
    /**
     * Bloquea un contacto para edición exclusiva con timeout
     * REQUISITO: Implementar un mecanismo de bloqueo de recursos para garantizar 
     * que solo un usuario pueda editar un contacto a la vez.
     * @param email Email del contacto a bloquear
     * @return true si se obtuvo el lock, false si timeout o error
     */
    private boolean bloquearContactoParaEdicion(String email) {
        try {
            // Obtener o crear lock específico para este contacto
            ReentrantLock lock = locksContactos.computeIfAbsent(email, k -> new ReentrantLock());
            // Intentar adquirir el lock con timeout para evitar bloqueos permanentes
            boolean adquirido = lock.tryLock(TIMEOUT_EDICION, TimeUnit.MILLISECONDS);
            if (adquirido) {
                logger.info("Lock adquirido para: " + email);
            } else {
                logger.warning("Timeout al adquirir lock para: " + email);
            }
            return adquirido;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Interrupción al adquirir lock para: " + email);
            return false;
        }
    }

    private void liberarContacto(String email) {
        ReentrantLock lock = locksContactos.get(email);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            logger.info("Lock liberado para: " + email);
            // No removemos el lock para reutilización
        }
    }

    private boolean estaSiendoEditado(String email) {
        ReentrantLock lock = locksContactos.get(email);
        return lock != null && lock.isLocked();
    }

    //  REQUISITO 5: MÉTODOS DE MODIFICACIÓN CON SINCRONIZACIÓN 
    /**
     * REQUISITO: Sincronización para evitar conflictos cuando múltiples 
     * hilos intenten modificar el mismo contacto al mismo tiempo.
     */
    public void iniciarModificacionContacto() {
        int selectedIndex = vista.lst_contactos.getSelectedIndex();
        if(selectedIndex == -1) {
            mostrarNotificacion("Seleccione un contacto para modificar");
            return;
        }

        contactoSeleccionadoIndex = selectedIndex;
        modelo.persona contactoSeleccionado;
        
        // REQUISITO: synchronized para acceso seguro a la lista de contactos
        synchronized(contactosLock) {
            if(selectedIndex < contactos.size()) {
                contactoSeleccionado = contactos.get(selectedIndex);
            } else {
                mostrarNotificacion("Contacto no encontrado");
                return;
            }
        }

        // Verificar si el contacto está siendo editado 
        if(estaSiendoEditado(contactoSeleccionado.getEmail())) {
            mostrarNotificacion("El contacto está siendo editado por otro proceso. Intente más tarde.");
            return;
        }

        // REQUISITO: Adquirir lock exclusivo para este contacto
        if(!bloquearContactoParaEdicion(contactoSeleccionado.getEmail())) {
            mostrarNotificacion("No se pudo bloquear el contacto para edición. Intente nuevamente.");
            return;
        }

        // Llenar campos con datos del contacto seleccionado
        SwingUtilities.invokeLater(() -> {
            vista.txt_nombres.setText(contactoSeleccionado.getNombre());
            vista.txt_telefono.setText(contactoSeleccionado.getTelefono());
            vista.txt_email.setText(contactoSeleccionado.getEmail());
            vista.cmb_categoria.setSelectedItem(contactoSeleccionado.getCategoria());
            vista.chb_favorito.setSelected(contactoSeleccionado.isFavorito());
            
            // Cambiar estados de botones
            vista.btn_add.setEnabled(false);
            vista.btn_modificar.setEnabled(false);
            vista.btn_eliminar.setText("CANCELAR");
            
            // Mostrar botón Confirmar
            if (vista.btn_confirmar != null) {
                vista.btn_confirmar.setVisible(true);
            }
            
            mostrarNotificacionTransitoria("Modo edición activado para: " + contactoSeleccionado.getNombre());
        });
    }

    // MÉTODO PARA MODIFICAR CONTACTO 
    /**
     * REQUISITO: ExecutorService para ejecutar modificación en background
     */
    public void modificarContacto() {
        if(contactoSeleccionadoIndex == -1) {
            mostrarNotificacion("No hay contacto seleccionado para modificar");
            return;
        }

        String nombre = vista.txt_nombres.getText().trim();
        String telefono = vista.txt_telefono.getText().trim();
        String email = vista.txt_email.getText().trim();
        String categoria = (String) vista.cmb_categoria.getSelectedItem();
        boolean favorito = vista.chb_favorito.isSelected();

        if(nombre.isEmpty() || telefono.isEmpty() || email.isEmpty() || "Elija una Categoria".equals(categoria)) {
            mostrarNotificacion("Todos los campos deben ser llenados");
            return;
        }

        if(!validarEmail(email)) {
            mostrarNotificacion("Formato de email inválido");
            return;
        }

        // Obtener el email original para liberar el lock correcto
        String emailOriginal;
        synchronized(contactosLock) {
            if(contactoSeleccionadoIndex < contactos.size()) {
                emailOriginal = contactos.get(contactoSeleccionadoIndex).getEmail();
            } else {
                mostrarNotificacion("Contacto no encontrado");
                return;
            }
        }

        // REQUISITO: Usar ExecutorService para la modificación con manejo de errores
        executor.submit(() -> {
            try {
                boolean exito = false;
                // REQUISITO: Operación atómica con synchronized
                synchronized(contactosLock) {
                    if(contactoSeleccionadoIndex < contactos.size()) {
                        modelo.persona contactoActual = contactos.get(contactoSeleccionadoIndex);
                        
                        // Validar que no exista otro contacto con el mismo email o teléfono
                        boolean existeDuplicado = false;
                        for(int i = 0; i < contactos.size(); i++) {
                            if(i != contactoSeleccionadoIndex) {
                                modelo.persona otroContacto = contactos.get(i);
                                if(otroContacto.getEmail().equalsIgnoreCase(email) || 
                                   otroContacto.getTelefono().equals(telefono)) {
                                    existeDuplicado = true;
                                    break;
                                }
                            }
                        }
                        
                        if(!existeDuplicado) {
                            modelo.persona contactoActualizado = new modelo.persona(nombre, telefono, email, categoria, favorito);
                            contactos.set(contactoSeleccionadoIndex, contactoActualizado);
                            guardarContactosEnArchivo();
                            exito = true;
                            logger.info("Contacto modificado exitosamente: " + email);
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                mostrarNotificacion("Ya existe otro contacto con el mismo email o teléfono");
                            });
                        }
                    }
                }
                
                if(exito) {
                    SwingUtilities.invokeLater(() -> {
                        actualizarListaContactos();
                        limpiarCampos();
                        resetearBotones();
                        mostrarNotificacion("Contacto modificado con éxito");
                    });
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error al modificar contacto", e);
                SwingUtilities.invokeLater(() -> {
                    mostrarNotificacion("Error al modificar contacto: " + e.getMessage());
                });
            } finally {
                // REQUISITO: Liberar el bloqueo del contacto original
                liberarContacto(emailOriginal);
                // Si el email cambió, también liberar el nuevo
                if (!emailOriginal.equals(email)) {
                    liberarContacto(email);
                }
            }
        });
    }

    public void cancelarModificacion() {
        if(contactoSeleccionadoIndex != -1) {
            synchronized(contactosLock) {
                if(contactoSeleccionadoIndex < contactos.size()) {
                    String email = contactos.get(contactoSeleccionadoIndex).getEmail();
                    liberarContacto(email);
                    logger.info("Edición cancelada para: " + email);
                }
            }
        }
        resetearBotones();
        limpiarCampos();
        mostrarNotificacionTransitoria("Edición cancelada");
    }
	
	    private void resetearBotones() {
        SwingUtilities.invokeLater(() -> {
            vista.btn_add.setEnabled(true);
            vista.btn_modificar.setEnabled(true);
            vista.btn_modificar.setText("MODIFICAR");
            vista.btn_eliminar.setText("ELIMINAR");
            
            // Ocultar botón Confirmar
            if (vista.btn_confirmar != null) {
                vista.btn_confirmar.setVisible(false);
            }
            
            contactoSeleccionadoIndex = -1;
        });
    }

    // ELIMINACIÓN CON SINCRONIZACIÓN MEJORADA 
    public void eliminarContacto() {
        // Si estamos en modo modificación, cancelar
        if("CANCELAR".equals(vista.btn_eliminar.getText())) {
            cancelarModificacion();
            return;
        }

        int selectedIndex = vista.lst_contactos.getSelectedIndex();
        if(selectedIndex == -1) {
            mostrarNotificacion("Seleccione un contacto para eliminar");
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(vista, 
            "¿Está seguro de eliminar este contacto?", "Confirmar eliminación", 
            JOptionPane.YES_NO_OPTION);
        
        if(confirmacion == JOptionPane.YES_OPTION) {
            //Variable para controlar si ya se mostró el mensaje
            final boolean[] mensajeMostrado = {false};
            
            // REQUISITO: Usar ExecutorService para la eliminación con manejo de locks
            executor.submit(() -> {
                modelo.persona contactoEliminado = null;
                try {
                    synchronized(contactosLock) {
                        if(selectedIndex < contactos.size()) {
                            contactoEliminado = contactos.remove(selectedIndex);
                            guardarContactosEnArchivo();
                            logger.info("Contacto eliminado: " + 
                                (contactoEliminado != null ? contactoEliminado.getEmail() : "desconocido"));
                        }
                    }
                    
                    SwingUtilities.invokeLater(() -> {
                        actualizarListaContactos();
                        // Solo mostrara el mensaje una vez
                        if (!mensajeMostrado[0]) {
                            mostrarNotificacion("Contacto eliminado con éxito");
                            mensajeMostrado[0] = true;
                        }
                    });
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error al eliminar contacto", e);
                    SwingUtilities.invokeLater(() -> {
                        //  Solo mostrara el mensaje de error una vez
                        if (!mensajeMostrado[0]) {
                            mostrarNotificacion("Error al eliminar contacto: " + e.getMessage());
                            mensajeMostrado[0] = true;
                        }
                    });
                } finally {
                    // REQUISITO: Liberar el bloqueo si existía
                    if (contactoEliminado != null) {
                        liberarContacto(contactoEliminado.getEmail());
                    }
                }
            });
        }
    }

    // REQUISITO 3: EXPORTACIÓN CON HILOS MÚLTIPLES 
    /**
     * Exporta contactos a CSV con sincronización robusta y manejo de progreso
     * REQUISITO: Crear un proceso en segundo plano que permita exportar la lista 
     * de contactos a un archivo CSV sin afectar la fluidez de la aplicación.
     * REQUISITO: Sincronizar el acceso al archivo para evitar corrupción de datos 
     * cuando múltiples exportaciones se realizan al mismo tiempo.
     */
    public void exportarCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("contactos_exportados.csv"));
        
        if (fileChooser.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            // REQUISITO: SwingWorker para exportación en segundo plano
            SwingWorker<Void, Integer> exportWorker = new SwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    logger.info("Iniciando exportación a: " + file.getAbsolutePath());
                    
                    // REQUISITO: Sincronización de archivo - evita corrupción con múltiples exportaciones
                    synchronized(exportLock) {
                        try (PrintWriter pw = new PrintWriter(file, "UTF-8")) {
                            // Escribir BOM para UTF-8 y encabezado
                            pw.print('\uFEFF');
                            pw.println("Nombre,Telefono,Email,Categoria,Favorito");
                            
                            List<modelo.persona> copiaContactos;
                            synchronized(contactosLock) {
                                copiaContactos = new ArrayList<>(contactos);
                            }
                            
                            int total = copiaContactos.size();
                            for (int i = 0; i < total; i++) {
                                if (isCancelled()) {
                                    logger.info("Exportación cancelada por usuario");
                                    return null;
                                }
                                
                                modelo.persona contacto = copiaContactos.get(i);
                                String fila = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                                    escapeCSV(contacto.getNombre()),
                                    escapeCSV(contacto.getTelefono()),
                                    escapeCSV(contacto.getEmail()),
                                    escapeCSV(contacto.getCategoria()),
                                    contacto.isFavorito() ? "Sí" : "No");
                                pw.println(fila);
                                
                                int progreso = (i + 1) * 100 / total;
                                publish(progreso);
                                
                                // Pequeña pausa para simular procesamiento
                                Thread.sleep(10);
                            }
                            logger.info("Exportación completada: " + total + " contactos");
                        } catch (IOException ex) {
                            logger.log(Level.SEVERE, "Error en exportación", ex);
                            SwingUtilities.invokeLater(() -> 
                                mostrarNotificacion("Error al exportar: " + ex.getMessage()));
                        } catch (Exception ex) {
                            logger.log(Level.SEVERE, "Error inesperado en exportación", ex);
                            throw ex;
                        }
                    }
                    return null;
                }

                @Override
                protected void process(List<Integer> chunks) {
                    if (!chunks.isEmpty()) {
                        vista.progressBar.setValue(chunks.get(chunks.size() - 1));
                    }
                }

                @Override
                protected void done() {
                    SwingUtilities.invokeLater(() -> {
                        vista.progressBar.setValue(0);
                        try {
                            if (!isCancelled()) {
                                mostrarNotificacion("Exportación completada: " + file.getName());
                            }
                        } catch (Exception ex) {
                            mostrarNotificacion("Error durante exportación: " + ex.getMessage());
                        }
                    });
                }
            };
            exportWorker.execute();
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    // IMPORTAR CONTACTOS A TABLA
    public void importarContactos() {
        vista.modeloTabla.setRowCount(0);
        
        SwingWorker<Void, Object[]> worker = new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                logger.info("Iniciando importación a tabla");
                List<modelo.persona> copiaContactos;
                synchronized(contactosLock) {
                    copiaContactos = new ArrayList<>(contactos);
                }
                
                int total = copiaContactos.size();
                for (int i = 0; i < total; i++) {
                    if (isCancelled()) {
                        return null;
                    }
                    
                    modelo.persona p = copiaContactos.get(i);
                    Object[] fila = {p.getNombre(), p.getTelefono(), p.getEmail(), 
                                   p.getCategoria(), p.isFavorito() ? "Sí" : "No"};
                    publish(fila);
                    
                    int progreso = (i + 1) * 100 / total;
                    setProgress(progreso);
                    Thread.sleep(20);
                }
                logger.info("Importación a tabla completada: " + total + " contactos");
                return null;
            }
            
            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] fila : chunks) {
                    vista.modeloTabla.addRow(fila);
                }
            }
            
            @Override
            protected void done() {
                vista.progressBar.setValue(100);
                try {
                    if (!isCancelled()) {
                        mostrarNotificacion("Datos importados correctamente: " + 
                            vista.modeloTabla.getRowCount() + " contactos");
                    }
                } catch (Exception ex) {
                    mostrarNotificacion("Error durante importación: " + ex.getMessage());
                }
            }
        };
        
        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                vista.progressBar.setValue((Integer) evt.getNewValue());
            }
        });
        worker.execute();
    }

    // === REQUISITO 4: NOTIFICACIONES EN TIEMPO REAL - MEJORADAS ===
    /**
     * REQUISITO: Implementar un thread que muestre notificaciones en tiempo real
     * REQUISITO: Asegurar que las actualizaciones de la UI sean manejadas correctamente 
     * con SwingUtilities.invokeLater()
     */
    private void mostrarNotificacion(String mensaje) {
        // REQUISITO: SwingUtilities.invokeLater para actualizaciones seguras de UI
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(vista, mensaje, "Notificación", 
                JOptionPane.INFORMATION_MESSAGE);
            logger.info("Notificación: " + mensaje);
        });
    }

    private void mostrarNotificacionTransitoria(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            
            logger.info("Notificación transitoria: " + mensaje);
        });
    }

    //  FILTRAR POR CATEGORÍA 
    public void filtrarCategoria() {
        String cat = (String) vista.cmbFiltrarCat.getSelectedItem();
        if ("Todos".equals(cat)) {
            ((javax.swing.table.TableRowSorter<?>) vista.tablaContactos.getRowSorter()).setRowFilter(null);
        } else {
            ((javax.swing.table.TableRowSorter<?>) vista.tablaContactos.getRowSorter())
                    .setRowFilter(RowFilter.regexFilter("^" + cat + "$", 3));
        }
    }

    // MÉTODOS AUXILIARES 
    private void cargarContactosExistentes() {
        try {
            modelo.personaDAO dao = new modelo.personaDAO(new modelo.persona());
            synchronized(contactosLock) {
                contactos = dao.leerArchivo();
            }
            actualizarListaContactos();
            logger.info("Contactos cargados: " + contactos.size());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al cargar contactos", e);
            mostrarNotificacion("Error al cargar contactos existentes: " + e.getMessage());
            contactos = new ArrayList<>();
        }
    }

    private void guardarContactosEnArchivo() {
        try {
            modelo.personaDAO dao = new modelo.personaDAO(new modelo.persona());
            List<modelo.persona> copiaContactos;
            synchronized(contactosLock) {
                copiaContactos = new ArrayList<>(contactos);
            }
            dao.actualizarContactos(copiaContactos);
            logger.info("Contactos guardados en archivo: " + copiaContactos.size());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al guardar contactos", e);
            mostrarNotificacion("Error al guardar contactos: " + e.getMessage());
        }
    }

    private void actualizarListaContactos() {
        DefaultListModel<String> modelList = new DefaultListModel<>();
        synchronized(contactosLock) {
            for(modelo.persona c : contactos) {
                modelList.addElement(c.formatoLista());
            }
        }
        vista.lst_contactos.setModel(modelList);
    }

    private void limpiarCampos() {
        SwingUtilities.invokeLater(() -> {
            vista.txt_nombres.setText("");
            vista.txt_telefono.setText("");
            vista.txt_email.setText("");
            vista.cmb_categoria.setSelectedIndex(0);
            vista.chb_favorito.setSelected(false);
        });
    }

    private void inicializarMenuContextual() {
        vista.tablaContactos.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    vista.menuTabla.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    vista.menuTabla.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // Eventos del menú contextual
        vista.menuModificar.addActionListener(e -> modificarContactoSeleccionado());
        vista.menuEliminar.addActionListener(e -> eliminarContactoSeleccionado());
    }

    // MÉTODOS DEL MENÚ CONTEXTUAL 
    private void modificarContactoSeleccionado() {
        int selectedRow = vista.tablaContactos.getSelectedRow();
        if(selectedRow != -1) {
            String nombre = (String) vista.modeloTabla.getValueAt(selectedRow, 0);
            // Buscar el contacto en la lista y seleccionarlo en la lista principal
            synchronized(contactosLock) {
                for(int i = 0; i < contactos.size(); i++) {
                    if(contactos.get(i).getNombre().equals(nombre)) {
                        vista.lst_contactos.setSelectedIndex(i);
                        vista.tabbedPane.setSelectedIndex(0);
                        iniciarModificacionContacto();
                        break;
                    }
                }
            }
        }
    }

    private void eliminarContactoSeleccionado() {
        int selectedRow = vista.tablaContactos.getSelectedRow();
        if(selectedRow != -1) {
            String nombre = (String) vista.modeloTabla.getValueAt(selectedRow, 0);
            // Buscar el contacto en la lista y seleccionarlo en la lista principal
            synchronized(contactosLock) {
                for(int i = 0; i < contactos.size(); i++) {
                    if(contactos.get(i).getNombre().equals(nombre)) {
                        vista.lst_contactos.setSelectedIndex(i);
                        vista.tabbedPane.setSelectedIndex(0);
                        eliminarContacto();
                        break;
                    }
                }
            }
        }
    }

    //  LIMPIEZA DE RECURSOS 
    /**
     * Cierra todos los recursos del controlador de manera segura
     * Incluye shutdown del ExecutorService y liberación de locks
     */
    public void cerrarRecursos() {
        logger.info("Cerrando recursos del controlador");
        
        // Liberar todos los locks
        synchronized(locksContactos) {
            for (Map.Entry<String, ReentrantLock> entry : locksContactos.entrySet()) {
                ReentrantLock lock = entry.getValue();
                if (lock.isLocked()) {
                    logger.warning("Lock aún activo para: " + entry.getKey());
                    // Forzar unlock si es necesario (con cuidado)
                    while (lock.isLocked() && lock.getHoldCount() > 0) {
                        lock.unlock();
                    }
                }
            }
            locksContactos.clear();
        }
        
        // Cerrar ExecutorService
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warning("ExecutorService no terminó en 5 segundos, forzando shutdown");
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.severe("ExecutorService no pudo ser terminado");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            logger.severe("Interrupción durante cierre de ExecutorService");
        }
        
        logger.info("Recursos del controlador cerrados exitosamente");
    }

    public void cambiarPestana(int indice) {
        vista.tabbedPane.setSelectedIndex(indice);
    }
}