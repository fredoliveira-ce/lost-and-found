package nl.fred.lostandfound.data.repository;

import nl.fred.lostandfound.domain.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    @Query("select coalesce(sum(c.quantity), 0) from Claim c where c.lostItem.id = :lostItemId")
    int sumQuantityByLostItemId(@Param("lostItemId") Long lostItemId);

    @Query("""
        select c.lostItem.id as lostItemId, sum(c.quantity) as totalQuantity
          from Claim c
         group by c.lostItem.id
        """)
    List<LostItemClaimedQuantity> sumQuantityGroupedByLostItemId();

}
