package com.example.springboot_demo.springboot_demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.springboot_demo.springboot_demo.model.Employee;

public interface EmployeeRepository extends JpaRepository <Employee, Long>{
    
}
