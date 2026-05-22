package com.aluggy.api.repositories;

import com.aluggy.api.entities.ProfilePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PhotoProfileRepository extends JpaRepository<ProfilePhoto, UUID> {
}
