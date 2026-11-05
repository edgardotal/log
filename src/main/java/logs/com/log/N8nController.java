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
            String n8nUrl = "https://n8n-service-s9ad.onrender.com/webhook/miworkflow";
            
            // Hacemos la petición a n8n
            String respuestaN8n = restTemplate.postForObject(n8nUrl, datos, String.class);
            
            // Si n8n devuelve algo, lo mostramos de forma limpia
            return "✅ Flujo disparado con éxito. Datos enviados: " + datos.toString();
            
        } catch (Exception e) {
            return "❌ Error al conectar con n8n: " + e.getMessage();
        }
    }
}
