package com.ironfit.backendmongo.servicio.seguridad;

import com.ironfit.backendmongo.dto.seguridad.RegisterRequest;
import com.ironfit.backendmongo.dto.seguridad.UserResponse;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import com.ironfit.backendmongo.repositorio.seguridad.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

import com.ironfit.backendmongo.dto.comun.PaginaResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;

@Service
public class UserService {

    private final UserRepository repositorioUsuario;
    private final PasswordEncoder passwordEncoder;
    private static final String PASSWORD_TEMPORAL = "Ironfit123*";
    private final MongoTemplate mongoTemplate;

    public UserService(UserRepository repositorioUsuario, PasswordEncoder passwordEncoder, MongoTemplate mongoTemplate) {
        this.repositorioUsuario = repositorioUsuario;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
    }

    public UserResponse crearUsuario(RegisterRequest solicitud) {
        if (repositorioUsuario.existsByCorreo(solicitud.getCorreo())) {
            throw new RuntimeException("Ya existe un usuario con ese correo");
        }

        UserDocument usuario = new UserDocument();
        usuario.setCorreo(solicitud.getCorreo());
        usuario.setPasswordHash(passwordEncoder.encode(solicitud.getPassword()));
        usuario.setRoles(solicitud.getRoles());
        usuario.setActivo(true);
        usuario.setNombres(solicitud.getNombres());
        usuario.setApellidos(solicitud.getApellidos());
        usuario.setTelefono(solicitud.getTelefono());
        usuario.setTipoDoc(solicitud.getTipoDoc());
        usuario.setNumDoc(solicitud.getNumDoc());

        UserDocument guardado = repositorioUsuario.save(usuario);
        return convertirAResponse(guardado);
    }

    public List<UserResponse> listarUsuarios() {
        return repositorioUsuario.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public PaginaResponse<UserResponse> listarUsuariosPaginados(
            int page,
            int size,
            String buscar,
            String rol,
            Boolean activo
    ) {
        int paginaActual = Math.max(page, 0);
        int tamanoPagina = size <= 0 ? 20 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                paginaActual,
                tamanoPagina,
                Sort.by(Sort.Direction.ASC, "nombres")
        );

        Query query = new Query();
        ArrayList<Criteria> criterios = new ArrayList<>();

        if (buscar != null && !buscar.trim().isEmpty()) {
            String texto = buscar.trim();

            criterios.add(new Criteria().orOperator(
                    Criteria.where("nombres").regex(texto, "i"),
                    Criteria.where("apellidos").regex(texto, "i"),
                    Criteria.where("correo").regex(texto, "i"),
                    Criteria.where("numDoc").regex(texto, "i")
            ));
        }

        if (rol != null && !rol.trim().isEmpty()) {
            criterios.add(Criteria.where("roles").in(rol.trim().toUpperCase()));
        }

        if (activo != null) {
            criterios.add(Criteria.where("activo").is(activo));
        }

        if (!criterios.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criterios.toArray(new Criteria[0])));
        }

        long totalElementos = mongoTemplate.count(query, UserDocument.class);

        query.with(pageable);

        List<UserResponse> contenido = mongoTemplate.find(query, UserDocument.class)
                .stream()
                .map(this::convertirAResponse)
                .toList();

        int totalPaginas = totalElementos == 0
                ? 0
                : (int) Math.ceil((double) totalElementos / tamanoPagina);

        boolean ultima = totalPaginas == 0 || paginaActual >= totalPaginas - 1;

        return new PaginaResponse<>(
                contenido,
                paginaActual,
                tamanoPagina,
                totalElementos,
                totalPaginas,
                ultima
        );
    }

    public UserResponse buscarPorCorreo(String correo) {
        UserDocument usuario = repositorioUsuario.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return convertirAResponse(usuario);
    }

    public UserResponse cambiarEstado(String id, boolean activo) {
        UserDocument usuario = repositorioUsuario.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setActivo(activo);
        UserDocument guardado = repositorioUsuario.save(usuario);

        return convertirAResponse(guardado);
    }

    private UserResponse convertirAResponse(UserDocument usuario) {
        UserResponse respuesta = new UserResponse();
        respuesta.setId(usuario.getId());
        respuesta.setCorreo(usuario.getCorreo());
        respuesta.setRoles(usuario.getRoles());
        respuesta.setActivo(usuario.isActivo());
        respuesta.setNombres(usuario.getNombres());
        respuesta.setApellidos(usuario.getApellidos());
        respuesta.setTelefono(usuario.getTelefono());
        respuesta.setTipoDoc(usuario.getTipoDoc());
        respuesta.setNumDoc(usuario.getNumDoc());
        return respuesta;
    }

    public UserResponse actualizarMiPerfil(String correo, UserResponse solicitud) {
        UserDocument usuario = repositorioUsuario.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setNombres(solicitud.getNombres());
        usuario.setApellidos(solicitud.getApellidos());
        usuario.setTelefono(solicitud.getTelefono());
        usuario.setCorreo(solicitud.getCorreo());

        UserDocument guardado = repositorioUsuario.save(usuario);

        return convertirAResponse(guardado);
    }

    public void cambiarMiPassword(String correo, String nuevaPassword) {
        UserDocument usuario = repositorioUsuario.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));

        repositorioUsuario.save(usuario);
    }

    public void resetearPassword(String id) {
        UserDocument usuario = repositorioUsuario.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD_TEMPORAL));

        repositorioUsuario.save(usuario);
    }

    public List<UserResponse> buscarClientes(String texto) {
    return repositorioUsuario.findAll()
            .stream()
            .filter(usuario -> usuario.getRoles().contains("CLIENTE"))
            .filter(usuario ->
                    (usuario.getNombres() != null && usuario.getNombres().toLowerCase().contains(texto.toLowerCase())) ||
                    (usuario.getNumDoc() != null && usuario.getNumDoc().contains(texto))
            )
            .map(this::convertirAResponse)
            .toList();
}
}