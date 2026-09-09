package nl.fred.lostandfound.data.repository;

import nl.fred.lostandfound.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
