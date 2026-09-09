package nl.fred.lostandfound.data.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import nl.fred.lostandfound.domain.entity.LostItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LostItemRepository extends JpaRepository<LostItem, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from LostItem l where l.id = :id")
  Optional<LostItem> findByIdForUpdate(@Param("id") Long id);

  @Query("select distinct l.place from LostItem l")
  List<String> findDistinctPlaces();

}
