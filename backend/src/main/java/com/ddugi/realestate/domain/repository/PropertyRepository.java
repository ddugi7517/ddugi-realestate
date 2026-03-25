package com.ddugi.realestate.domain.repository;

import com.ddugi.realestate.domain.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    boolean existsByNameAndRegionCode(String name, String regionCode);

    List<Property> findByRegionCode(String regionCode);
}
