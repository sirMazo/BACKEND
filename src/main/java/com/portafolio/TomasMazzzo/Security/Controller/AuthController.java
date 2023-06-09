/**
 *
 * @author sirM
 */
package com.portafolio.TomasMazzzo.Security.Controller;

import com.portafolio.TomasMazzzo.Security.Dto.JwtDto;
import com.portafolio.TomasMazzzo.Security.Dto.LoginUsuario;
import com.portafolio.TomasMazzzo.Security.Dto.NuevoUsuario;
import com.portafolio.TomasMazzzo.Security.Entity.Usuario;
import com.portafolio.TomasMazzzo.Security.Entity.rol;
import com.portafolio.TomasMazzzo.Security.Enums.RolNombre;
import com.portafolio.TomasMazzzo.Security.Service.RolService;
import com.portafolio.TomasMazzzo.Security.Service.UsuarioService;
import com.portafolio.TomasMazzzo.Security.jwt.JwtProvider;
import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    UsuarioService usuarioService;
    @Autowired
    RolService rolService;
    @Autowired
    JwtProvider jwtProvider;

    @PostMapping("/nuevo")
    public ResponseEntity<?> nuevo(@Valid @RequestBody NuevoUsuario nuevoUsuario, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new ResponseEntity<>(new Mensaje("Campos mal puestos o email inválido"), HttpStatus.BAD_REQUEST);
        }

        if (usuarioService.existByNombreUsuario(nuevoUsuario.getNombreUsuario())) {
            return new ResponseEntity<>(new Mensaje("Ese nombre de usuario ya existe"), HttpStatus.BAD_REQUEST);
        }

        if (usuarioService.existByMail(nuevoUsuario.getMail())) {
            return new ResponseEntity<>(new Mensaje("Este Email de usuario ya existe"), HttpStatus.BAD_REQUEST);
        }

        Usuario usuario = new Usuario(nuevoUsuario.getNombre(), nuevoUsuario.getNombreUsuario(), nuevoUsuario.getMail(), passwordEncoder.encode(nuevoUsuario.getPassword()));

        Set<rol> roles = new HashSet<>();
        roles.add(rolService.getByRolNombre(RolNombre.ROLE_USER).get());

        if (nuevoUsuario.getRoles().contains("Admin")) {
            roles.add(rolService.getByRolNombre(RolNombre.ROLE_ADMIN).get());
            
        }
        usuario.setRoles(roles);
        usuarioService.save(usuario);

        return new ResponseEntity<>(new Mensaje("Usuario guardado"), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtDto> login(@Valid @RequestBody LoginUsuario loginUsuario, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new ResponseEntity(new Mensaje("Campos mal puestos"), HttpStatus.BAD_REQUEST);
        }

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUsuario.getNombreUsuario(), loginUsuario.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtProvider.generateToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        JwtDto jwtDto = new JwtDto(jwt, userDetails.getUsername(), userDetails.getAuthorities());

        return new ResponseEntity(jwtDto, HttpStatus.OK);
    }
}
