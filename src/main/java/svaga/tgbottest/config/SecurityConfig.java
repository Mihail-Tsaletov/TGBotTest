package svaga.tgbottest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/tg/admin/**").authenticated()  // Только авторизованные в админку
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().permitAll()  // Остальное — свободно (бот, API и т.д.)
                )
                .formLogin(form -> form
                        .loginPage("/tg/login")                    // Кастомная страница логина
                        .loginProcessingUrl("/tg/login")           // POST сюда отправляет форму (по умолчанию)
                        .defaultSuccessUrl("/tg/admin/orders", true)  // После входа — в ожидающие
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/tg/logout")
                        .logoutSuccessUrl("/tg/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/tg/admin/broadcast/send", "/tg/internal/**")
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")  // Логин
                .password(passwordEncoder().encode("ARINA_1997"))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}