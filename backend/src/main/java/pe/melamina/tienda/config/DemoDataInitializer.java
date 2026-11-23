package pe.melamina.tienda.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.melamina.tienda.entity.Categoria;
import pe.melamina.tienda.entity.Producto;
import pe.melamina.tienda.entity.TipoProducto;
import pe.melamina.tienda.repository.CategoriaRepository;
import pe.melamina.tienda.repository.ProductoRepository;
import pe.melamina.tienda.repository.UsuarioRepository;
import pe.melamina.tienda.entity.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.melamina.tienda.service.AlmacenamientoService;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static pe.melamina.tienda.entity.TipoProducto.*;

/**
 * Carga productos de muestra para ver la tienda con contenido.
 * Solo inserta los que no existen (se identifican por SKU), así que puedes reiniciar sin duplicar.
 * Para no cargarlos más, pon app.demo.cargar: false en application.yml.
 * Para quitarlos, elimínalos desde el panel (todos tienen SKU que empieza con "DEMO-").
 * Los precios son referenciales: reemplázalos por los reales de MelaminCorp.
 */
@Component
@Order(2)
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AlmacenamientoService almacenamientoService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo.cargar:true}")
    private boolean cargar;

    public DemoDataInitializer(ProductoRepository productoRepository, CategoriaRepository categoriaRepository,
                               AlmacenamientoService almacenamientoService, UsuarioRepository usuarioRepository,
                               PasswordEncoder passwordEncoder) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.almacenamientoService = almacenamientoService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Un usuario de prueba por rol. Contraseña: Melamin123! (cámbiala o desactívalos en producción). */
    private void crearUsuariosDemo() {
        String[][] demo = {
                {"ventas@melamincorp.pe", "Atención y Ventas (demo)", "VENTAS"},
                {"almacen@melamincorp.pe", "Almacén (demo)", "ALMACEN"},
                {"taller@melamincorp.pe", "Operario de taller (demo)", "OPERARIO"},
        };
        for (String[] d : demo) {
            if (usuarioRepository.findByEmailIgnoreCase(d[0]).isPresent()) continue;
            Usuario u = new Usuario();
            u.setEmail(d[0]);
            u.setNombre(d[1]);
            u.setRol(d[2]);
            u.setPassword(passwordEncoder.encode("Melamin123!"));
            u.setActivo(true);
            usuarioRepository.save(u);
            log.info("Usuario de prueba creado: {} ({})", d[0], d[2]);
        }
    }

    private record Demo(String sku, String nombre, TipoProducto tipo, String categoria,
                        String precio, String oferta, int stock,
                        String medidas, String color, String espesor,
                        String imagen, boolean destacado, boolean soloCotizacion, String descripcion) {}

    private static final List<Demo> PRODUCTOS = List.of(
            // ---------- Servicios ----------
            new Demo("DEMO-SV-OPT", "Optimización y despiece con Lepton Optimizer", SERVICIO, "Optimización y despiece",
                    "0.00", null, 0, "Por proyecto", null, null, "servicio-optimizacion.jpg", true, true,
                    "Con tu lista de piezas calculamos el plan de cortes que mejor aprovecha cada plancha, para que compres solo lo necesario.\nSin costo al comprar tus planchas con nosotros."),
            new Demo("DEMO-SV-COR", "Corte a medida", SERVICIO, "Corte a medida",
                    "15.00", null, 0, "Por plancha", null, null, "servicio-corte.jpg", true, true,
                    "Cortamos tus piezas con precisión en maquinaria especializada, siguiendo el despiece aprobado.\nEl precio final depende de la cantidad de cortes."),
            new Demo("DEMO-SV-CAD", "Canteado con tapacanto delgado (0.45 mm)", SERVICIO, "Canteado",
                    "1.50", null, 0, "Por metro lineal", "Del color de tu plancha", "0.45 mm", "servicio-canteado-delgado.jpg", false, true,
                    "Pegado de tapacanto en los bordes visibles de las piezas, con verificación del acabado. Ideal para piezas interiores."),
            new Demo("DEMO-SV-CAG", "Canteado con tapacanto grueso (2 mm)", SERVICIO, "Canteado",
                    "3.00", null, 0, "Por metro lineal", "Del color de tu plancha", "2 mm", "servicio-canteado-grueso.jpg", false, true,
                    "Tapacanto de mayor resistencia para frentes, puertas y bordes expuestos a golpes."),
            new Demo("DEMO-SV-INS", "Instalación en obra", SERVICIO, "Instalación",
                    "150.00", null, 0, "Según proyecto", null, null, "servicio-instalacion.jpg", true, true,
                    "Armamos e instalamos tus muebles en Lima. Coordinamos la visita y el precio según el tamaño del trabajo.")
    );

    /** Catálogo real de colores de MelaminCorp (imágenes en resources/demo/muestra-NN-*.jpg). */
    private record Muestra(int numero, String nombre, String archivo, char grupo) {}

    private static final String MADERADOS = "Maderados";
    private static final String UNICOLOR = "Unicolor";
    private static final String ESPECIALES = "Texturas especiales";

    // grupo: M = maderado, U = unicolor, E = textura especial
    private static final List<Muestra> MUESTRAS = List.of(
            new Muestra(1, "Panela", "panela", 'M'), new Muestra(2, "Rovere", "rovere", 'M'),
            new Muestra(3, "Taupe", "taupe", 'U'), new Muestra(4, "Fume", "fume", 'M'),
            new Muestra(5, "Caramelo", "caramelo", 'M'), new Muestra(6, "Nácar", "nacar", 'M'),
            new Muestra(7, "Castaño", "castano", 'M'), new Muestra(8, "Arupo", "arupo", 'U'),
            new Muestra(9, "Ónix", "onix", 'E'), new Muestra(10, "Gales", "gales", 'M'),
            new Muestra(11, "Duna", "duna", 'M'), new Muestra(12, "Niebla", "niebla", 'U'),
            new Muestra(13, "Agave", "agave", 'U'), new Muestra(14, "Tivoli", "tivoli", 'M'),
            new Muestra(15, "Capri", "capri", 'U'), new Muestra(16, "Espresso", "espresso", 'M'),
            new Muestra(17, "Cemento", "cemento", 'M'), new Muestra(18, "Negro", "negro", 'U'),
            new Muestra(19, "Coñac", "conac", 'M'), new Muestra(20, "Lino", "lino", 'U'),
            new Muestra(21, "Milán", "milan", 'M'), new Muestra(22, "Inox", "inox", 'E'),
            new Muestra(23, "Galápagos", "galapagos", 'E'), new Muestra(24, "Plomo", "plomo", 'U'),
            new Muestra(25, "Roble Negro", "roble-negro", 'M'), new Muestra(26, "Gris", "gris", 'U'),
            new Muestra(27, "Cedro", "cedro", 'M'), new Muestra(28, "Roble Gris", "roble-gris", 'M'),
            new Muestra(29, "Bronce", "bronce", 'E'), new Muestra(30, "Burdeos", "burdeos", 'U'),
            new Muestra(31, "Cartagena", "cartagena", 'M'), new Muestra(32, "Macadamia", "macadamia", 'M'),
            new Muestra(33, "Roble Blanco", "roble-blanco", 'M'), new Muestra(34, "Miel", "miel", 'M'),
            new Muestra(35, "Bardolino", "bardolino", 'M'), new Muestra(36, "Manzano", "manzano", 'M'),
            new Muestra(37, "Avellana", "avellana", 'M'), new Muestra(38, "Positano", "positano", 'M'),
            new Muestra(39, "Mamba", "mamba", 'E'), new Muestra(40, "Nazca", "nazca", 'M')
    );

    /** Colores que se muestran en el inicio. */
    private static final java.util.Set<Integer> DESTACADAS = java.util.Set.of(1, 13, 16, 39);

    private static final java.util.Map<String, String> DETALLE_ESPECIAL = java.util.Map.of(
            "Ónix", "textura de piedra", "Inox", "efecto acero cepillado",
            "Galápagos", "textura en relieve", "Bronce", "efecto metálico", "Mamba", "efecto mármol");

    private void crearCategoriasCatalogo() {
        crearCategoria(MADERADOS, "Planchas con veta de madera");
        crearCategoria(UNICOLOR, "Planchas de color liso");
        crearCategoria(ESPECIALES, "Acabados de piedra, mármol y metálicos");
    }

    private void crearCategoria(String nombre, String descripcion) {
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) return;
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        c.setTipo(PLANCHA);
        categoriaRepository.save(c);
    }

    /** Crea las 40 planchas del catálogo real (no duplica: usa el SKU MC-PL-NN). */
    private int crearMuestras() {
        int creados = 0;
        // Se crean de la 40 a la 1 para que el catálogo ("más recientes") empiece por la 1
        for (int i = MUESTRAS.size() - 1; i >= 0; i--) {
            Muestra m = MUESTRAS.get(i);
            String sku = String.format("MC-PL-%02d", m.numero());
            String imagen = String.format("muestra-%02d-%s.jpg", m.numero(), m.archivo());
            if (productoRepository.existsBySkuIgnoreCase(sku)) {
                copiarImagen(imagen);
                continue;
            }
            String categoria;
            String precio;
            int stock;
            String descripcion;
            switch (m.grupo()) {
                case 'U' -> {
                    categoria = UNICOLOR; precio = "199.00"; stock = 25;
                    descripcion = "Plancha de melamina unicolor " + m.nombre() + ", de acabado liso y fácil de limpiar. "
                            + "Combina con los maderados en frentes, puertas y detalles.";
                }
                case 'E' -> {
                    categoria = ESPECIALES; precio = "239.00"; stock = 12;
                    descripcion = "Plancha de melamina de acabado especial " + m.nombre() + " ("
                            + DETALLE_ESPECIAL.getOrDefault(m.nombre(), "textura especial")
                            + "). Para frentes y muebles que buscan destacar.";
                }
                default -> {
                    categoria = MADERADOS; precio = "215.00"; stock = 20;
                    descripcion = "Plancha de melamina con veta de madera en tono " + m.nombre()
                            + ". Ideal para closets, cocinas, escritorios y muebles de sala.";
                }
            }
            Producto p = new Producto();
            p.setSku(sku);
            p.setNombre("Plancha de melamina " + m.nombre() + " 18 mm");
            p.setTipo(PLANCHA);
            p.setCategoria(categoriaRepository.findByNombreIgnoreCase(categoria).orElse(null));
            p.setPrecio(new BigDecimal(precio)); // precio referencial: reemplázalo por el real
            p.setStock(stock);
            p.setMedidas("2.44 x 2.14 m");
            p.setColor(m.nombre());
            p.setEspesor("18 mm");
            p.setDescripcion(descripcion + "\nLa optimizamos, cortamos y canteamos a tu medida.");
            p.setDestacado(DESTACADAS.contains(m.numero()));
            p.setSoloCotizacion(false);
            p.setActivo(true);
            p.setImagenUrl(copiarImagen(imagen));
            productoRepository.save(p);
            creados++;
        }
        return creados;
    }

    /** Las planchas con imágenes generadas (DEMO-PL-*) se ocultan al cargar el catálogo real por primera vez. */
    private void ocultarPlanchasGeneradas() {
        productoRepository.findAll().stream()
                .filter(p -> p.getSku() != null && p.getSku().startsWith("DEMO-PL-") && p.isActivo())
                .forEach(p -> p.setActivo(false));
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!cargar) return;
        crearUsuariosDemo();
        crearCategoriasCatalogo();
        boolean primeraCargaCatalogo = !productoRepository.existsBySkuIgnoreCase("MC-PL-01");
        int muestras = crearMuestras();
        if (primeraCargaCatalogo) ocultarPlanchasGeneradas();
        if (muestras > 0) log.info("Planchas del catálogo MelaminCorp creadas: {}", muestras);
        int creados = 0;
        for (Demo d : PRODUCTOS) {
            if (productoRepository.existsBySkuIgnoreCase(d.sku())) {
                copiarImagen(d.imagen()); // asegura que la foto exista en la base (no duplica)
                continue;
            }

            Producto p = new Producto();
            p.setSku(d.sku());
            p.setNombre(d.nombre());
            p.setTipo(d.tipo());
            p.setCategoria(categoriaRepository.findByNombreIgnoreCase(d.categoria()).orElse(null));
            p.setPrecio(new BigDecimal(d.precio()));
            p.setPrecioOferta(d.oferta() != null ? new BigDecimal(d.oferta()) : null);
            p.setStock(d.stock());
            p.setMedidas(d.medidas());
            p.setColor(d.color());
            p.setEspesor(d.espesor());
            p.setDescripcion(d.descripcion());
            p.setDestacado(d.destacado());
            p.setSoloCotizacion(d.soloCotizacion());
            p.setActivo(true);
            p.setImagenUrl(copiarImagen(d.imagen()));
            productoRepository.save(p);
            creados++;
        }
        if (creados > 0) log.info("Productos de muestra creados: {}", creados);
    }

    /** Copia la imagen desde src/main/resources/demo a la carpeta de uploads. */
    private String copiarImagen(String archivo) {
        try (InputStream in = new ClassPathResource("demo/" + archivo).getInputStream()) {
            return almacenamientoService.guardarArchivo("demo-" + archivo, in);
        } catch (Exception e) {
            log.warn("No se pudo copiar la imagen de muestra {}: {}", archivo, e.getMessage());
            return null;
        }
    }
}
