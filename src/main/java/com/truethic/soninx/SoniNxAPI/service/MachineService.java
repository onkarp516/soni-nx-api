package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.MachineRepository;
import com.truethic.soninx.SoniNxAPI.repository.TaskViewRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.MachineDTDTO;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.model.Machine;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class MachineService {
    private static final Logger machineLogger = LoggerFactory.getLogger(MachineService.class);
    @Autowired
    MachineRepository machineRepository;
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private TaskViewRepository taskViewRepository;
    @Autowired
    private TaskService taskService;

    public Object createMachine(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        Machine machine = new Machine();
        machine.setName(request.getParameter("name"));
        machine.setNumber(request.getParameter("number"));
        machine.setDateOfPurchase(LocalDate.parse(request.getParameter("dateOfPurchase")));
        machine.setCost(Integer.parseInt(request.getParameter("cost")));
        machine.setWhatMachineMakes(request.getParameter("whatMachineMakes"));
        machine.setIsMachineCount(Boolean.valueOf(request.getParameter("isMachineCount")));
        if (Boolean.valueOf(request.getParameter("isMachineCount"))) {
            machine.setCurrentMachineCount(Long.valueOf(0));
        }
        machine.setCurrentMachineCount(request.getParameter("currentMachineCount") != null ?
                Long.valueOf(request.getParameter("currentMachineCount")) : Long.valueOf(0));
        machine.setDefaultMachineCount(request.getParameter("defaultMachineCount") != null ?
                Long.valueOf(request.getParameter("defaultMachineCount")) : Long.valueOf(0));
        machine.setStatus(true);
        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        machine.setCreatedBy(user.getId());
        machine.setInstitute(user.getInstitute());
        machine.setCreatedAt(LocalDateTime.now());
        try {
            machineRepository.save(machine);
            responseObject.setMessage("Machine saved successfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Failed to save machine");
        }
        return responseObject;
    }

    public Object getMachine() {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            List<Machine> machineList = machineRepository.findAllByStatus(true);
            responseMessage.setResponse(machineList);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.setMessage("Exception occurred");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public Object DTMachine(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Machine> machineList = new ArrayList<>();
        List<MachineDTDTO> machineDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `machine_tbl` WHERE status=1 AND institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (what_machine_makes LIKE '%" + searchText + "%' OR current_machine_count LIKE '%"
                        + searchText + "%' OR name LIKE '%" + searchText + "%' OR date_of_purchase LIKE '%" +
                        searchText + "%'  OR number LIKE '%" + searchText + "%')";
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
                query = query + " ORDER BY id DESC";
            }
            String query1 = query;
            Integer endLimit = to - from;
            query = query + " LIMIT " + from + ", " + endLimit;
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, Machine.class);
            Query q1 = entityManager.createNativeQuery(query1, Machine.class);

            machineList = q.getResultList();
            System.out.println("Limit total rows " + machineList.size());

            for (Machine machine : machineList) {
                machineDTDTOList.add(convertToDTDTO(machine));
            }
            List<Machine> machineArrayList = new ArrayList<>();
            machineArrayList = q1.getResultList();
            System.out.println("total rows " + machineArrayList.size());

            genericDTData.setRows(machineDTDTOList);
            genericDTData.setTotalRows(machineArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(machineDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private MachineDTDTO convertToDTDTO(Machine machine) {
        MachineDTDTO machineDTDTO = new MachineDTDTO();
        machineDTDTO.setId(machine.getId());
        machineDTDTO.setName(machine.getName());
        machineDTDTO.setNumber(machine.getNumber());
        machineDTDTO.setDateOfPurchase(machine.getDateOfPurchase().toString());
        machineDTDTO.setCost(machine.getCost());
        machineDTDTO.setWhatMachineMakes(machine.getWhatMachineMakes());
        machineDTDTO.setIsMachineCount(machine.getIsMachineCount());
        machineDTDTO.setCurrentMachineCount(machine.getCurrentMachineCount());
        machineDTDTO.setDefaultMachineCount(machine.getDefaultMachineCount());
        machineDTDTO.setCreatedAt(String.valueOf(machine.getCreatedAt()));
        if (machine.getUpdatedAt() != null)
            machineDTDTO.setUpdatedAt(String.valueOf(machine.getUpdatedAt()));
        return machineDTDTO;
    }

    public Object deleteMachine(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Machine machine = machineRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")),
                true);
        if (machine != null) {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            machine.setStatus(false);
            machine.setUpdatedAt(LocalDateTime.now());
            machine.setUpdatedBy(users.getId());
            machine.setInstitute(users.getInstitute());
            try {
                machineRepository.save(machine);
                responseMessage.setMessage("Machine deleted successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                System.out.println("Exception " + e.getMessage());
                e.printStackTrace();
                responseMessage.setMessage("Failed to delete machine");
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } else {
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }

    public Object findMachine(Map<String, String> requestParam) {
        ResponseMessage responseMessage = new ResponseMessage();
        Machine machine = machineRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
        if (machine != null) {
            MachineDTDTO machineDTDTO = convertToDTDTO(machine);
            responseMessage.setResponse(machineDTDTO);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } else {
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }

    public Object updateMachine(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        Machine machine = machineRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
        try {
            if (machine != null) {
                machine.setName(requestParam.get("name"));
                machine.setNumber(requestParam.get("number"));

                LocalDate localDate = LocalDate.parse(requestParam.get("dateOfPurchase"));
                machine.setDateOfPurchase(localDate);
                machine.setCost(Integer.parseInt(requestParam.get("cost")));
                machine.setWhatMachineMakes(requestParam.get("whatMachineMakes"));
                machine.setIsMachineCount(Boolean.valueOf(requestParam.get("isMachineCount")));
                if (Boolean.valueOf(requestParam.get("isMachineCount"))) {
                    machine.setCurrentMachineCount(Long.valueOf(0));
                }
                machine.setCurrentMachineCount(requestParam.get("currentMachineCount") != null ?
                        Long.valueOf(requestParam.get("currentMachineCount")) : Long.valueOf(0));
                machine.setDefaultMachineCount(requestParam.get("defaultMachineCount") != null ?
                        Long.valueOf(requestParam.get("defaultMachineCount")) : Long.valueOf(0));
                machine.setStatus(true);
                Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                machine.setUpdatedBy(user.getId());
                machine.setUpdatedAt(LocalDateTime.now());
                machine.setInstitute(user.getInstitute());
                try {
                    machineRepository.save(machine);
                    responseObject.setMessage("Machine updated successfully");
                    responseObject.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception:" + e.getMessage());
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseObject.setMessage("Failed to update machine");
                }
            } else {
                responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                responseObject.setMessage("Data not found");
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Failed to update machine");
        }
        return responseObject;
    }


    /*mobile app url start*/

    public JsonObject listForSelection(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            List<Machine> machineList = machineRepository.findAllByInstituteIdAndStatus(employee.getInstitute().getId(),true);
            JsonArray jsonArray = new JsonArray();
            for (Machine machine : machineList) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id", machine.getId());
                jsonObject.addProperty("name", machine.getName());
                jsonObject.addProperty("machineNo", machine.getNumber());
                jsonObject.addProperty("isMachineCount", machine.getIsMachineCount());
                jsonObject.addProperty("currentMachineCount", machine.getCurrentMachineCount());
                jsonArray.add(jsonObject);
            }
            responseMessage.add("response", jsonArray);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.addProperty("message", "Exception occurred");
            responseMessage.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public JsonObject machineListForSelection(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            List<Machine> machineList = machineRepository.findAllByInstituteIdAndStatus(users.getInstitute().getId(),true);
            JsonArray jsonArray = new JsonArray();
            for (Machine machine : machineList) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id", machine.getId());
                jsonObject.addProperty("name", machine.getName());
                jsonObject.addProperty("machineNo", machine.getNumber());
                jsonObject.addProperty("isMachineCount", machine.getIsMachineCount());
                jsonObject.addProperty("currentMachineCount", machine.getCurrentMachineCount());
                jsonArray.add(jsonObject);
            }
            responseMessage.add("response", jsonArray);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.addProperty("message", "Exception occurred");
            responseMessage.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public JsonObject getMachineReport(Map<String, String> request) {
        JsonObject response = new JsonObject();
        JsonArray machineArray = new JsonArray();
        try {

            String fromDate = request.get("fromDate");
            String toDate = request.get("toDate");
            String machineId = request.get("machineId");

            List<Object[]> machineList = new ArrayList<>();

            if (!machineId.equalsIgnoreCase(""))
                machineList = machineRepository.getMachineReportByMachine(fromDate, toDate, machineId, true);
            else machineList = machineRepository.getMachineReport(fromDate, toDate, true);

            for (int i = 0; i < machineList.size(); i++) {
                Object[] obj = machineList.get(i);

                JsonObject machineObj = new JsonObject();
                machineObj.addProperty("machineId", obj[0].toString());
                machineObj.addProperty("machineName", obj[1].toString());
                machineObj.addProperty("machineNumber", obj[5].toString());
                machineObj.addProperty("itemId", obj[2].toString());
                machineObj.addProperty("itemName", obj[3].toString());
                machineObj.addProperty("productionQty", obj[4].toString());

                machineId = obj[0].toString();
                Long jobId = Long.valueOf(obj[2].toString());
                System.out.println("machineId " + machineId);
                System.out.println("jobId " + jobId);
                List<Object[]> taskViewList = taskViewRepository.findMachineViewData(
                        Long.valueOf(machineId), jobId, LocalDate.parse(fromDate), LocalDate.parse(toDate), true);

                System.out.println("taskViewList " + taskViewList.size());
                JsonArray taskDTOList = new JsonArray();
                for (int j=0; j < taskViewList.size(); j++) {
//                    taskDTOList.add(taskService.convertToJSonObj(taskView));
                    Object[] object = taskViewList.get(j);

                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("employeeId", object[0].toString());
                    jsonObject.addProperty("employeeName", object[1].toString());
                    jsonObject.addProperty("cycleTime", object[2].toString());
                    jsonObject.addProperty("totalTime", object[3].toString());
                    jsonObject.addProperty("actualWorkTime", object[4].toString());
                    jsonObject.addProperty("totalCount", object[5].toString());
                    jsonObject.addProperty("requiredProduction", object[6].toString());
                    jsonObject.addProperty("actualProduction", object[7].toString());
                    jsonObject.addProperty("operationName", object[8].toString());
//                    jsonObject.addProperty("remark", object[9] != null ? object[9].toString(): "");
                    taskDTOList.add(jsonObject);
                }

                machineObj.add("taskList", taskDTOList);
                machineArray.add(machineObj);
            }

            response.add("response", machineArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            machineLogger.error("getMachineReport exception " + e);
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;

    }


    /*mobile app url end*/
}
