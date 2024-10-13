// package com.example.springboot_demo.springboot_demo.service.implementation;

// import java.util.List;
// import java.util.Optional;

// import org.springframework.stereotype.Service;

// import com.example.springboot_demo.springboot_demo.controller.ResourceNotFoundException;
// import com.example.springboot_demo.springboot_demo.entity.Employee;
// import com.example.springboot_demo.springboot_demo.repository.EmployeeRepository;
// import com.example.springboot_demo.springboot_demo.service.EmployeeService;

// @Service
// public class EmployeeServiceImpl implements EmployeeService{

//     private EmployeeRepository employeeRepository;

//     public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
//         super();
//         this.employeeRepository = employeeRepository;
//     }

//     @Override
//     public Employee saveEmployee(Employee employee){
//         return employeeRepository.save(employee);
//     }

//     @Override
//     public List<Employee> getAllEmployees() {
//         return employeeRepository.findAll();
//     }

//     @Override
//     public Employee getEmployeeById(long id) {
//         Optional<Employee> employee = employeeRepository.findById(id);
//         if(employee.isPresent()){
//             return employee.get();
//         }
//         else{
//             throw new ResourceNotFoundException("Employee", "ID", id);
//         }

//         // One -line code for the above code.
//         // return employeeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
//     }

//     @Override
//     public Employee updateEmployee(Employee employee, long id) {
//         // check whether employee with given id is present in DB or not
//         Employee existingEmployee = employeeRepository.findById(id).orElseThrow(
//             () -> new ResourceNotFoundException("Employee", "Id", id)
//         );

//         existingEmployee.setFirstName(employee.getFirstName());
//         existingEmployee.setLastName(employee.getLastName());
//         existingEmployee.setEmail(employee.getEmail());

//         // Save new data to DB
//         employeeRepository.save(existingEmployee);
//         return existingEmployee;
//     }

//     @Override
//     public void deleteEmployee(long id) {
//         // check whether employee with given id is present in DB or not
//         employeeRepository.findById(id).orElseThrow(
//             () -> new ResourceNotFoundException("Employee", "Id", id)
//         );

//         employeeRepository.deleteById(id);
//     }
    
// }
