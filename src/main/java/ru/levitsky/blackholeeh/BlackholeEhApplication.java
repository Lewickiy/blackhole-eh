package ru.levitsky.blackholeeh;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.levitsky.blackholeeh.service.FileProcessor;

@SpringBootApplication
@RequiredArgsConstructor
public class BlackholeEhApplication implements CommandLineRunner {

private final FileProcessor fileProcessor;

    static void main(String[] args) {
        SpringApplication.run(BlackholeEhApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String directory = args.length > 0 ? args[0] : "target/classes/img";
        System.out.println("📂 Processing directory: " + directory);
        fileProcessor.processDirectory(directory);
        System.out.println("✅ Done");
    }

}
