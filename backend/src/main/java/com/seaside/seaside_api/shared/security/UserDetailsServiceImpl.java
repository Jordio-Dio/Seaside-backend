package com.seaside.seaside_api.shared.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.seaside.seaside_api.feature.utilisateur.repository.UtilisateurRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService { 
    
    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails  loadUserByUsername(String email) throws UsernameNotFoundException {
        // Spring Security  appelle cette methode avec email
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "Utilisateur non trouve avec email : " + email
                ));
    }
}
