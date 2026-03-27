package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Create roles if they don't exist
        createRoleIfNotFound("ROLE_ADMIN");
        createRoleIfNotFound("ROLE_MODERATOR");
        createRoleIfNotFound("ROLE_USER");
    }

    private void createRoleIfNotFound(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
        }
    }
}
