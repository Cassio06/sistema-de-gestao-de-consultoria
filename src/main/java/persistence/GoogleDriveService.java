package persistence;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class GoogleDriveService {
    private static final String CREDENTIALS_PATH = resolverCaminhoCredenciais();

    private static String resolverCaminhoCredenciais() {
        String envPath = System.getenv("GOOGLE_CREDENTIALS_PATH");
        if (envPath != null && !envPath.isBlank()) {
            return envPath.trim();
        }
        Path dotEnv = Paths.get(".env");
        if (Files.exists(dotEnv)) {
            try {
                for (String linha : Files.readAllLines(dotEnv, StandardCharsets.UTF_8)) {
                    if (linha.startsWith("GOOGLE_CREDENTIALS_PATH=")) {
                        String valor = linha.split("=", 2)[1].trim().replace("\"", "");
                        if (!valor.isEmpty()) return valor;
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return "credentials.json";
    }

    public void enviarBackup(java.io.File arquivoLocal) {
        if (!Files.exists(Paths.get(CREDENTIALS_PATH))) {
            System.out.println("[GOOGLE DRIVE] Backup em nuvem ignorado (credenciais '" + CREDENTIALS_PATH + "' não configuradas).");
            return;
        }

        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(CREDENTIALS_PATH))
                    .createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));

            Drive service = new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Planilha Certa Consultoria")
                    .build();

            File fileMetadata = new File();
            fileMetadata.setName("backup_planilha_certa.json");

            FileContent mediaContent = new FileContent("application/json", arquivoLocal);
            File uploadedFile = service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();

            System.out.println("[GOOGLE DRIVE] Backup sincronizado na nuvem! ID do arquivo: " + uploadedFile.getId());
        } catch (Exception e) {
            System.out.println("[GOOGLE DRIVE] Falha na sincronização (modo offline mantido): " + e.getMessage());
        }
    }
}