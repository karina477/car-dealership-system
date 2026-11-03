package org.example.mainbackendservice.repository;

import org.example.mainbackendservice.model.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    Optional<Car> findByVinIgnoreCase(String vin);

    @Query("SELECT c FROM Car c WHERE " +
            "(:brand IS NULL OR LOWER(c.brand) = LOWER(CAST(:brand AS string))) AND " +
            "(:minPrice IS NULL OR c.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR c.price <= :maxPrice) AND " +
            "(c.status IN :statuses)")
    Page<Car> findCarsWithFilters(
            @Param("brand") String brand,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("statuses") List<String> statuses,
            Pageable pageable
    );
}
