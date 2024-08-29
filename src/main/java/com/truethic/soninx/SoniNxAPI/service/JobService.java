package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.fileConfig.FileStorageProperties;
import com.truethic.soninx.SoniNxAPI.fileConfig.FileStorageService;
import com.truethic.soninx.SoniNxAPI.repository.JobRepository;
import com.truethic.soninx.SoniNxAPI.repository.TaskViewRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.JobDTDTO;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.model.Job;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class JobService {
    private static final Logger jobLogger = LoggerFactory.getLogger(JobService.class);
    @Autowired
    JobRepository jobRepository;
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @Value("${spring.serversource.url}")
    private String serverUrl;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private TaskViewRepository taskViewRepository;
    @Autowired
    private TaskService taskService;

    public Object createJob(MultipartHttpServletRequest request) {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        ResponseMessage responseObject = new ResponseMessage();
        Job job = new Job();
        job.setJobName(request.getParameter("jobName"));

        if (request.getFile("jobDocument") != null) {
            MultipartFile image = request.getFile("jobDocument");
            fileStorageProperties.setUploadDir("./uploads" + File.separator + "jobs" + File.separator);
            String imagePath = fileStorageService.storeFile(image, fileStorageProperties);

            if (imagePath != null) {
                job.setJobImagePath("/uploads" + File.separator + "jobs" + File.separator + imagePath);
            } else {
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseObject.setMessage("Failed to upload documents. Please try again!");
                return responseObject;
            }
        }

//            String imagePath = uploadJobDocument(request.getFile("jobDocument"));
//            String[] arr = imagePath.split("#");
//            if (imagePath != null) {
//                job.setJobImagePath(awss3Service.getBASE_URL() + arr[0]);
//                job.setJobImageKey(arr[1]);
//            } else {
//                responseObject.setMessage("Document uploading error");
//                responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
//                return responseObject;
//            }
//        }
//        else {
//            responseObject.setMessage("Please upload document");
//            responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
//            return responseObject;
//        }

        job.setStatus(true);
        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        job.setCreatedBy(user.getId());
        job.setCreatedAt(LocalDateTime.now());
        job.setInstitute(user.getInstitute());
        try {
            jobRepository.save(job);
            responseObject.setMessage("Job created successfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Failed to create job");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    public Object DTJob(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Job> jobList = new ArrayList<>();
        List<JobDTDTO> jobDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `job_tbl` WHERE status=1 AND institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND job_name LIKE '%" + searchText + "%'";
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

            Query q = entityManager.createNativeQuery(query, Job.class);
            Query q1 = entityManager.createNativeQuery(query1, Job.class);

            jobList = q.getResultList();
            System.out.println("Limit total rows " + jobList.size());

            List<Job> jobArrayList = new ArrayList<>();
            jobArrayList = q1.getResultList();
            System.out.println("total rows " + jobArrayList.size());

            for (Job job : jobList) {
                jobDTDTOList.add(convertJobToJobDT(job));
            }

            genericDTData.setRows(jobDTDTOList);
            genericDTData.setTotalRows(jobArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(jobDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private JobDTDTO convertJobToJobDT(Job job) {
        JobDTDTO jobDTDTO = new JobDTDTO();
        jobDTDTO.setId(job.getId());
        jobDTDTO.setJobName(job.getJobName());
        if (job.getJobImagePath() != null)
            jobDTDTO.setJobImagePath(serverUrl + job.getJobImagePath());
        jobDTDTO.setCreatedAt(String.valueOf(job.getCreatedAt()));
        jobDTDTO.setStatus(job.getStatus());
        return jobDTDTO;
    }

    public JsonObject listOfJobsForSelect(HttpServletRequest httpServletRequest) {
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        JsonObject responseMessage = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            List<Job> jobList = jobRepository.findAllByInstituteIdAndStatus(user.getInstitute().getId(), true);
            for (Job job : jobList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", job.getId());
                object.addProperty("jobName", job.getJobName());
                object.addProperty("jobDocument", serverUrl + job.getJobImagePath());

                jsonArray.add(object);
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

    public Object listOfJobs() {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            List<Job> jobList = jobRepository.findAllByStatus(true);
            responseMessage.setResponse(jobList);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.setMessage("Exception occurred");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public Object findJob(Map<String, String> requestParam) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Job job = jobRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
            if (job != null) {

                String uploadedFilePath = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path(job.getJobImagePath())
                        .toUriString();
                System.out.println("uploadedFilePath " + uploadedFilePath);
                job.setJobImagePath(uploadedFilePath);
                responseMessage.setResponse(job);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
        }
        return responseMessage;
    }

    public Object updateJob(MultipartHttpServletRequest request) throws IOException {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        ResponseMessage responseObject = new ResponseMessage();
        Job job = jobRepository.findByIdAndStatus(Long.parseLong(request.getParameter("id")), true);
        if (job != null) {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

            if (request.getFile("jobDocument") != null) {
                if (job.getJobImagePath() != null) {
                    File oldFile = new File("." + job.getJobImagePath());

                    if (oldFile.exists()) {
                        System.out.println("Document Deleted");
                        //remove file from local directory
                        if (!oldFile.delete()) {
                            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                            responseObject.setMessage("Failed to delete old documents. Please try again!");
                            return responseObject;
                        }
                    }
                }

                MultipartFile image = request.getFile("jobDocument");
                fileStorageProperties.setUploadDir("./uploads" + File.separator + "jobs" + File.separator);
                String imagePath = fileStorageService.storeFile(image, fileStorageProperties);

                if (imagePath != null) {
                    job.setJobImagePath("/uploads" + File.separator + "jobs" + File.separator + imagePath);
                } else {
                    responseObject.setMessage("Failed to upload documents. Please try again!");
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    return responseObject;
                }
            }

            job.setJobName(request.getParameter("jobName"));
            job.setStatus(true);
            job.setUpdatedBy(user.getId());
            job.setInstitute(user.getInstitute());
            job.setUpdatedAt(LocalDateTime.now());

            /*if (request.getFile("jobDocument") != null) {
                if (job.getJobImageKey() != null) {
                    Boolean result = awss3Service.deleteFileFromS3Bucket(job.getJobImageKey());
                    if (result) {
                        System.out.println("Document Deleted");
                    } else {
                        responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                        responseObject.setMessage("Failed to delete old documents. Please try again!");
                        return responseObject;
                    }
                }
                String imagePath = uploadJobDocument(request.getFile("jobDocument"));
                String[] arr = imagePath.split("#");
                if (imagePath != null) {
                    job.setJobImagePath(awss3Service.getBASE_URL() + arr[0]);
                    job.setJobImageKey(arr[1]);
                } else {
                    responseObject.setMessage("Document uploading error");
                    responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
                    return responseObject;
                }
            }*/

            try {
                jobRepository.save(job);
                responseObject.setMessage("Job updated successfully");
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

    public Object deleteJob(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        Job job = jobRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
        if (job != null) {
            job.setStatus(false);
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            job.setUpdatedBy(user.getId());
            job.setUpdatedAt(LocalDateTime.now());
            job.setInstitute(user.getInstitute());
            try {
                jobRepository.save(job);
                responseObject.setMessage("Job deleted successfully");
                responseObject.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseObject.setMessage("Failed to delete job");
                e.printStackTrace();
                System.out.println("Exception:" + e.getMessage());
            }
        } else {
            responseObject.setMessage("Data not found");
            responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseObject;
    }

    private String uploadJobDocument(MultipartFile multipartFile) {
        String dir = "job_document";
        String imagePath = null;
        // imagePath = awss3Service.uploadFile(multipartFile, dir);
        return imagePath;
    }


    /*mobile app url start*/

    public JsonObject listForSelection(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            List<Job> jobList = jobRepository.findAllByInstituteIdAndStatus(employee.getInstitute().getId(),true);
            for (Job job : jobList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", job.getId());
                object.addProperty("name", job.getJobName());
//                    object.addProperty("jobDocument", job.getJobImagePath());
                jsonArray.add(object);
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
    /*mobile app url end*/

    public Object uploadJob(MultipartHttpServletRequest request) {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        ResponseMessage responseObject = new ResponseMessage();
        Job job = new Job();
        job.setJobName(request.getParameter("jobName"));

        if (request.getFile("jobDocument") != null) {
            MultipartFile image = request.getFile("jobDocument");
//            fileStorageProperties.setUploadDir(uploadPath + File.separator + jobFolder + File.separator);
            fileStorageProperties.setUploadDir("./uploads" + File.separator + "/jobs" + File.separator);
            String imagePath = fileStorageService.storeFile(image, fileStorageProperties);

            if (imagePath != null) {
                job.setJobImagePath("/uploads/jobs/" + imagePath);
            } else {
                responseObject.setMessage("Document uploading error");
                responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
                return responseObject;
            }
        }

        job.setStatus(true);
        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        job.setCreatedBy(user.getId());
        job.setCreatedAt(LocalDateTime.now());
        job.setInstitute(user.getInstitute());
        try {
            jobRepository.save(job);
            responseObject.setMessage("Job created successfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Failed to create job");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    public JsonObject getItemReport(Map<String, String> request) {
        JsonObject response = new JsonObject();
        JsonArray itemArray = new JsonArray();
        try {
            LocalDate fromDate = LocalDate.parse(request.get("fromDate"));
            LocalDate toDate = LocalDate.parse(request.get("toDate"));
            String jobId = request.get("jobId");
            List<Object[]> itemList = new ArrayList<>();

            if (!jobId.equalsIgnoreCase(""))
                itemList = jobRepository.getItemReportsByJobId(fromDate, toDate, jobId, true);
            else itemList = jobRepository.getItemReports(fromDate, toDate, true);

            for (int i = 0; i < itemList.size(); i++) {
                Object[] obj = itemList.get(i);

                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("itemId", obj[0] != "" ? obj[0].toString():"");
                itemObj.addProperty("itemName", obj[1].toString());
                itemObj.addProperty("jobOperationId", obj[2] != "" ? obj[2].toString():"");
                itemObj.addProperty("jobOperationName", obj[3].toString());
                itemObj.addProperty("productionQty", obj[4].toString());

                jobId = String.valueOf(obj[0] != null ? Long.valueOf(obj[0].toString()) : null);
                Long jobOperationId = Long.valueOf(obj[2] != null ? obj[2].toString() : null);
                List<Object[]> taskViewList = taskViewRepository.findTaskViewData(
                        Long.valueOf(jobId), jobOperationId, fromDate, toDate, true);

                JsonArray taskDTOList = new JsonArray();
                for (int j=0; j< taskViewList.size(); j++) {
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
                itemObj.add("taskList", taskDTOList);
                itemArray.add(itemObj);
            }

            response.add("response", itemArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            jobLogger.error("getItemReport exception " + e);
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }
}
