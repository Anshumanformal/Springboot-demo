package com.example.springboot_demo.springboot_demo.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// import org.springframework.security.core.userdetails.UserDetails;


@Data // A shortcut for generating getters, setters, toString, equals, and hashCode methods
/* @Entity
 * -> Marks this class as a persistent entity that maps to a database table.
 * -> The class annotated with @Entity will be managed by an ORM (like Hibernate), 
 * meaning it will be saved to or retrieved from a database table.
 * -> The fields of the class correspond to the columns in the database table.
*/
@Entity
@NoArgsConstructor
@AllArgsConstructor
// It is a Lombok annotation that provides a flexible way to construct an instance of the class with the Builder pattern.
@Builder
@Table(name="employees")
public class Employee implements UserDetails {

    /*
     * @Id: Defines the primary key.
       @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq"): Uses a sequence for auto-generating primary key values.
       @SequenceGenerator: Specifies the name of the sequence (user_sequence), how the sequence is generated, and its allocation size for batch inserts.
    */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_Seq", sequenceName = "user_sequence", allocationSize = 1)
    // it's default value is null, so useful when used for databases entries.
    private Long id;
    // it's default value is 0, so it's best for performance or guaranteed value (no nulls)
    // private long id;

    @Embedded
    @Valid
    private  Username name;

    @Column(unique = true, nullable = false)
    @Email(message = "Enter a valid email")
    @NotBlank(message = "Email can't be blank")
    private String email;

    @NotBlank(message = "Password can't be blank")
    @Column(nullable = false)
    private String Password;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Choose your gender please")
    private Gender gender;

    @Column(unique = true)
    private String phoneNumber;

    @Column(length = 1000)
    private String profilePicture;

    private Boolean isVerified ;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getFullName(){
        return name.getFirstName() + " " + name.getLastName();
    }
}
