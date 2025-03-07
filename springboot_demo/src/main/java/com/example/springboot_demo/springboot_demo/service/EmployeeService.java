package com.example.springboot_demo.springboot_demo.service;

import java.util.List;

import com.example.springboot_demo.springboot_demo.entity.Employee;

public interface EmployeeService {
    Employee saveEmployee(Employee employee);
    List<Employee> getAllEmployees();
    Employee getEmployeeById(long id);
    Employee updateEmployee(Employee employee, long id);
    void deleteEmployee(long id);
}
