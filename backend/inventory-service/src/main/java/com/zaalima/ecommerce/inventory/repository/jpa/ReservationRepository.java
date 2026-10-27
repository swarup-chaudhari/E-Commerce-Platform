package com.zaalima.ecommerce.inventory.repository.jpa;

import com.zaalima.ecommerce.inventory.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, String> {
}
