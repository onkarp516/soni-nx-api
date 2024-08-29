package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
     Employee findByIdAndStatus(long parseLong, boolean b);
    Employee findByIdAndInstituteIdAndStatus(long parseLong,long instituteId, boolean b);

//    Page<Employee> findByOrderByIdDesc(Pageable pageable);
    @Query(value = " SELECT * FROM employee_tbl WHERE mobile_number=?1 AND device_id=?2", nativeQuery = true)
    Employee findByMobileNumberAndDeviceId(String mobile, String deviceId);

    @Query(value = " SELECT * FROM employee_tbl WHERE mobile_number=?1 AND text_password=?2", nativeQuery = true)
    Employee findByMobileNumberAndPassword(String mobile, String password);

    @Query(value = " SELECT * FROM employee_tbl WHERE mobile_number=?1 AND text_password=?2 AND status=?3", nativeQuery = true)
    Employee findByMobileNumberAndPasswordAndStatus(String mobile, String password, Boolean b);

    @Query(value = " SELECT * FROM employee_tbl WHERE mobile_number=?1 AND device_id=?2 AND text_password=?3", nativeQuery = true)
    Employee findByMobileNumberAndDeviceIdAndTextPassword(String mobile, String deviceId, String password);
    Employee findByMobileNumber(long parseLong);
    Employee findByDeviceId(String deviceId);
    List<Employee> getEmployeesByDeviceId(String deviceId);
    Employee findByMobileNumberAndDeviceId(long parseLong, String deviceId);
    Employee findByInstituteIdAndMobileNumber(long instituteId, long parseLong);
    List<Employee> findByStatus(boolean b);

    List<Employee> findAllByStatus(boolean b);
    List<Employee> findAllByInstituteIdAndStatus(long instituteId, boolean b);

    @Query(value = " SELECT IFNULL(COUNT(id),0) FROM employee_tbl as a WHERE a.status=?1 AND institute_id=?2", nativeQuery = true)
    int getEmployeeCount(boolean b, Long instituteId);

    @Query(value = " SELECT IFNULL(COUNT(id),0) FROM employee_tbl as a WHERE a.institute_id=?1 AND a.status=?2", nativeQuery = true)
    int getEmployeeCountOfInstitute(long instituteId, boolean b);

    List<Employee> findAllByOrderByFirstName();

    List<Employee> findByInstituteIdOrderByFirstName(long instituteId);
    List<Employee> findAllByOrderByLastName();

    @Query(value = "SELECT * from employee_tbl WHERE status=?3 AND institute_id=?4 AND id NOT IN " +
            "(SELECT employee_tbl.id FROM shift_assign_tbl,employee_tbl WHERE employee_tbl.status=?3 AND employee_tbl.institute_id=?4 AND shift_assign_tbl.employee_id=employee_tbl.id AND " +
            "((?1 BETWEEN shift_assign_tbl.from_date AND shift_assign_tbl.to_date) OR " +
            "(?2 BETWEEN shift_assign_tbl.from_date AND shift_assign_tbl.to_date)))", nativeQuery = true)
    List<Employee> findEmployeeIdAndStatus(String fromDate, String toDate, boolean b, long institute_id);

    List<Employee> findByCompanyIdAndStatus(Long valueOf, boolean b);
    List<Employee> findByInstituteIdAndCompanyIdAndStatus(long instituteId, Long valueOf, boolean b);

    @Query(value = "SELECT COUNT(employee_tbl.id) FROM employee_tbl WHERE employee_tbl.shift_id=?2 AND employee_tbl.status=?1 AND employee_tbl.institute_id=?3", nativeQuery = true)
    int getEmployeeCountByShiftNew(boolean b, Long valueOf, long institute_id);

    List<Employee> findByInstituteIdAndStatusOrderByFirstNameAsc(long instituteId, boolean b);
    List<Employee> findByStatusOrderByFirstNameAsc(boolean b);

    List<Employee> findByInstituteIdAndStatus(Long id, boolean b);

    @Query(value = "SELECT * FROM `employee_tbl` WHERE mobile_number=?1 AND text_password=?2", nativeQuery = true)
    Employee getByMobileNumberAndTextPassword(String username, String password);
}
