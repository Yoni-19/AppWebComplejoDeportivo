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
    private final CurrentUserService currentUser;

    public UsuarioService(final UsuarioRepository repository, final ReservaRepository reservaRepository,
            final PasswordEncoder passwordEncoder, final CurrentUserService currentUser) {
        this.repository = repository;
        this.reservaRepository = reservaRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
    }

    /**
     * Un ADMIN ve a todos menos a sí mismo; un RECEPCIONISTA ve solo clientes.
     * El filtro por rol es opcional y lo usan las pantallas que necesitan
     * únicamente clientes (selector de reservas, contador del dashboard).
     */
    public List<Usuario> listar(Rol filtro) {
        Usuario actual = currentUser.get();
        if (!Rol.ADMIN.equals(actual.getRol())) {
            return repository.findByRol(Rol.CLIENTE);
        }

        List<Usuario> usuarios = filtro != null ? repository.findByRol(filtro) : repository.findAll();
        return usuarios.stream()
                .filter(usuario -> !usuario.getId().equals(actual.getId()))
                .toList();
    }

    public Optional<Usuario> buscarPorId(Integer id) {
        Optional<Usuario> usuario = repository.findById(id);
        // Un recepcionista no puede abrir la ficha de un admin u otro recepcionista.
        if (usuario.isPresent() && !Rol.ADMIN.equals(currentUser.rol())
                && !Rol.CLIENTE.equals(usuario.get().getRol())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No podés consultar a un usuario del personal");
        }
        return usuario;
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

        // Un recepcionista puede corregir clientes, nunca al personal ni a su propia cuenta.
        if (!Rol.ADMIN.equals(currentUser.rol()) && !Rol.CLIENTE.equals(existente.getRol())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No podés editar a un usuario del personal");
        }

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

        if (usuario.getId().equals(currentUser.get().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No podés cambiar tu propio rol");
        }

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
        if (id.equals(currentUser.get().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No podés eliminar tu propia cuenta");
        }
        if (reservaRepository.existsByUsuario_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el usuario porque tiene reservas asociadas");
        }
        repository.deleteById(id);
    }
}
