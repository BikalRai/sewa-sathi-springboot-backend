    package raicod3.example.com.repository;


    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.stereotype.Repository;
    import raicod3.example.com.model.User;

    import java.util.List;
    import java.util.Optional;
    import java.util.UUID;

    @Repository
    public interface UserRepository extends JpaRepository<User, UUID> {

        Optional<User> findUserByEmail(String email);

        Optional<User> findUserByEmailAndPassword(String email, String password);

        Optional<User> findByProviderId(String sub);

        Optional<User> findByEmail(String email);

        Page<User> findAll(Pageable pageable);
    }
