package com.proyectosw.sreservas.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.repository.ReservaRepository;
import com.proyectosw.sreservas.repository.UsuarioRepository;

import jakarta.transaction.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final ReservaRepository reservaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(final UsuarioRepository repository, final ReservaRepository reservaRepository,
            final PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.reservaRepository = reservaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    public Optional<Usuario> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional 
    public Usuario crear(Usuario usuario) {
        if (usuario.getClave() == null || usuario.getClave().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clave es obligatoria");
        }
        if (repository.findByCorreo(usuario.getCorreo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        // El alta es publica: la cuenta nace siempre como CLIENTE. El rol se cambia despues
        // con PUT /api/usuarios/{id}/rol, que solo puede usar un ADMIN.
        usuario.setRol(Rol.CLIENTE);
        usuario.setClave(passwordEncoder.encode(usuario.getClave()));
        return repository.save(usuario);
    }

    @Transactional
    public Usuario actualizar(Integer id, Usuario datos) {
        Usuario existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el usuario con id " + id));

        Optional<Usuario> conMismoCorreo = repository.findByCorreo(datos.getCorreo());
        if (conMismoCorreo.isPresent() && !conMismoCorreo.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        existente.setNombres(datos.getNombres());
        existente.setApellidos(datos.getApellidos());
        existente.setCorreo(datos.getCorreo());
        if (datos.getClave() != null)
            existente.setClave(passwordEncoder.encode(datos.getClave()));

        return repository.save(existente);
    }

    @Transactional
    public Usuario cambiarRol(Integer id, Rol rol) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el usuario con id " + id));

        // Sin esta guarda, el ultimo ADMIN puede degradarse y nadie queda habilitado
        // para volver a asignar roles: el sistema quedaria trabado hasta tocar la base.
        if (Rol.ADMIN.equals(usuario.getRol()) && !Rol.ADMIN.equals(rol)
                && repository.countByRol(Rol.ADMIN) == 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede quitar el rol del último administrador");
        }

        usuario.setRol(rol);
        return repository.save(usuario);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No existe el usuario con id " + id);
        }
        if (reservaRepository.existsByUsuario_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el usuario porque tiene reservas asociadas");
        }
        repository.deleteById(id);
    }
}
