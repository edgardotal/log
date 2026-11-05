package logs.com.log;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class N8nController {

    @GetMapping("/saludo")
    public String saludo() {
        return "¡Hola! La API de Spring Boot está funcionando.";
    }

    @PostMapping("/disparar")
    public String dispararFlujo(@RequestBody Map<String, Object> datos) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            // Aquí luego pondremos la URL de n8n
            String N8N_WEBHOOK_URL = "http://localhost:5678/webhook-test/mi-flujo"; 
            String respuesta = restTemplate.postForObject(N8N_WEBHOOK_URL, datos, String.class);
            return "Flujo disparado. Respuesta: " + respuesta;
        } catch (Exception e) {
            return "Error al conectar con n8n: " + e.getMessage();
        }
    }
}
