package persistence;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleDriveService {
    public void enviarBackup(java.io.File arquivoLocal) {
        try {
            Map<String, String> config = lerDotEnv();
            if (!Boolean.parseBoolean(configuracao(config, "GOOGLE_DRIVE_ENABLED"))) {
                return;
            }
            String credenciais = configuracao(config, "GOOGLE_APPLICATION_CREDENTIALS", "GOOGLE_CREDENTIALS_PATH");
            if (credenciais.isEmpty() || !Files.isRegularFile(Paths.get(credenciais))) {
                System.out.println("[GOOGLE DRIVE] Configure um arquivo de credenciais para enviar o backup.");
                return;
            }
            GoogleCredentials credentials;
            try (InputStream entrada = Files.newInputStream(Paths.get(credenciais))) {
                credentials = GoogleCredentials.fromStream(entrada).createScoped(List.of(DriveScopes.DRIVE_FILE));
            }

            Drive service = new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Sistema de Gestão de Consultoria")
                    .build();

            File fileMetadata = new File();
            fileMetadata.setName("backup_planilha_certa.json");
            String pasta = configuracao(config, "GOOGLE_DRIVE_FOLDER_ID");
            if (!pasta.isEmpty()) {
                fileMetadata.setParents(List.of(pasta));
            }

            FileContent mediaContent = new FileContent("application/json", arquivoLocal);
            File uploadedFile = service.files().create(fileMetadata, mediaContent)
                    .setSupportsAllDrives(true)
                    .setFields("id")
                    .execute();

            System.out.println("[GOOGLE DRIVE] Backup sincronizado na nuvem! ID do arquivo: " + uploadedFile.getId());
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            System.out.println("[GOOGLE DRIVE] Falha na sincronização (modo offline mantido): " + e.getMessage());
        }
    }

    private static Map<String, String> lerDotEnv() throws IOException {
        Path arquivo = Paths.get(".env");
        Map<String, String> valores = new HashMap<>();
        if (Files.exists(arquivo)) {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                String texto = linha.strip();
                int separador = texto.indexOf('=');
                if (texto.startsWith("#") || separador < 1) {
                    continue;
                }
                String valor = texto.substring(separador + 1).strip();
                if (valor.length() >= 2 && (valor.startsWith("\"") && valor.endsWith("\"")
                        || valor.startsWith("'") && valor.endsWith("'"))) {
                    valor = valor.substring(1, valor.length() - 1);
                }
                valores.put(texto.substring(0, separador).strip(), valor);
            }
        }
        return valores;
    }

    private static String configuracao(Map<String, String> config, String... chaves) {
        for (String chave : chaves) {
            String ambiente = System.getenv(chave);
            if (ambiente != null) {
                return ambiente.strip();
            }
        }
        for (String chave : chaves) {
            if (config.containsKey(chave)) {
                return config.get(chave);
            }
        }
        return "";
    }
}
