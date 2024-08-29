package com.truethic.soninx.SoniNxAPI.util;

import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.repository.AttendanceRepository;
import com.truethic.soninx.SoniNxAPI.repository.EmployeeRepository;
import com.truethic.soninx.SoniNxAPI.repository.EmployeeSalaryRepository;
import com.truethic.soninx.SoniNxAPI.repository.TaskMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import static java.time.temporal.ChronoUnit.SECONDS;

@Service
public class Utility {
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private TaskMasterRepository taskRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private EmployeeSalaryRepository employeeSalaryRepository;

    public String getEmployeeName(Employee employee) {
        String employeeName = employee.getFirstName();
        if (employee.getMiddleName() != null)
            employeeName = employeeName + " " + employee.getMiddleName();
        if (employee.getLastName() != null)
            employeeName = employeeName + " " + employee.getLastName();
        return employeeName;
    }

    public Double getEmployeeWages(Long employeeId) {
        return employeeSalaryRepository.getEmployeeSalary(employeeId, LocalDate.now());
    }

    public Double getTimeInDouble(String time){
        String[] timeParts = time.toString().split(":");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        return  Double.parseDouble(hours+"."+minutes);
    }

    public LocalTime getTimeDiffFromTimes(LocalTime l1, LocalTime l2) {
        int s = (int) SECONDS.between(l2, l1);
        System.out.println(" s ------------------->----------------------------------- " + s);
        int sec = Math.abs(s % 60);
        int min = Math.abs((s / 60) % 60);
        int hours = Math.abs((s / 60) / 60);

        String strSec = (sec < 10) ? "0" + sec : Integer.toString(sec);
        String strmin = (min < 10) ? "0" + min : Integer.toString(min);
        String strHours = (hours < 10) ? "0" + hours : Integer.toString(hours);

        System.out.println("------------------->----------------------------------- ");
        System.out.println("HH:MM:SS --->>>> " + strHours + ":" + strmin + ":" + strSec);
        System.out.println("------------------->----------------------------------- ");
        return LocalTime.parse(strHours + ":" + strmin + ":" + strSec);
    }

    public LocalTime getDateTimeDiffInTime(LocalDateTime fromDate, LocalDateTime toDate) throws ParseException {
        System.out.println("fromDate " + fromDate);
        System.out.println("toDate " + toDate);

        System.out.println("fromDate.getSecond() " + fromDate.getSecond());
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

        Date d1 = fromDate.getSecond() > 0 ? df1.parse(fromDate.toString()) : df2.parse(fromDate.toString());
        Date d2 = toDate.getSecond() > 0 ? df1.parse(toDate.toString()) : df2.parse(toDate.toString());

        long d = Math.abs(d2.getTime() - d1.getTime());
        Duration duration = Duration.ofMillis(d);
        long seconds1 = duration.getSeconds();
        long HH = (seconds1 / 3600);
        long MM = ((seconds1 % 3600) / 60);
        long SS = (seconds1 % 60);
        String timeInHHMMSS = String.format("%02d:%02d:%02d", HH, MM, SS);
        System.out.println("String.format(\"%02d:%02d:%02d\", HH, MM, SS) " + String.format("%02d:%02d:%02d", HH, MM, SS));

        String totalTime = null;
        if (HH > 23) {
            totalTime = "16:00:00";
        } else {
            totalTime = timeInHHMMSS;
        }
        return LocalTime.parse(totalTime);
    }

    public String getKeyName(String str, boolean isId) {
        str = str.replace(" ","_");
        if(isId)
            str = str+"_"+"id";
//        System.out.println(str);
        return str;
    }
}
