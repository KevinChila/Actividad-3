package vista;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import controlador.ControladorNuevo;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ventana extends JFrame {

    // --- Componentes de la interfaz ---
    public JPanel contentPane;
    public JTextField txt_nombres;
    public JTextField txt_telefono;
    public JTextField txt_email;
    public JTextField txt_buscar;
    public JCheckBox chb_favorito;
    public JComboBox<String> cmb_categoria;
    public JButton btn_add;
    public JButton btn_modificar;
    public JButton btn_eliminar;
    public JButton btn_confirmar;
    public JList<String> lst_contactos;
    public JScrollPane scrLista;

    public JTabbedPane tabbedPane;
    public JTable tablaContactos;
    public DefaultTableModel modeloTabla;
    public JComboBox<String> cmbFiltrarCat;
    public JButton btnImportar;
    public JButton btnExportar;
    public JProgressBar progressBar;

    // --- Menu contextual ---
    public JPopupMenu menuTabla;
    public JMenuItem menuModificar;
    public JMenuItem menuEliminar;

    public ControladorNuevo controlador;
    private JPanel panel;
    private JLabel lblNewLabel;
    private JPanel panel_1;
    private JLabel lblNewLabel_2;
    private JPanel panel_2;
    private JLabel lblNewLabel_3;
    private JPanel panel_3;
    private JLabel lblNewLabel_4;
    private JLabel lblNewLabel_5;
    private JPanel panel_4;
    private JLabel lblNewLabel_6;
    private JLabel lblNewLabel_7;
    private JLabel lblNewLabel_8;
    private JPanel panel_5;
    private JLabel lblNewLabel_9;
    private JLabel lblNewLabel_10;
    private JPanel panel_6;
    private JPanel panel_7;
    private JLabel lblNewLabel_1;
    private JLabel lblNewLabel_11;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                ventana frame = new ventana();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public ventana() {
        setTitle("GESTION DE CONTACTOS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setBounds(100, 100, 901, 699);

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(0, 0));

        // JTabbedPane para organizar la interfaz en pestañas
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        contentPane.add(tabbedPane);

        // Panel de Contactos
        JPanel panelContactos = new JPanel();
        panelContactos.setBackground(new Color(191, 191, 191));
        tabbedPane.addTab("Contactos", panelContactos);

        // Panel de Estadísticas
        JPanel panelEstadisticas = new JPanel();
        panelEstadisticas.setBackground(new Color(191, 191, 191));
        tabbedPane.addTab("Estadísticas", panelEstadisticas);

        // INICIALIZAR cmb_categoria ANTES de usarlo
        cmb_categoria = new JComboBox<>();
        String[] categorias = {"Elija una Categoria", "Familia", "Amigos", "Trabajo"};
        for (String categoria : categorias) cmb_categoria.addItem(categoria);

        // --- Lista de contactos ---
        lst_contactos = new JList<>();
        lst_contactos.setFont(new Font("Tahoma", Font.PLAIN, 15));
        lst_contactos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrLista = new JScrollPane(lst_contactos);
        scrLista.setBounds(65, 432, 743, 172);
        panelContactos.setLayout(null);
        panelContactos.add(scrLista);
        
        panel_2 = new JPanel();
        panel_2.setBackground(new Color(203, 7, 138));
        panel_2.setBounds(65, -1, 743, 51);
        panelContactos.add(panel_2);
        panel_2.setLayout(null);
        
        lblNewLabel_3 = new JLabel("GESTION DE CONTACTOS");
        lblNewLabel_3.setForeground(new Color(255, 255, 255));
        lblNewLabel_3.setBackground(new Color(255, 255, 255));
        lblNewLabel_3.setFont(new Font("Tahoma", Font.PLAIN, 21));
        lblNewLabel_3.setBounds(239, 10, 245, 31);
        panel_2.add(lblNewLabel_3);
        
        panel_3 = new JPanel();
        panel_3.setBackground(new Color(191, 242, 5));
        panel_3.setBounds(65, 60, 743, 185);
        panelContactos.add(panel_3);
        panel_3.setLayout(null);
        
        txt_nombres = new JTextField();
        txt_nombres.setBounds(112, 5, 427, 21);
        panel_3.add(txt_nombres);
        txt_nombres.setFont(new Font("Tahoma", Font.PLAIN, 15));
        txt_nombres.setColumns(10);
        
        txt_telefono = new JTextField();
        txt_telefono.setBounds(122, 36, 427, 25);
        panel_3.add(txt_telefono);
        txt_telefono.setFont(new Font("Tahoma", Font.PLAIN, 15));
        txt_telefono.setColumns(10);
        
        txt_email = new JTextField();
        txt_email.setBounds(112, 71, 427, 25);
        panel_3.add(txt_email);
        txt_email.setFont(new Font("Tahoma", Font.PLAIN, 15));
        txt_email.setColumns(10);
        
        // --- Campos y etiquetas de Contactos ---
        JLabel lbl_etiqueta1 = new JLabel("NOMBRES:");
        lbl_etiqueta1.setBounds(10, 11, 89, 13);
        panel_3.add(lbl_etiqueta1);
        lbl_etiqueta1.setFont(new Font("Tahoma", Font.BOLD, 15));
        
        // Etiquetas adicionales
        JLabel lbl_etiqueta1_1 = new JLabel("TELEFONO:");
        lbl_etiqueta1_1.setBounds(10, 44, 89, 13);
        panel_3.add(lbl_etiqueta1_1);
        lbl_etiqueta1_1.setFont(new Font("Tahoma", Font.BOLD, 15));
        
        JLabel lbl_etiqueta1_2 = new JLabel("EMAIL:");
        lbl_etiqueta1_2.setBounds(22, 77, 89, 13);
        panel_3.add(lbl_etiqueta1_2);
        lbl_etiqueta1_2.setFont(new Font("Tahoma", Font.BOLD, 15));
        
        // cmb_categoria ya está inicializado arriba
        cmb_categoria.setBounds(112, 118, 192, 31);
        panel_3.add(cmb_categoria);
        
        lblNewLabel_4 = new JLabel("Categoria");
        lblNewLabel_4.setFont(new Font("Tahoma", Font.BOLD, 15));
        lblNewLabel_4.setBounds(10, 108, 89, 25);
        panel_3.add(lblNewLabel_4);
        
        chb_favorito = new JCheckBox("CONTACTO FAVORITO");
        chb_favorito.setBounds(335, 121, 182, 21);
        panel_3.add(chb_favorito);
        chb_favorito.setFont(new Font("Tahoma", Font.PLAIN, 15));
        
        lblNewLabel_5 = new JLabel("");
        lblNewLabel_5.setIcon(new ImageIcon("C:\\Users\\kchil\\Desktop\\imagens\\Imagen3.png"));
        lblNewLabel_5.setBounds(527, 118, 42, 22);
        panel_3.add(lblNewLabel_5);
        
        JComboBox comboBox = new JComboBox();
        comboBox.setBounds(607, 24, 80, 20);
        panel_3.add(comboBox);
        
        panel_4 = new JPanel();
        panel_4.setBackground(new Color(191, 242, 5));
        panel_4.setBounds(65, 255, 743, 106);
        panelContactos.add(panel_4);
        
        // --- Botones principales ---
        btn_add = new JButton("AGREGAR");
        btn_add.setFont(new Font("Tahoma", Font.PLAIN, 15));
        
        btn_modificar = new JButton("MODIFICAR");
        btn_modificar.setFont(new Font("Tahoma", Font.PLAIN, 15));
        
        btn_eliminar = new JButton("ELIMINAR");
        btn_eliminar.setFont(new Font("Tahoma", Font.PLAIN, 15));
        
        // NUEVO BOTÓN CONFIRMAR
        btn_confirmar = new JButton("CONFIRMAR");
        btn_confirmar.setFont(new Font("Tahoma", Font.PLAIN, 15));
        
        lblNewLabel_6 = new JLabel("New label");
        lblNewLabel_6.setIcon(new ImageIcon("C:\\Users\\kchil\\Desktop\\imagens\\Imagen4.png"));
        
        lblNewLabel_7 = new JLabel("");
        lblNewLabel_7.setIcon(new ImageIcon("C:\\Users\\kchil\\Desktop\\imagens\\Imagen6.png"));
        
        lblNewLabel_8 = new JLabel("");
        lblNewLabel_8.setIcon(new ImageIcon("C:\\Users\\kchil\\Desktop\\imagens\\Imagen5.png"));
        
        GroupLayout gl_panel_4 = new GroupLayout(panel_4);
        gl_panel_4.setHorizontalGroup(
        	gl_panel_4.createParallelGroup(Alignment.LEADING)
        		.addGroup(gl_panel_4.createSequentialGroup()
        			.addGroup(gl_panel_4.createParallelGroup(Alignment.LEADING)
        				.addGroup(gl_panel_4.createSequentialGroup()
        					.addGap(53)
        					.addComponent(btn_add, GroupLayout.PREFERRED_SIZE, 116, GroupLayout.PREFERRED_SIZE))
        				.addGroup(gl_panel_4.createSequentialGroup()
        					.addGap(91)
        					.addComponent(lblNewLabel_6, GroupLayout.PREFERRED_SIZE, 55, GroupLayout.PREFERRED_SIZE)))
        			.addGroup(gl_panel_4.createParallelGroup(Alignment.LEADING)
        				.addGroup(gl_panel_4.createSequentialGroup()
        					.addGap(55)
        					.addComponent(btn_modificar, GroupLayout.PREFERRED_SIZE, 131, GroupLayout.PREFERRED_SIZE)
        					.addGap(27)
        					.addComponent(btn_confirmar, GroupLayout.PREFERRED_SIZE, 131, GroupLayout.PREFERRED_SIZE)
        					.addGap(32)
        					.addComponent(btn_eliminar, GroupLayout.PREFERRED_SIZE, 132, GroupLayout.PREFERRED_SIZE)
        					.addGap(28))
        				.addGroup(Alignment.TRAILING, gl_panel_4.createSequentialGroup()
        					.addPreferredGap(ComponentPlacement.RELATED)
        					.addComponent(lblNewLabel_7, GroupLayout.PREFERRED_SIZE, 49, GroupLayout.PREFERRED_SIZE)
        					.addGap(182)
        					.addComponent(lblNewLabel_8, GroupLayout.PREFERRED_SIZE, 49, GroupLayout.PREFERRED_SIZE)
        					.addGap(65))))
        );
        gl_panel_4.setVerticalGroup(
        	gl_panel_4.createParallelGroup(Alignment.LEADING)
        		.addGroup(Alignment.TRAILING, gl_panel_4.createSequentialGroup()
        			.addGroup(gl_panel_4.createParallelGroup(Alignment.TRAILING)
        				.addGroup(gl_panel_4.createSequentialGroup()
        					.addContainerGap()
        					.addComponent(lblNewLabel_6)
        					.addPreferredGap(ComponentPlacement.RELATED))
        				.addGroup(Alignment.LEADING, gl_panel_4.createSequentialGroup()
        					.addContainerGap()
        					.addGroup(gl_panel_4.createParallelGroup(Alignment.LEADING)
        						.addComponent(lblNewLabel_7)
        						.addComponent(lblNewLabel_8))
        					.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        			.addGroup(gl_panel_4.createParallelGroup(Alignment.LEADING)
        				.addComponent(btn_confirmar, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
        				.addComponent(btn_eliminar, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
        				.addGroup(gl_panel_4.createParallelGroup(Alignment.BASELINE)
        					.addComponent(btn_add, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
        					.addComponent(btn_modificar, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)))
        			.addContainerGap())
        );
        panel_4.setLayout(gl_panel_4);
        
        panel_5 = new JPanel();
        panel_5.setBackground(new Color(3, 64, 1));
        panel_5.setBounds(65, 371, 743, 51);
        panelContactos.add(panel_5);
        panel_5.setLayout(null);
        
        txt_buscar = new JTextField();
        txt_buscar.setBounds(211, 10, 356, 36);
        panel_5.add(txt_buscar);
        txt_buscar.setFont(new Font("Tahoma", Font.PLAIN, 15));
        txt_buscar.setColumns(10);
        
        lblNewLabel_9 = new JLabel("BUSCAR:\n");
        lblNewLabel_9.setForeground(new Color(255, 255, 255));
        lblNewLabel_9.setFont(new Font("Tahoma", Font.BOLD, 15));
        lblNewLabel_9.setBounds(102, 12, 89, 31);
        panel_5.add(lblNewLabel_9);
        
        lblNewLabel_10 = new JLabel("");
        lblNewLabel_10.setIcon(new ImageIcon("C:\\Users\\kchil\\Desktop\\imagens\\Imagen7.png"));
        lblNewLabel_10.setBounds(587, 14, 64, 31);
        panel_5.add(lblNewLabel_10);

        // --- Tabla de Estadísticas ---
        String[] columnasTabla = {"Nombre", "Teléfono", "Email", "Categoría", "Favorito"};
        modeloTabla = new DefaultTableModel(columnasTabla, 0);
        tablaContactos = new JTable(modeloTabla);
        tablaContactos.setFont(new Font("Tahoma", Font.PLAIN, 15));
        tablaContactos.setAutoCreateRowSorter(true);
        
        JScrollPane scrollTabla = new JScrollPane(tablaContactos);
        scrollTabla.setBounds(10, 136, 486, 403);
        
        // --- Barra de progreso ---
        progressBar = new JProgressBar();
        progressBar.setForeground(new Color(191, 242, 5));
        progressBar.setBounds(60, 559, 722, 39);
        progressBar.setStringPainted(true);

        panel = new JPanel();
        panel.setBackground(new Color(242, 25, 5));
        panel.setBounds(0, 10, 1085, 55);
        
        panel_1 = new JPanel();
        panel_1.setBackground(new Color(203, 7, 138));
        panel_1.setBounds(0, 75, 488, 43);
        
        panel_6 = new JPanel();
        panel_6.setBackground(new Color(191, 242, 5));
        panel_6.setBounds(502, 136, 360, 403);
        
        panel_7 = new JPanel();
        panel_7.setBackground(new Color(203, 7, 138));
        panel_7.setBounds(498, 75, 364, 43);
        panel_7.setLayout(null);
        
        JLabel lblFiltrarCat = new JLabel("Filtrar por categoría:");
        lblFiltrarCat.setBounds(36, 9, 158, 19);
        panel_7.add(lblFiltrarCat);
        lblFiltrarCat.setFont(new Font("Tahoma", Font.BOLD, 15));
        
        cmbFiltrarCat = new JComboBox<>();
        cmbFiltrarCat.setBounds(207, 5, 147, 27);
        panel_7.add(cmbFiltrarCat);
        cmbFiltrarCat.setFont(new Font("Tahoma", Font.PLAIN, 15));
        cmbFiltrarCat.addItem("Todos");
        cmbFiltrarCat.addItem("Familia");
        cmbFiltrarCat.addItem("Amigos");
        cmbFiltrarCat.addItem("Trabajo");
        panel_6.setLayout(null);
        
        // --- Botones de importar y exportar ---
        btnImportar = new JButton("Cargar Contactos");
        btnImportar.setBounds(20, 109, 170, 83);
        panel_6.add(btnImportar);
        btnImportar.setFont(new Font("Tahoma", Font.PLAIN, 15));
        
        btnExportar = new JButton("Exportar a CSV");
        btnExportar.setBounds(20, 218, 170, 83);
        panel_6.add(btnExportar);
        btnExportar.setFont(new Font("Tahoma", Font.PLAIN, 15));
        
        lblNewLabel_2 = new JLabel("Inportar Contactos");
        lblNewLabel_2.setFont(new Font("Tahoma", Font.PLAIN, 20));
        panel_1.add(lblNewLabel_2);
        
        lblNewLabel = new JLabel("Análisis de Contactos");
        lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 21));
        panelEstadisticas.setLayout(null);
        panelEstadisticas.add(progressBar);
        panelEstadisticas.add(panel_1);
        panelEstadisticas.add(scrollTabla);
        panelEstadisticas.add(panel_7);
        panelEstadisticas.add(panel_6);
        
        lblNewLabel_1 = new JLabel("");
        lblNewLabel_1.setIcon(new ImageIcon("C:\\Users\\kchil\\Desktop\\imagens\\Imagen8.png"));
        lblNewLabel_1.setBounds(213, 129, 60, 46);
        panel_6.add(lblNewLabel_1);
        
        lblNewLabel_11 = new JLabel("");
        lblNewLabel_11.setIcon(new ImageIcon("C:\\Users\\kchil\\Desktop\\imagens\\Imagen9.png"));
        lblNewLabel_11.setBounds(213, 238, 60, 46);
        panel_6.add(lblNewLabel_11);
        panelEstadisticas.add(panel);
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(
        	gl_panel.createParallelGroup(Alignment.LEADING)
        		.addGroup(gl_panel.createSequentialGroup()
        			.addGap(324)
        			.addComponent(lblNewLabel, GroupLayout.PREFERRED_SIZE, 210, GroupLayout.PREFERRED_SIZE))
        );
        gl_panel.setVerticalGroup(
        	gl_panel.createParallelGroup(Alignment.LEADING)
        		.addGroup(gl_panel.createSequentialGroup()
        			.addGap(10)
        			.addComponent(lblNewLabel, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE))
        );
        panel.setLayout(gl_panel);

        // --- Menú contextual ---
        menuTabla = new JPopupMenu();
        menuModificar = new JMenuItem("Modificar");
        menuEliminar = new JMenuItem("Eliminar");
        menuTabla.add(menuModificar);
        menuTabla.add(menuEliminar);

        // ✅ SOLUCIÓN: NO configurar eventos aquí - solo en el controlador
        // configurarEventos(); // ❌ COMENTADO
        
        // Inicialización del controlador 
        controlador = new ControladorNuevo(this);
        controlador.inicializar(); // ✅ Los eventos se configuran SOLO en el controlador
        
        // WindowListener para limpiar recursos 
        agregarWindowListener();
    }

    /**
     * ✅ COMENTADO: Los eventos se configuran SOLO en el controlador
     */
    /*
    private void configurarEventos() {
        // ❌ ESTO CAUSA DUPLICACIÓN - COMENTADO
    }
    */
    
    /**
     * Agrega el WindowListener para limpiar recursos al cerrar la ventana
     */
    private void agregarWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                if (controlador != null) {
                    controlador.cerrarRecursos();
                }
            }
        });
    }
    
    /**
     * Método para cambiar de pestaña programáticamente
     * @param indice El índice de la pestaña a seleccionar
     */
    public void cambiarPestana(int indice) {
        tabbedPane.setSelectedIndex(indice);
    }
    
    /**
     * Método para obtener el controlador
     * @return El controlador de la aplicación
     */
    public ControladorNuevo getControlador() {
        return controlador;
    }
    
    /**
     * Método para establecer el controlador
     * @param controlador El controlador de la aplicación
     */
    public void setControlador(ControladorNuevo controlador) {
        this.controlador = controlador;
    }
}