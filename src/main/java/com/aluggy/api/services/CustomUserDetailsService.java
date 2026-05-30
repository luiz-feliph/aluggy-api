package com.aluggy.api.services;

import com.aluggy.api.entities.User;
import com.aluggy.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repository;


    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {

        return repository
                .findByUsernameOrEmail(login, login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
