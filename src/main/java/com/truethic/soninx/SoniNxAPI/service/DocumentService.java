package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.DocumentRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.DocumentDTDTO;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.model.Document;
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
public class DocumentService {
    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @PersistenceContext
    private EntityManager entityManager;

    public Object createDocument(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

            Document document = new Document();
            document.setName(requestParam.get("documentName"));
            document.setIsRequired(Boolean.parseBoolean(requestParam.get("isRequired")));
            document.setStatus(true);
            document.setCreatedBy(user.getId());
            document.setInstitute(user.getInstitute());
            try {
                Document document1 = documentRepository.save(document);
                responseObject.setResponse(document1);
                responseObject.setMessage("Document saved successfully");
                responseObject.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseObject.setMessage("Failed to save document");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
            responseObject.setMessage("Failed to save document");
        }
        return responseObject;
    }

    public JsonObject listOfDocument(HttpServletRequest httpServletRequest) {
        Users users = jwtTokenUtil.getUserDataFromToken(httpServletRequest .getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        try {
            List<Document> documentList = documentRepository.findAllByInstituteIdAndStatusOrderByNameAsc(users.getInstitute().getId(),true);
            for (Document document : documentList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", document.getId());
                object.addProperty("documentName", document.getName());
                object.addProperty("isRequired", document.getIsRequired());
                jsonArray.add(object);
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object DTDocuments(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Document> documentList = new ArrayList<>();
        List<DocumentDTDTO> documentDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `document_tbl` WHERE status=1 AND institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND name LIKE '%" + searchText + "%'";
            }

            String jsonToStr = request.get("sort");
            JsonObject jsonObject = new Gson().fromJson(jsonToStr, JsonObject.class);
            if (!jsonObject.get("colId").toString().equalsIgnoreCase("null") &&
                    jsonObject.get("colId").toString() != null) {
                //   System.out.println(" ORDER BY " + jsonObject.getString("colId"));
                String sortBy = jsonObject.get("colId").toString();
                query = query + " ORDER BY " + sortBy;
                if (jsonObject.get("isAsc").getAsBoolean()) {
                    query = query + " ASC";
                } else {
                    query = query + " DESC";
                }
            }
            String query1 = query;
            Integer endLimit = to - from;
            query = query + " LIMIT " + from + ", " + endLimit;
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, Document.class);
            Query q1 = entityManager.createNativeQuery(query1, Document.class);

            documentList = q.getResultList();
            System.out.println("Limit total rows " + documentList.size());

            for (Document document : documentList) {
                documentDTDTOList.add(convertToDTO(document));
            }
            List<Document> documentArrayList = new ArrayList<>();
            documentArrayList = q1.getResultList();
            System.out.println("total rows " + documentArrayList.size());

            genericDTData.setRows(documentDTDTOList);
            genericDTData.setTotalRows(documentArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(documentDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private DocumentDTDTO convertToDTO(Document document) {
        DocumentDTDTO documentDTDTO = new DocumentDTDTO();
        documentDTDTO.setId(document.getId());
        documentDTDTO.setName(document.getName());
        documentDTDTO.setIsRequired(document.getIsRequired());
        documentDTDTO.setCreatedBy(document.getCreatedBy());
        documentDTDTO.setCreatedAt(String.valueOf(document.getCreatedAt()));
        documentDTDTO.setUpdatedBy(document.getUpdatedBy());
        documentDTDTO.setUpdatedAt(String.valueOf(document.getUpdatedAt()));
        return documentDTDTO;
    }

    public Object findDocument(Map<String, String> requestParam) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Document document = documentRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
            if (document != null) {
                responseMessage.setResponse(document);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to load data");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object updateDocument(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        Document document = documentRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
        if (document != null) {
            document.setName(requestParam.get("documentName"));
            document.setIsRequired(Boolean.parseBoolean(requestParam.get("isRequired")));
            document.setUpdatedBy(user.getId());
            document.setUpdatedAt(LocalDateTime.now());
            document.setInstitute(user.getInstitute());
            try {
                documentRepository.save(document);
                responseMessage.setMessage("Document updated successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseMessage.setMessage("Failed to update document");
            }
        } else {
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }

    public Object deleteDocument(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        Document document = documentRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
        if (document != null) {
            document.setStatus(false);
            document.setUpdatedBy(user.getId());
            document.setUpdatedAt(LocalDateTime.now());
            document.setInstitute(user.getInstitute());
            try {
                documentRepository.save(document);
                responseMessage.setMessage("Document deleted successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseMessage.setMessage("Failed to delete document");
            }
        } else {
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }
}
