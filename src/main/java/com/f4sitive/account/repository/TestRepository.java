package com.f4sitive.account.repository;

import com.f4sitive.account.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepository extends JpaRepository<Test,String> {
}
