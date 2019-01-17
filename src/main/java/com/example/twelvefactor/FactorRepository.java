package com.example.twelvefactor;

import lombok.Data;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import static javax.persistence.GenerationType.IDENTITY;

@RepositoryRestResource
public interface FactorRepository extends CrudRepository<FactorRepository.Factor, Long> {

    @Data
    @Entity(name = "factor")
    class Factor {

        @Id
        @GeneratedValue(strategy = IDENTITY)
        private Long id;
        private Integer number;
        private String name;
        private String statement;
    }
}