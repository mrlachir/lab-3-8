package ma.fst.studentapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class StudentApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private static final String URL="/api/students";
    private static final String STUDENT="""
        {"firstName":"Sara","lastName":"El Amrani","email":"sara@example.com","major":"Informatique","age":21}
        """;

    @Test void crudLifecycle() throws Exception {
        String body=mvc.perform(post(URL).contentType("application/json").content(STUDENT))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
            .andReturn().getResponse().getContentAsString();
        String resource=URL+"/"+json.readTree(body).get("id").asLong();
        mvc.perform(get(URL)).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.email == 'sara@example.com')]").isNotEmpty());
        mvc.perform(get(resource)).andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Sara"));
        mvc.perform(put(resource).contentType("application/json").content(STUDENT.replace("Informatique","Data Science")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.major").value("Data Science"));
        mvc.perform(get(resource)).andExpect(jsonPath("$.major").value("Data Science"));
        mvc.perform(delete(resource)).andExpect(status().isNoContent());
        mvc.perform(get(resource)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test void invalidAndMalformedRequests() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content(STUDENT.replace("21","16")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(post(URL).contentType("application/json").content("{"))
            .andExpect(status().isBadRequest());
    }

    @Test void duplicateEmailConflicts() throws Exception {
        String payload=STUDENT.replace("sara@example.com","duplicate@example.com");
        mvc.perform(post(URL).contentType("application/json").content(payload)).andExpect(status().isCreated());
        mvc.perform(post(URL).contentType("application/json").content(payload)).andExpect(status().isConflict());
    }

    @Test void openApiAndSwaggerAreAvailable() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/students'].post").exists())
            .andExpect(jsonPath("$.paths['/api/students/{id}'].delete").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
