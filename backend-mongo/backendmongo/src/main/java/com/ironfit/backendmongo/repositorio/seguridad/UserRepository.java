package com.ironfit.backendmongo.repositorio.seguridad;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.ironfit.backendmongo.modelo.seguridad.UserDocument;
import java.util.Optional;

public interface UserRepository extends MongoRepository<UserDocument, String> {
    Optional<UserDocument> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
}