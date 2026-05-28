package com.irisperez.tfg.encuentraformacion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class DatabaseDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseDataInitializer.class);
    private static final String DUMP_CLASSPATH = "db/backup_ef.dump";

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${DB_NAME}")
    private String dbName;

    @Value("${db.auto-restore.container:encuentra_formacion_db}")
    private String containerName;

    @Value("${db.auto-restore.enabled:true}")
    private boolean autoRestoreEnabled;

    public DatabaseDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!autoRestoreEnabled) {
            log.info("Restauración automática desactivada (db.auto-restore.enabled=false).");
            return;
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM encuentra_formacion.formaciones", Integer.class);

        if (count != null && count > 0) {
            log.info("Base de datos ya inicializada ({} formaciones).", count);
            return;
        }

        log.info("Base de datos vacía. Restaurando datos desde {}...", DUMP_CLASSPATH);
        restoreFromBackup();
        log.info("Restauración completada.");
    }

    private void restoreFromBackup() throws Exception {
        ClassPathResource resource = new ClassPathResource(DUMP_CLASSPATH);
        Path tempFile = Files.createTempFile("backup_ef", ".dump");

        try (InputStream is = resource.getInputStream()) {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        String containerPath = "/tmp/backup_ef.dump";

        try {
            runCommand("docker", "cp", tempFile.toString(), containerName + ":" + containerPath);
            runCommand("docker", "exec", containerName,
                    "pg_restore",
                    "-U", dbUser,
                    "-d", dbName,
                    "--schema=encuentra_formacion",
                    "-F", "c",
                    "--data-only",
                    "--clean",
                    "--disable-triggers",
                    "--no-owner", "--no-privileges",
                    containerPath);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void runCommand(String... cmd) throws Exception {
        String cmdStr = Arrays.stream(cmd).collect(Collectors.joining(" "));
        log.debug("Ejecutando: {}", cmdStr);

        Process process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();

        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IllegalStateException(
                    "Comando fallido (exit " + exitCode + "): " + cmdStr + "\n" + output);
        }
    }
}
