package com.truethic.soninx.SoniNxAPI.service;

import com.truethic.soninx.SoniNxAPI.repository.*;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericData;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class OperationProcedureService {
    @Autowired
    OperationProcedureRepository operationProcedureRepository;

    @Autowired
    JobOperationRepository jobOperationRepository;

    @Autowired
    MachineRepository machineRepository;

    @Autowired
    OperationParameterRepository operationParameterRepository;

    @Autowired
    ToolMgmtRepository toolMgmtRepository;

    @Autowired
    JwtTokenUtil jwtTokenUtil;

    public Object createOperationProcedure(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        OperationProcedure operationProcedure = new OperationProcedure();

        JobOperation jobOperation =
                jobOperationRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobOperationId")), true);
        operationProcedure.setJobOperation(jobOperation);
        Machine machine = machineRepository.findByIdAndStatus(Long.parseLong(request.getParameter("machineId")), true);
        operationProcedure.setMachine(machine);
        OperationParameter operationParameter = operationParameterRepository.findByIdAndStatus(Long.parseLong(request.getParameter("parameterId")), true);
        operationProcedure.setOperationParameter(operationParameter);
        ToolManagement toolManagement = toolMgmtRepository.findByIdAndStatus(Long.parseLong(request.getParameter("toolId")), true);
        operationProcedure.setToolManagement(toolManagement);
        operationProcedure.setPrevJobOperation(Long.parseLong(request.getParameter("prevJobOperation")));
        operationProcedure.setNextJobOperation(Long.parseLong(request.getParameter("nextJobOperation")));
        operationProcedure.setCustDrgNo(Long.parseLong(request.getParameter("custDrgNo")));
        operationProcedure.setPartName(request.getParameter("partName"));
        operationProcedure.setPartNumber(Long.parseLong(request.getParameter("partNumber")));
        operationProcedure.setRevNo(Long.parseLong(request.getParameter("revNo")));
        operationProcedure.setChangeLevelDate(LocalDate.parse(request.getParameter("changeLevelDate")));
        operationProcedure.setOperationProcedure(request.getParameter("operationProcedure"));
        operationProcedure.setStatus(true);
        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        operationProcedure.setCreatedBy(user.getId());
        operationProcedure.setInstitute(user.getInstitute());
        operationProcedure.setCreatedAt(LocalDateTime.now());
        try {
            operationProcedureRepository.save(operationProcedure);
            responseObject.setMessage("Procedure created successfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Failed to create job");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }


    public Object DTOperationProcedure(Integer pageNo, Integer pageSize) {
        ResponseMessage responseMessage = new ResponseMessage();
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        try {
            Page<OperationProcedure> operationProceduresPage = operationProcedureRepository.findByStatusOrderByIdDesc(pageable, true);
            List<OperationProcedure> operationProceduresList = operationProceduresPage.toList();
            GenericData<OperationProcedure> data = new GenericData<>(operationProceduresList, operationProceduresPage.getTotalElements(), pageNo, pageSize,
                    operationProceduresPage.getTotalPages());
            responseMessage.setResponse(data);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.setMessage("Exception occurred");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }


    public Object findOperationProcedure(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        OperationProcedure operationProcedure = operationProcedureRepository.findByIdAndStatus(Long.parseLong(request.get("id")), true);
        if (operationProcedure != null) {
            responseMessage.setResponse(operationProcedure);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } else {
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }


    public Object updateOperationProcedure(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        OperationProcedure operationProcedure = operationProcedureRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
        if (operationProcedure != null) {
            operationProcedure.setCustDrgNo(Long.parseLong(requestParam.get("custDrgNo")));
            operationProcedure.setPartName(requestParam.get("partName"));
            operationProcedure.setPartNumber(Long.parseLong(requestParam.get("partNumber")));
            operationProcedure.setRevNo(Long.parseLong(requestParam.get("revNo")));
            operationProcedure.setChangeLevelDate(LocalDate.parse(requestParam.get("changeLevelDate")));
            operationProcedure.setOperationProcedure(requestParam.get("operationProcedure"));
            JobOperation jobOperation =
                    jobOperationRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobOperationId")), true);
            operationProcedure.setJobOperation(jobOperation);
            Machine machine = machineRepository.findByIdAndStatus(Long.parseLong(request.getParameter("machineId")), true);
            operationProcedure.setMachine(machine);
            OperationParameter operationParameter = operationParameterRepository.findByIdAndStatus(Long.parseLong(request.getParameter("parameterId")), true);
            operationProcedure.setOperationParameter(operationParameter);
            ToolManagement toolManagement = toolMgmtRepository.findByIdAndStatus(Long.parseLong(request.getParameter("toolId")), true);
            operationProcedure.setToolManagement(toolManagement);
            operationProcedure.setStatus(true);
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            operationProcedure.setUpdatedBy(user.getId());
            operationProcedure.setInstitute(user.getInstitute());
            operationProcedure.setUpdatedAt(LocalDateTime.now());
            try {
                operationProcedureRepository.save(operationProcedure);
                responseObject.setMessage("Procedure updated successfully");
                responseObject.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseObject.setMessage("Failed to update job");
                e.printStackTrace();
                System.out.println("Exception:" + e.getMessage());
            }
        } else {
            responseObject.setMessage("Data not found");
            responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseObject;
    }


    public Object deleteOperationProcedure(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            OperationProcedure operationProcedure = operationProcedureRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")),
                    true);
            if (operationProcedure != null) {
                operationProcedure.setStatus(false);
                Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                operationProcedure.setCreatedBy(user.getId());
                operationProcedure.setInstitute(user.getInstitute());
                try {
                    operationProcedureRepository.save(operationProcedure);
                    responseObject.setMessage("Procedure deleted successfully");
                    responseObject.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseObject.setMessage("Internal Server Error");
                    e.printStackTrace();
                    System.out.println("Exception:" + e.getMessage());
                }
            }
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Failed to add job operation");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }


}
