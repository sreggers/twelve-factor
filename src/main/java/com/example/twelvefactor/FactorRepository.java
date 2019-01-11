package com.example.twelvefactor;

import lombok.Data;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@RepositoryRestResource
public interface FactorRepository extends CrudRepository<FactorRepository.Factor, Long> {

    @Data
    @Entity
    class Factor {

        @Id
        @GeneratedValue
        private Long id;
        private Integer number;
        private String name;
        private String description;
    }
}
