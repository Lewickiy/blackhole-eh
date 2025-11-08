package ru.levitsky.blackholeeh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.levitsky.blackholeeh.service.FileProcessor;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class BlackholeEhApplication implements CommandLineRunner {

private final FileProcessor fileProcessor;

    static void main(String[] args) {
        SpringApplication.run(BlackholeEhApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String directory = args.length > 0 ? args[0] : "target/classes/img";
        log.info("Processing directory: {}", directory);
        fileProcessor.processDirectory(directory);
        log.info(" Done");
    }

}
