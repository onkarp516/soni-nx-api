package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.truethic.soninx.SoniNxAPI.repository.*;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.OperationParameterDTO;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OperationParameterService {
    @Autowired
    OperationParameterRepository operationParameterRepository;
    @Autowired
    JobRepository jobRepository;
    @Autowired
    JobOperationRepository jobOperationRepository;
    @Autowired
    ActionRepository actionRepository;
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private InstrumentRepository instrumentRepository;
    @Autowired
    private ControlMethodRepository methodRepository;
    @Autowired
    private CheckingFrequencyRepository frequencyRepository;

    public Object createOperationParameter(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

        try {
            String dsrows = request.getParameter("rows");
            JsonArray jsonArray = new JsonParser().parse(dsrows).getAsJsonArray();

            List<OperationParameter> operationParameterList = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject object = jsonArray.get(i).getAsJsonObject();
                if (object.get("specification").getAsString() != "" && object.get("specification").getAsString() != null) {
                    OperationParameter operationParameter = new OperationParameter();
                    Job job = jobRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobId")), true);
                    if (job != null) {
                        operationParameter.setJob(job);
//                        job.getOperationParameters().add(operationParameter);
                    }
                    JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobOperationId")), true);
                    if (jobOperation != null) {
                        operationParameter.setJobOperation(jobOperation);
//                        jobOperation.getOperationParameters().add(operationParameter);
                    }

                    operationParameter.setSpecification(object.get("specification").getAsString());
                    operationParameter.setFirstParameter(object.get("firstParameter").getAsString());
                    operationParameter.setSecondParameter(object.get("secondParameter").getAsString());

                    Instrument instrument = instrumentRepository.findByIdAndStatus(object.get("instrumentUsed").getAsLong(), true);
                    if (instrument != null)
                        operationParameter.setInstrument(instrument);

                    CheckingFrequency checkingFrequency = frequencyRepository.findByIdAndStatus(object.get("checkingFrequency").getAsLong(), true);
                    if (checkingFrequency != null)
                        operationParameter.setCheckingFrequency(checkingFrequency);

                    ControlMethod controlMethod = methodRepository.findByIdAndStatus(object.get("controlMethod").getAsLong(), true);
                    if (controlMethod != null)
                        operationParameter.setControlMethod(controlMethod);

                    operationParameter.setStatus(true);
                    operationParameter.setCreatedBy(user.getId());
                    operationParameter.setInstitute(user.getInstitute());
                    operationParameterList.add(operationParameter);
                }
            }
            try {
                operationParameterRepository.saveAll(operationParameterList);
                responseObject.setResponseStatus(HttpStatus.OK.value());
                responseObject.setMessage("Line Inspection Successfully");
            } catch (Exception e) {
                System.out.println("Exception " + e.getMessage());
                e.printStackTrace();
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseObject.setMessage("Internal Server Error");
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Internal Server Error");
        }

        /*Action action = actionRepository.findByIdAndStatus(Long.parseLong(request.getParameter("actionId")), true);
        if (action != null) {
            operationParameter.setAction(action);
            action.getOperationParameters().add(operationParameter);
        }*/
        /*try {
            operationParameterRepository.save(operationParameter);
            responseObject.setMessage("Operation Parameter added successfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Internal Server Error");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }*/
        return responseObject;
    }

    public Object DTOperationParameter(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        String selectedJobName = request.get("selectedJobName");
        String selectedOperationName = request.get("selectedOperationName");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<OperationParameter> operationParameterList = new ArrayList<>();
        List<OperationParameterDTO> parameterDTOList = new ArrayList<>();
        try {
            String query = "SELECT operation_parameter_tbl.*, job_tbl.job_name as job_name, job_operation_tbl.operation_name as operation_name," +
                    " instrument_tbl.name as instrument_used, checking_frequency_tbl.checking_frequency_label AS checking_frequency," +
                    " control_method_tbl.control_method_label AS control_method FROM `operation_parameter_tbl` LEFT" +
                    " JOIN job_tbl ON operation_parameter_tbl.job_id=job_tbl.id LEFT JOIN job_operation_tbl ON" +
                    " operation_parameter_tbl.job_operation_id=job_operation_tbl.id left join instrument_tbl ON" +
                    " operation_parameter_tbl.instrument_id=instrument_tbl.id LEFT JOIN checking_frequency_tbl ON" +
                    " operation_parameter_tbl.checking_frequency_id=checking_frequency_tbl.id LEFT JOIN control_method_tbl ON" +
                    " operation_parameter_tbl.control_method_id=control_method_tbl.id WHERE operation_parameter_tbl.status=1 AND operation_parameter_tbl.institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (job_name LIKE '%" + searchText + "%' OR operation_name LIKE '%" + searchText +
                        "%' OR first_parameter LIKE '%" + searchText + "%' OR second_parameter LIKE '%" + searchText +
                        "%' OR specification LIKE '%" + searchText + "%' OR instrument_tbl.name LIKE '%" + searchText +
                        "%' OR checking_frequency_tbl.checking_frequency_label LIKE '%" + searchText +
                        "%' OR control_method_tbl.control_method_label LIKE '%" + searchText + "%')";
            }

            if (!selectedJobName.equalsIgnoreCase("")) {
                query = query + " AND job_name= '" + selectedJobName + "' ";
            }

            if (!selectedOperationName.equalsIgnoreCase("")) {
                query = query + " AND operation_name= '" + selectedOperationName + "' ";
            }

            String jsonToStr = request.get("sort");
            JsonObject jsonObject = new Gson().fromJson(jsonToStr, JsonObject.class);
            if (!jsonObject.get("colId").toString().equalsIgnoreCase("null") &&
                    jsonObject.get("colId").toString() != null) {
                System.out.println(" ORDER BY " + jsonObject.get("colId").toString());
                String sortBy = jsonObject.get("colId").toString();
                query = query + " ORDER BY " + sortBy;
                if (jsonObject.get("isAsc").getAsBoolean()) {
                    query = query + " ASC";
                } else {
                    query = query + " DESC";
                }
            } else {
                query = query + " ORDER BY operation_parameter_tbl.id DESC";
            }
            String query1 = query;
            Integer endLimit = to - from;
            query = query + " LIMIT " + from + ", " + endLimit;
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, OperationParameter.class);
            Query q1 = entityManager.createNativeQuery(query1, OperationParameter.class);

            operationParameterList = q.getResultList();
            System.out.println("Limit total rows " + operationParameterList.size());

            List<OperationParameter> operationParameterArrayList = new ArrayList<>();
            operationParameterArrayList = q1.getResultList();
            System.out.println("total rows " + operationParameterArrayList.size());

            if (operationParameterList.size() > 0) {
                for (OperationParameter operationParameter : operationParameterList) {
                    parameterDTOList.add(convertToOperationParameterDTO(operationParameter));
                }
            }

            genericDTData.setRows(parameterDTOList);
            genericDTData.setTotalRows(operationParameterArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            genericDTData.setRows(parameterDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private OperationParameterDTO convertToOperationParameterDTO(OperationParameter operationParameter) {
        OperationParameterDTO operationParameterDTO = new OperationParameterDTO();
        operationParameterDTO.setOperationParameterId(operationParameter.getId());
        if (operationParameter.getJob() != null) {
            operationParameterDTO.setJobId(operationParameter.getJob().getId());
            operationParameterDTO.setJobName(operationParameter.getJob().getJobName());
        }
        if (operationParameter.getJobOperation() != null) {
            operationParameterDTO.setJobOperationId(operationParameter.getJobOperation().getId());
            operationParameterDTO.setJobOperationName(operationParameter.getJobOperation().getOperationName());
        }
        /*if (operationParameter.getAction() != null) {
            operationParameterDTO.setActionId(operationParameter.getAction().getId());
            operationParameterDTO.setActionName(operationParameter.getAction().getActionName());
        }*/
        operationParameterDTO.setSpecification(operationParameter.getSpecification());
        operationParameterDTO.setFirstParameter(operationParameter.getFirstParameter());
        operationParameterDTO.setSecondParameter(operationParameter.getSecondParameter());
        operationParameterDTO.setInstrumentUsedId(operationParameter.getInstrument() != null ? operationParameter.getInstrument().getId() : 0);
        operationParameterDTO.setInstrumentUsed(operationParameter.getInstrument() != null ? operationParameter.getInstrument().getName() : "");
        operationParameterDTO.setCheckingFrequencyId(operationParameter.getCheckingFrequency() != null ? operationParameter.getCheckingFrequency().getId() : 0);
        operationParameterDTO.setCheckingFrequency(operationParameter.getCheckingFrequency() != null ? operationParameter.getCheckingFrequency().getCheckingFrequencyLabel() : "");
        operationParameterDTO.setControlMethodId(operationParameter.getControlMethod() != null ? operationParameter.getControlMethod().getId() : 0);
        operationParameterDTO.setControlMethod(operationParameter.getControlMethod() != null ? operationParameter.getControlMethod().getControlMethodLabel() : "");
        operationParameterDTO.setStatus(operationParameter.getStatus());
        operationParameterDTO.setCreatedAt(operationParameter.getCreatedAt() != null ?
                operationParameter.getCreatedAt().toString() : "");
        operationParameterDTO.setCreatedBy(operationParameter.getCreatedBy());
        operationParameterDTO.setUpdatedBy(operationParameter.getUpdatedBy());
        operationParameterDTO.setUpdatedAt(operationParameter.getUpdatedAt() != null ?
                operationParameter.getUpdatedAt().toString() : "");
        return operationParameterDTO;
    }

    public Object findOperationParameter(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            OperationParameter operationParameter =
                    operationParameterRepository.findByIdAndStatus(Long.parseLong(request.get("id")), true);
            if (operationParameter != null) {
                OperationParameterDTO operationParameterDTO = new OperationParameterDTO();
                operationParameterDTO.setOperationParameterId(operationParameter.getId());
                operationParameterDTO.setSpecification(operationParameter.getSpecification());
                operationParameterDTO.setFirstParameter(operationParameter.getFirstParameter());
                operationParameterDTO.setSecondParameter(operationParameter.getSecondParameter());
                operationParameterDTO.setInstrumentUsedId(operationParameter.getInstrument() != null ? operationParameter.getInstrument().getId() : 0);
                operationParameterDTO.setInstrumentUsed(operationParameter.getInstrument() != null ? operationParameter.getInstrument().getName() : "");
                operationParameterDTO.setCheckingFrequencyId(operationParameter.getCheckingFrequency() != null ? operationParameter.getCheckingFrequency().getId() : 0);
                operationParameterDTO.setCheckingFrequency(operationParameter.getCheckingFrequency() != null ? operationParameter.getCheckingFrequency().getCheckingFrequencyLabel() : "");
                operationParameterDTO.setControlMethodId(operationParameter.getControlMethod() != null ? operationParameter.getControlMethod().getId() : 0);
                operationParameterDTO.setControlMethod(operationParameter.getControlMethod() != null ? operationParameter.getControlMethod().getControlMethodLabel() : "");
//                operationParameterDTO.setActionId(operationParameter.getAction().getId());
                operationParameterDTO.setJobId(operationParameter.getJob().getId());
                operationParameterDTO.setJobOperationId(operationParameter.getJobOperation().getId());
                responseMessage.setResponse(operationParameterDTO);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object updateOperationParameter(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            OperationParameter operationParameter = operationParameterRepository.findByIdAndStatus(
                    Long.parseLong(request.getParameter("id")), true);
            if (operationParameter != null) {
                Job job = jobRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobId")), true);
                if (job != null) {
                    if (operationParameter.getJob() == job) {
                        System.out.println("same object");
                        operationParameter.setJob(job);
                    } else {
                        Job job1 = operationParameter.getJob();
                        job1.getOperationParameters().remove(operationParameter);

                        operationParameterRepository.save(operationParameter);

                        operationParameter.setJob(job);
                        job.getOperationParameters().add(operationParameter);
                    }
                } else {
                    responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                    responseObject.setMessage("Job not found");
                    return responseObject;
                }

                JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobOperationId")), true);
                if (jobOperation != null) {
                    if (operationParameter.getJobOperation() == jobOperation) {
                        System.out.println("same object");
                        operationParameter.setJobOperation(jobOperation);
                    } else {
                        JobOperation jobOperation1 = operationParameter.getJobOperation();
                        jobOperation1.getOperationParameters().remove(operationParameter);

                        operationParameterRepository.save(operationParameter);

                        operationParameter.setJobOperation(jobOperation);
                        jobOperation.getOperationParameters().add(operationParameter);
                    }
                } else {
                    responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                    responseObject.setMessage("Job Operation not found");
                    return responseObject;
                }

                /*Action action = actionRepository.findByIdAndStatus(Long.parseLong(request.getParameter("actionId")), true);
                if (action != null) {
                    if (operationParameter.getAction() == action) {
                        System.out.println("same object");
                        operationParameter.setAction(action);
                    } else {
                        Action action1 = operationParameter.getAction();
                        action1.getOperationParameters().remove(operationParameter);

                        operationParameterRepository.save(operationParameter);

                        operationParameter.setAction(action);
                        action.getOperationParameters().add(operationParameter);
                    }
                } else {
                    responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                    responseObject.setMessage("Action not found");
                    return responseObject;
                }*/

                operationParameter.setSpecification(request.getParameter("specification"));
                operationParameter.setFirstParameter(request.getParameter("firstParameter"));
                operationParameter.setSecondParameter(request.getParameter("secondParameter"));


                Instrument instrument = instrumentRepository.findByIdAndStatus(Long.parseLong(request.getParameter("instrumentUsed")), true);
                if (instrument != null)
                    operationParameter.setInstrument(instrument);

                CheckingFrequency checkingFrequency = frequencyRepository.findByIdAndStatus(Long.parseLong(request.getParameter("checkingFrequency")), true);
                if (checkingFrequency != null)
                    operationParameter.setCheckingFrequency(checkingFrequency);

                ControlMethod controlMethod = methodRepository.findByIdAndStatus(Long.parseLong(request.getParameter("controlMethod")), true);
                if (controlMethod != null)
                    operationParameter.setControlMethod(controlMethod);

                operationParameter.setStatus(true);
                Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                operationParameter.setUpdatedBy(user.getId());
                operationParameter.setInstitute(user.getInstitute());
                operationParameter.setUpdatedAt(LocalDateTime.now());
                try {
                    operationParameterRepository.save(operationParameter);
                    responseObject.setMessage("Operation Parameter updated successfully");
                    responseObject.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseObject.setMessage("Internal Server Error");
                    e.printStackTrace();
                    System.out.println("Exception:" + e.getMessage());
                }
            } else {
                responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                responseObject.setMessage("Data not found");
            }
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Internal Server Error");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }

        return responseObject;
    }

    public Object deleteOperationParameter(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            OperationParameter operationParameter = operationParameterRepository.findByIdAndStatus(
                    Long.parseLong(request.getParameter("id")), true);
            if (operationParameter != null) {
                operationParameter.setStatus(false);
                Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                operationParameter.setUpdatedBy(user.getId());
                operationParameter.setInstitute(user.getInstitute());
                operationParameter.setUpdatedAt(LocalDateTime.now());

                try {
                    operationParameterRepository.save(operationParameter);
                    responseObject.setMessage("Operation Parameter deleted successfully");
                    responseObject.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseObject.setMessage("Internal Server Error");
                    e.printStackTrace();
                    System.out.println("Exception:" + e.getMessage());
                }
            } else {
                responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                responseObject.setMessage("Data not found");
            }
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Internal Server Error");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }

        return responseObject;
    }

    public JsonObject getDrawingSizes(Map<String, String> request) {
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            List<OperationParameter> operationParameterList = operationParameterRepository.findByJobOperationIdAndStatus(Long.valueOf(request.get("jobOperationId")), true);
            for (OperationParameter operationParameter : operationParameterList) {
                JsonObject dObject = new JsonObject();
                // System.out.println("operation parameter : "+operationParameter);
                dObject.addProperty("id", operationParameter.getId());
                dObject.addProperty("drawingSize", operationParameter.getFirstParameter() + "/" + operationParameter.getSecondParameter());
                dObject.addProperty("min", operationParameter.getFirstParameter());
                dObject.addProperty("max", operationParameter.getSecondParameter());
                dObject.addProperty("specification", operationParameter.getSpecification());
                dObject.addProperty("instrumentUsed", operationParameter.getInstrument() != null ?
                        operationParameter.getInstrument().getName() : "");
                if (operationParameter.getInstrument() != null)
                    dObject.addProperty("isReadingApplicable", operationParameter.getInstrument().getIsReadingApplicable() != null
                            ? operationParameter.getInstrument().getIsReadingApplicable() == true ? true : false : true);
                else
                    dObject.addProperty("isReadingApplicable", false);
                dObject.addProperty("checkingFrequency", operationParameter.getCheckingFrequency() != null ?
                        operationParameter.getCheckingFrequency().getCheckingFrequencyLabel() : "");
                dObject.addProperty("controlMethod", operationParameter.getControlMethod() != null ?
                        operationParameter.getControlMethod().getControlMethodLabel() : "");
                jsonArray.add(dObject);
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.addProperty("message", "Internal Server Error");
        }
        return response;
    }
}
