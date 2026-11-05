package logs.com.log;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class N8nController {

    // Simulamos la base de datos con mapas en memoria
    private final List<LogEntry> logDatabase = new ArrayList<>();
    private final Map<String, AnalysisResult> analysisDatabase = new HashMap<>();
    
    // IMPORTANTE: Cambia esto por tu URL de producción de n8n
    private final String N8N_WEBHOOK_URL = "https://n8n-service-s9ad.onrender.com/webhook/miworkflow";

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

        try {
            RestTemplate restTemplate = new RestTemplate();
            // Enviamos los logs a n8n
            String n8nResponse = restTemplate.postForObject(N8N_WEBHOOK_URL, logsToAnalyze, String.class);
            
            // Generamos un ID para el análisis y lo guardamos
            String analysisId = UUID.randomUUID().toString();
            AnalysisResult result = new AnalysisResult(analysisId, n8nResponse, "COMPLETED");
            analysisDatabase.put(analysisId, result);

            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al conectar con n8n: " + e.getMessage());
        }
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
