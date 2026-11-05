package logs.com.log;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class N8nController {

    private final List<LogEntry> logDatabase = new ArrayList<>();
    private final Map<String, AnalysisResult> analysisDatabase = new HashMap<>();
    
    // URL de n8n
    private final String N8N_WEBHOOK_URL = "https://n8n-service-s9ad.onrender.com/webhook/miworkflow";
    
    // URL de tu API en Render (para que n8n nos devuelva el resultado)
    // IMPORTANTE: Render te asigna una URL. Asegúrate de que sea la tuya.
    private final String MY_API_URL = "https://log-4t0m.onrender.com/api/logs/callback";

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "SmartLog Analyzer API");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logs")
    public ResponseEntity<?> ingestLog(@RequestBody LogEntry log) {
        if (log.getMessage() == null || log.getLevel() == null) {
            return ResponseEntity.badRequest().body("Faltan campos obligatorios (message, level)");
        }
        log.setId(UUID.randomUUID().toString());
        log.setTimestamp(java.time.LocalDateTime.now().toString());
        logDatabase.add(log);
        return ResponseEntity.status(HttpStatus.CREATED).body(log);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<LogEntry>> getLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String service) {
        
        List<LogEntry> filtered = logDatabase.stream()
            .filter(l -> level == null || l.getLevel().equalsIgnoreCase(level))
            .filter(l -> service == null || l.getService().equalsIgnoreCase(service))
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(filtered);
    }

    @PostMapping("/logs/analyze")
    public ResponseEntity<?> analyzeLogs(@RequestBody List<LogEntry> logsToAnalyze) {
        if (logsToAnalyze == null || logsToAnalyze.isEmpty()) {
            return ResponseEntity.badRequest().body("No se enviaron logs para analizar");
        }

        String analysisId = UUID.randomUUID().toString();
        AnalysisResult result = new AnalysisResult(analysisId, "Análisis en proceso...", "PROCESSING");
        analysisDatabase.put(analysisId, result);

        // Enviamos a n8n de forma asíncrona
        Map<String, Object> payloadToN8n = new HashMap<>();
        payloadToN8n.put("analysisId", analysisId);
        payloadToN8n.put("logs", logsToAnalyze);
        payloadToN8n.put("callbackUrl", MY_API_URL); // Le decimos a n8n a dónde responder

        new Thread(() -> {
            try {
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.postForObject(N8N_WEBHOOK_URL, payloadToN8n, String.class);
            } catch (Exception e) {
                System.err.println("Error enviando a n8n: " + e.getMessage());
            }
        }).start();

        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body("Petición aceptada. Análisis en curso. Consulte el resultado en: /api/analysis/" + analysisId);
    }

    // ESTE ES EL NUEVO ENDPOINT QUE N8N USARÁ PARA DEVOLVER EL RESULTADO
    @PostMapping("/logs/callback")
    public ResponseEntity<?> receiveAnalysisFromN8n(@RequestBody Map<String, String> payload) {
        String analysisId = payload.get("analysisId");
        String aiResponse = payload.get("aiResponse");
        
        if (analysisId == null || aiResponse == null) {
            return ResponseEntity.badRequest().body("Faltan datos en el callback");
        }

        AnalysisResult result = analysisDatabase.get(analysisId);
        if (result != null) {
            analysisDatabase.put(analysisId, new AnalysisResult(analysisId, aiResponse, "COMPLETED"));
            return ResponseEntity.ok("Resultado guardado exitosamente");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ID de análisis no encontrado");
    }

    @GetMapping("/analysis/{id}")
    public ResponseEntity<?> getAnalysis(@PathVariable String id) {
        AnalysisResult result = analysisDatabase.get(id);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Análisis no encontrado");
        }
        return ResponseEntity.ok(result);
    }
}
