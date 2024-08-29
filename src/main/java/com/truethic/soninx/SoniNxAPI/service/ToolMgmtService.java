package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.ActionRepository;
import com.truethic.soninx.SoniNxAPI.repository.ToolMgmtRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.ToolManagementDTO;
import com.truethic.soninx.SoniNxAPI.model.Action;
import com.truethic.soninx.SoniNxAPI.model.ToolManagement;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ToolMgmtService {
    @Autowired
    ToolMgmtRepository toolMgmtRepository;
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private ActionRepository actionRepository;

    public Object createToolManagement(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        ToolManagement toolManagement = new ToolManagement();

        Action action = actionRepository.findByIdAndStatus(Long.parseLong(request.getParameter("actionId")), true);
        if (action != null) {
            toolManagement.setAction(action);
            action.getToolManagements().add(toolManagement);
        }
        toolManagement.setBlock(request.getParameter("block"));
        toolManagement.setOffsetNo(request.getParameter("offsetNo"));
        toolManagement.setToolHolders(request.getParameter("toolHolders"));
        toolManagement.setInserts(request.getParameter("inserts"));
        toolManagement.setFrequency(request.getParameter("frequency"));
        toolManagement.setUsedFor(request.getParameter("usedFor"));
        toolManagement.setStatus(true);
        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        toolManagement.setCreatedBy(user.getId());
        toolManagement.setInstitute(user.getInstitute());
        try {
            toolMgmtRepository.save(toolManagement);
            responseObject.setMessage("Tool added successfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Internal Server Error");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }


    public Object DTToolMgmt(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<ToolManagement> toolManagementList = new ArrayList<>();
        List<ToolManagementDTO> toolManagementDTOList = new ArrayList<>();
        try {
            String query = "SELECT tool_mgmt_tbl.*, action.action_name as action_name FROM `tool_mgmt_tbl` LEFT JOIN" +
                    " action ON tool_mgmt_tbl.action_id=action.id WHERE tool_mgmt_tbl.status=1 AND tool_mgmt_tbl.institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (block LIKE '%" + searchText + "%' OR frequency LIKE '%" + searchText + "%' OR" +
                        " inserts LIKE '%" + searchText + "%' OR offset_no LIKE '%" + searchText + "%' OR tool_holders" +
                        " LIKE '%" + searchText + "%' OR used_for LIKE '%" + searchText + "%')";
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

            Query q = entityManager.createNativeQuery(query, ToolManagement.class);
            Query q1 = entityManager.createNativeQuery(query1, ToolManagement.class);

            toolManagementList = q.getResultList();
            System.out.println("Limit total rows " + toolManagementList.size());

            if (toolManagementList.size() > 0) {
                for (ToolManagement toolManagement : toolManagementList) {
                    toolManagementDTOList.add(convertToToolManagementDTO(toolManagement));
                }
            }

            List<ToolManagement> toolManagementArrayList = new ArrayList<>();
            toolManagementArrayList = q1.getResultList();
            System.out.println("total rows " + toolManagementArrayList.size());

            genericDTData.setRows(toolManagementDTOList);
            genericDTData.setTotalRows(toolManagementArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(toolManagementDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }


    public Object findToolMgmt(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        ToolManagement toolManagement = toolMgmtRepository.findByIdAndStatus(Long.parseLong(request.get("id")), true);
        if (toolManagement != null) {
            ToolManagementDTO toolManagementDTO = convertToToolManagementDTO(toolManagement);
            responseMessage.setResponse(toolManagementDTO);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } else {
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }


    public Object updateToolMgmt(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        ToolManagement toolManagement = toolMgmtRepository.findByIdAndStatus(Long.parseLong(request.getParameter("id")),
                true);
        if (toolManagement != null) {
            Action action = actionRepository.findByIdAndStatus(Long.parseLong(request.getParameter("actionId")), true);
            if (action != null) {
                if (toolManagement.getAction() == action) {
                    System.out.println("same object");
                    toolManagement.setAction(action);
                } else {
                    Action action1 = toolManagement.getAction();
                    action1.getToolManagements().remove(toolManagement);

                    toolManagement.setAction(action);
                    action.getToolManagements().add(toolManagement);
                }
            } else {
                responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                responseObject.setMessage("Action not found");
                return responseObject;
            }
            toolManagement.setBlock(request.getParameter("block"));
            toolManagement.setOffsetNo(request.getParameter("offsetNo"));
            toolManagement.setToolHolders(request.getParameter("toolHolders"));
            toolManagement.setInserts(request.getParameter("inserts"));
            toolManagement.setFrequency(request.getParameter("frequency"));
            toolManagement.setUsedFor(request.getParameter("usedFor"));
            toolManagement.setStatus(true);
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            toolManagement.setUpdatedBy(user.getId());
            toolManagement.setInstitute(user.getInstitute());
            toolManagement.setUpdatedAt(LocalDateTime.now());
            try {
                toolMgmtRepository.save(toolManagement);
                responseObject.setMessage("Tool updated successfully");
                responseObject.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseObject.setMessage("Failed to update tool");
                e.printStackTrace();
                System.out.println("Exception:" + e.getMessage());
            }
        } else {
            responseObject.setMessage("Data not found");
            responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseObject;
    }


    public Object deleteToolMgmt(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            ToolManagement toolManagement = toolMgmtRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")),
                    true);
            if (toolManagement != null) {
                toolManagement.setStatus(false);
                Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                toolManagement.setUpdatedBy(user.getId());
                toolManagement.setInstitute(user.getInstitute());
                try {
                    toolMgmtRepository.save(toolManagement);
                    responseObject.setMessage("Tool deleted successfully");
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
            responseObject.setMessage("Failed to delete tool");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }


    public Object getToolMgmt() {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            List<ToolManagement> toolMgmtList = toolMgmtRepository.findAllByStatus(true);
            responseMessage.setResponse(toolMgmtList);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.setMessage("Exception occurred");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    private ToolManagementDTO convertToToolManagementDTO(ToolManagement toolManagement) {
        ToolManagementDTO toolManagementDTO = new ToolManagementDTO();
        if (toolManagement.getAction() != null) {
            toolManagementDTO.setActionId(toolManagement.getAction().getId());
            toolManagementDTO.setActionName(toolManagement.getAction().getActionName());
        }
        toolManagementDTO.setToolManagementId(toolManagement.getId());
        toolManagementDTO.setBlock(toolManagement.getBlock());
        toolManagementDTO.setOffsetNo(toolManagement.getOffsetNo());
        toolManagementDTO.setToolHolders(toolManagement.getToolHolders());
        toolManagementDTO.setInserts(toolManagement.getInserts());
        toolManagementDTO.setFrequency(toolManagement.getFrequency());
        toolManagementDTO.setUsedFor(toolManagement.getUsedFor());
        toolManagementDTO.setStatus(toolManagement.getStatus());
        toolManagementDTO.setCreatedAt(toolManagement.getCreatedAt());
        toolManagementDTO.setCreatedBy(toolManagement.getCreatedBy());
        toolManagementDTO.setUpdatedBy(toolManagement.getUpdatedBy());
        toolManagementDTO.setUpdatedAt(toolManagement.getUpdatedAt());
        return toolManagementDTO;
    }
}
