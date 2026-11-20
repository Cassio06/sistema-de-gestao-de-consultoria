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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

public class GoogleDriveService {
    private static final String CREDENTIALS_PATH = "credentials.json";

    public void enviarBackup(java.io.File arquivoLocal) {
        // Verifica se a credencial existe antes de tentar conectar (evita crash na apresentação sem internet/chave)
        if (!Files.exists(Paths.get(CREDENTIALS_PATH))) {
            System.out.println("[GOOGLE DRIVE] Backup em nuvem ignorado (arquivo 'credentials.json' não configurado).");
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
